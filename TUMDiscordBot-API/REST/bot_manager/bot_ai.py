import json
import logging
import datetime
import asyncio
from pathlib import Path
from flask import Blueprint, jsonify, request, abort
from REST.api.api_validation import requires_api_key
from REST.AI.gemini_service import get_gemini_service
from REST.settings_manager import SETTINGS

ai_bp = Blueprint('ai', __name__)

PROJECT_ROOT = Path(__file__).resolve().parents[2]

@ai_bp.route('/api/ai/generate-qa', methods=['POST'])
@requires_api_key
def generate_qa_from_pdf():
    """
    Generate questions and answers from a PDF file using Gemini AI.

    Request:
        - Content-Type: multipart/form-data
        - api_key (str): API key for authentication (query parameter or form data)
        - pdf (file): PDF file to process
        - num_questions (int): Number of questions to generate
    """
    try:
        if 'pdf' not in request.files:
            abort(400, description="No PDF file provided")

        pdf_file = request.files['pdf']

        if pdf_file.filename == '':
            abort(400, description="Empty filename")

        num_questions = request.form.get('num_questions')
        if not num_questions:
            abort(400, description="num_questions parameter is required")

        try:
            num_questions = int(num_questions)
            if num_questions <= 0:
                abort(400, description="num_questions must be a positive integer")
        except ValueError:
            abort(400, description="num_questions must be a valid integer")

        try:
            gemini_service = get_gemini_service()
        except ValueError:
            abort(500, description="Gemini API key not configured")

        try:
            import REST.utils.bot_context as bc
            if bc.bot_running:
                loop = bc.get_live_loop()
                future = asyncio.run_coroutine_threadsafe(gemini_service.process_pdf_and_generate_qa(pdf_file, num_questions), loop)
                result = future.result(timeout=60)
            else:
                result = asyncio.run(gemini_service.process_pdf_and_generate_qa(pdf_file, num_questions))
        except Exception as e:
            logging.error(f"Async AI call failed: {e}")
            raise e

        logging.info(f"Generated {result['num_questions']} Q&A pairs")
        logging.debug(f"Q&A pairs: {result['qa_pairs']}")

        response_data = {
            'success': True,
            'num_questions': result['num_questions'],
            'qa_pairs': result['qa_pairs'],
            'pdf_text_length': result['pdf_text_length']
        }

        logging.info(f"Sending response: {json.dumps(response_data, indent=2)}")

        return jsonify(response_data), 200

    except ValueError as e:
        logging.error(f"Validation error: {str(e)}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 400
    except Exception as e:
        import traceback
        error_trace = traceback.format_exc()
        logging.error(f"Server error: {error_trace}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500


@ai_bp.route('/api/ai/save-qa', methods=['POST'])
def save_qa_pairs():
    """
    Save edited Q&A pairs to a JSON file and send to Discord channel as a modal/quiz.

    Request:
        - Content-Type: application/json
        - Body:
        {
            "qa_pairs": [
                {
                    "question": "What is...",
                    "answer": "The answer is..."
                }
            ],
            "channel_id": "123456789",  # Discord channel ID
            "duration": 3600,  # Time in seconds the modal will be active
            "filename": "optional_custom_filename.json",  # optional
            "aiFeedback": false  # optional, default false - if true, AI provides feedback on each answer
        }
    """
    try:
        data = request.get_json()

        if not data or 'qa_pairs' not in data:
            abort(400, description="qa_pairs field is required")

        if 'channel_id' not in data:
            abort(400, description="channel_id field is required")

        if 'duration' not in data:
            abort(400, description="duration field is required")

        qa_pairs = data['qa_pairs']
        channel_id = data['channel_id']
        duration = data['duration']
        ai_feedback = data.get('aiFeedback', False)

        if not isinstance(qa_pairs, list) or len(qa_pairs) == 0:
            abort(400, description="qa_pairs must be a non-empty list")

        try:
            channel_id = int(channel_id)
        except (ValueError, TypeError):
            abort(400, description="channel_id must be a valid integer")

        try:
            duration = float(duration)
            if duration <= 0:
                abort(400, description="duration must be a positive number")
        except (ValueError, TypeError):
            abort(400, description="duration must be a valid number")

        for i, pair in enumerate(qa_pairs):
            if not isinstance(pair, dict):
                abort(400, description=f"Q&A pair at index {i} must be an object")
            if 'question' not in pair or 'answer' not in pair:
                abort(400, description=f"Q&A pair at index {i} must have 'question' and 'answer' fields")

        qa_dir = PROJECT_ROOT / 'data' / 'qa_pairs'
        qa_dir.mkdir(exist_ok=True, parents=True)

        filename = data.get('filename')
        if not filename:
            timestamp = datetime.datetime.now().strftime('%Y-%m-%d_%H-%M-%S')
            filename = f"qa_pairs_{timestamp}.json"

        if not filename.endswith('.json'):
            filename += '.json'

        file_path = qa_dir / filename

        with open(file_path, 'w', encoding='utf-8') as f:
            json.dump({
                'created_at': datetime.datetime.now().isoformat(),
                'num_pairs': len(qa_pairs),
                'ai_feedback_enabled': ai_feedback,
                'qa_pairs': qa_pairs
            }, f, indent=2, ensure_ascii=False)

        logging.info(f"Saved {len(qa_pairs)} Q&A pairs to {file_path}")

        from bot.discord_bot_functions import send_qa_to_channel, schedule_send_qa_results
        result = send_qa_to_channel(channel_id, qa_pairs, duration, ai_feedback)

        if result['status'] == 'failure':
            logging.error(f"Failed to send Q&A to Discord: {result['message']}")
            return jsonify({
                'success': False,
                'error': f"Q&A saved but failed to send to Discord: {result['message']}"
            }), 500

        logging.info(f"Successfully sent Q&A to Discord channel {channel_id}")

        view = result.get('view')
        if view:
            gemini_api_key = None
            if ai_feedback:
                try:
                    gemini_api_key = SETTINGS.get('bot', {}).get('gemini_api_key') if SETTINGS else None
                    if not gemini_api_key:
                        logging.warning("AI feedback requested but Gemini API key not configured")
                except Exception:
                    logging.warning("AI feedback requested but Gemini API key not configured")

            schedule_send_qa_results(view, duration, qa_pairs, ai_feedback, gemini_api_key)

        return jsonify({
            'success': True,
            'message': 'Q&A pairs saved and sent to Discord channel',
            'file_path': str(file_path.relative_to(PROJECT_ROOT)),
            'num_pairs': len(qa_pairs),
            'channel_id': str(channel_id),
            'ai_feedback_enabled': ai_feedback
        }), 200

    except ValueError as e:
        logging.error(f"Validation error: {str(e)}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 400
    except Exception as e:
        import traceback
        error_trace = traceback.format_exc()
        logging.error(f"Server error: {error_trace}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500
