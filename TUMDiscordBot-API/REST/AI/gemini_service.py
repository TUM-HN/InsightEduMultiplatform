import json
import PyPDF2
from google import genai
from google.genai.errors import ClientError
from typing import List, Dict, Optional
from .prompts import get_questions_generation_prompt, get_answer_evaluation_prompt, get_batch_answer_evaluation_prompt
from REST.settings_manager import SETTINGS


class GeminiService:
    """Service for interacting with Gemini API"""

    def __init__(self, api_key: str):
        self.client = genai.Client(api_key=api_key)
        self.model_name = 'gemini-3.1-flash-lite-preview'

    async def _call_gemini_with_retry(self, prompt: str, max_retries: int = 3, base_delay: float = 1.0) -> str:
        """Centralized helper for calling Gemini with retry logic and exponential backoff."""
        import asyncio
        import random

        for attempt in range(max_retries + 1):
            try:
                response = await self.client.aio.models.generate_content(
                    model=self.model_name,
                    contents=prompt
                )
                return response.text.strip()

            except ClientError as e:
                is_retryable = '503' in str(e) or 'unavailable' in str(e).lower() or 'overloaded' in str(e).lower()
                
                if is_retryable and attempt < max_retries:
                    delay = (base_delay * (2 ** attempt)) + (random.random() * 0.1)
                    print(f"Gemini API 503/Unavailable. Retrying in {delay:.2f}s... (Attempt {attempt + 1}/{max_retries})")
                    await asyncio.sleep(delay)
                    continue
                
                if 'quota' in str(e).lower() or 'resource' in str(e).lower():
                    raise Exception(f"Gemini API quota exceeded: {e}")
                raise Exception(f"Gemini API error: {e}")
            except Exception as e:
                if attempt < max_retries:
                    delay = (base_delay * (2 ** attempt)) + (random.random() * 0.1)
                    print(f"Unexpected error in Gemini API. Retrying in {delay:.2f}s... (Attempt {attempt + 1}/{max_retries}): {e}")
                    await asyncio.sleep(delay)
                    continue
                raise Exception(f"Error calling Gemini API after {max_retries} retries: {e}")

    def extract_text_from_pdf(self, pdf_file) -> str:
        pdf_reader = PyPDF2.PdfReader(pdf_file)
        text = ""

        for page in pdf_reader.pages:
            text += page.extract_text() + "\n"

        return text.strip()

    async def generate_questions_and_answers(self, pdf_text: str, num_questions: int) -> List[Dict[str, str]]:
        """Generate a list of dictionaries containing questions and answers based on the provided PDF text."""
        prompt = get_questions_generation_prompt(pdf_text, num_questions)

        try:
            response_text = await self._call_gemini_with_retry(prompt)

            if response_text.startswith("```json"):
                response_text = response_text[7:]
            elif response_text.startswith("```"):
                response_text = response_text[3:]

            if response_text.endswith("```"):
                response_text = response_text[:-3]

            response_text = response_text.strip()

            qa_pairs = json.loads(response_text)

            for qa_pair in qa_pairs:
                if 'question' in qa_pair and len(qa_pair['question']) > 100:
                    qa_pair['question'] = qa_pair['question'][:97] + "..."

            return qa_pairs[:num_questions]

        except json.JSONDecodeError as e:
            raise ValueError(f"Failed to parse Gemini response as JSON: {e}")
        except Exception as e:
            raise Exception(f"Error generating questions and answers: {e}")

    async def process_pdf_and_generate_qa(self, pdf_file, num_questions: int) -> Dict:
        """Process PDF and generate questions and answers."""
        pdf_text = self.extract_text_from_pdf(pdf_file)

        if not pdf_text:
            raise ValueError("No text could be extracted from the PDF")

        qa_pairs = await self.generate_questions_and_answers(pdf_text, num_questions)

        return {
            'num_questions': len(qa_pairs),
            'qa_pairs': qa_pairs,
            'pdf_text_length': len(pdf_text)
        }

    async def evaluate_answer(self, question: str, correct_answer: str, user_answer: str) -> Dict[str, str]:
        """Evaluate a user's answer against the correct answer using AI."""
        prompt = get_answer_evaluation_prompt(question, correct_answer, user_answer)

        try:
            response_text = await self._call_gemini_with_retry(prompt)

            if response_text.startswith("```json"):
                response_text = response_text[7:]
            elif response_text.startswith("```"):
                response_text = response_text[3:]

            if response_text.endswith("```"):
                response_text = response_text[:-3]

            response_text = response_text.strip()
            evaluation = json.loads(response_text)
            valid_scores = ["correct", "mostly correct", "mostly incorrect", "incorrect"]

            if evaluation.get('score') not in valid_scores:
                evaluation['score'] = "incorrect"

            if 'feedback' in evaluation and len(evaluation['feedback']) > 1000:
                evaluation['feedback'] = evaluation['feedback'][:997] + "..."

            return evaluation

        except json.JSONDecodeError as e:
            return {
                "score": "incorrect",
                "feedback": "Unable to evaluate answer due to a technical issue. Please review the correct answer provided."
            }
        except Exception as e:
            raise Exception(f"Error evaluating answer: {e}")

    async def evaluate_answers_batch(self, question: str, correct_answer: str, user_answers: List[str]) -> List[Dict[str, str]]:
        """Evaluate multiple users' answers to the same question in a single batch call."""
        prompt = get_batch_answer_evaluation_prompt(question, correct_answer, user_answers)
        
        try:
            response_text = await self._call_gemini_with_retry(prompt)

            if response_text.startswith("```json"):
                response_text = response_text[7:]
            elif response_text.startswith("```"):
                response_text = response_text[3:]

            if response_text.endswith("```"):
                response_text = response_text[:-3]

            response_text = response_text.strip()
            evaluations_raw = json.loads(response_text)
            evaluations_raw.sort(key=lambda x: x.get('index', 0))
            
            results = []
            valid_scores = ["correct", "mostly correct", "mostly incorrect", "incorrect"]
            
            for i in range(len(user_answers)):
                # Find evaluation for this index, or provide default
                eval_item = next((item for item in evaluations_raw if item.get('index') == i), {})
                
                score = eval_item.get('score', 'incorrect')
                if score not in valid_scores:
                    score = 'incorrect'
                    
                feedback = eval_item.get('feedback', "Unable to evaluate answer.")
                if len(feedback) > 1000:
                    feedback = feedback[:997] + "..."
                    
                results.append({
                    "score": score,
                    "feedback": feedback
                })

            return results

        except Exception as e:
            if not isinstance(e, json.JSONDecodeError):
                print(f"Error in Gemini Batch API: {e}")

            return [{
                "score": "incorrect",
                "feedback": "Unable to evaluate answer due to a technical issue."
            } for _ in range(len(user_answers))]

    async def generate_feedback_analysis(self, prompt: str) -> str:
        """Generate feedback analysis based on the provided prompt."""
        try:
            return await self._call_gemini_with_retry(prompt)
        except Exception as e:
            raise Exception(f"Error generating feedback analysis: {e}")

    async def summarize_content(self, data_type: str, content: str) -> str:
        """Summarize content using the AI."""
        from .prompts import get_summarization_prompt
        prompt = get_summarization_prompt(data_type, content)

        try:
            return await self._call_gemini_with_retry(prompt)
        except Exception as e:
            raise Exception(f"Error generating summary: {e}")


_gemini_service_instance: Optional[GeminiService] = None


def get_gemini_service(api_key: Optional[str] = None) -> GeminiService:
    global _gemini_service_instance

    if api_key:
        return GeminiService(api_key)

    if _gemini_service_instance:
        return _gemini_service_instance

    settings_api_key = SETTINGS.get('bot', {}).get('gemini_api_key') if SETTINGS else None
    if not settings_api_key:
        raise ValueError("Gemini API key not found in settings and no api_key provided.")

    _gemini_service_instance = GeminiService(settings_api_key)
    return _gemini_service_instance
