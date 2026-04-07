def get_questions_generation_prompt(pdf_text: str, num_questions: int) -> str:
    return f"""
        Based on the following text content, generate exactly {num_questions} relevant questions and their corresponding answers.
        The questions should test understanding of the key concepts in the text.

        IMPORTANT REQUIREMENTS:
        - Each question MUST be a maximum of 100 characters long (including spaces and punctuation)
        - Each question MUST relate to the key learning concepts in the text, ignore other topics
        - Keep questions concise and clear
        - Avoid formulas and equations in questions and answers
        - Answers can be up to 500 characters

        Format your response as a JSON array of objects with 'question' and 'answer' fields.

        Example format:
        [{{"question": "What is...", "answer": "The answer is..."}}, {{"question": "How does...", "answer": "It works by..."}}]   

        Text content:
        {pdf_text}

        Generate exactly {num_questions} questions and answers in JSON format (remember: questions max 100 characters):
        """

def get_answer_evaluation_prompt(question: str, correct_answer: str, user_answer: str) -> str:
    return f"""
        You are an educational assessment AI. Evaluate the student's answer to a question.

        Question: {question}

        Correct Answer: {correct_answer}

        Student's Answer: {user_answer}

        Provide your evaluation in JSON format with two fields:
        1. "score": Must be exactly one of: "correct", "mostly correct", "mostly incorrect", "incorrect"
        2. "feedback": A constructive evaluation (max 1000 characters) that:
            - Highlights what the student got right
            - Points out what was missing or incorrect
            - Provides guidance for improvement if needed
            - Is encouraging and educational in tone

        Be fair and considerate. If the student's answer conveys the same meaning as the correct answer or close enough to it (even with different wording), consider it correct or mostly correct.

        Example format:
        {{"score": "mostly correct", "feedback": "Your answer demonstrates a solid understanding of the core concept..."}}

        Provide your evaluation:
        """

def get_batch_answer_evaluation_prompt(question: str, correct_answer: str, user_answers: list) -> str:
    answers_text = ""
    for i, ans in enumerate(user_answers):
        answers_text += f"Student {i}: {ans}\n"

    return f"""
        You are an educational assessment AI. Evaluate multiple students' answers to the SAME question.

        Question: {question}

        Correct Answer: {correct_answer}

        Students' Answers:
        {answers_text}

        Provide your evaluations in JSON format as a list of objects. Each object MUST correspond to the student's index and include:
        1. "index": The student's index (0, 1, 2, etc.)
        2. "score": Must be exactly one of: "correct", "mostly correct", "mostly incorrect", "incorrect"
        3. "feedback": A constructive evaluation (max 1000 characters) that:
            - Highlights what the student got right
            - Points out what was missing or incorrect
            - Provides guidance for improvement if needed
            - Is encouraging and educational in tone

        Be fair and considerate. If the student's answer conveys the same meaning as the correct answer or close enough to it (even with different wording), consider it correct or mostly correct.

        Example format:
        [
            {{"index": 0, "score": "correct", "feedback": "Excellent work..."}},
            {{"index": 1, "score": "mostly incorrect", "feedback": "You missed the core part..."}}
        ]

        Provide your evaluations as a JSON list:
        """

def get_feedback_analysis_prompt(
        feedback_count: int,
        good_count: int,
        satisfactory_count: int,
        poor_count: int,
        open_feedback_comments: list,
        channel_summaries: dict
) -> str:
    prompt = "Please analyze the following data and provide comprehensive insights:\n\n"

    if feedback_count > 0:
        prompt += "=== FEEDBACK STATISTICS ===\n"
        prompt += f"Total feedback entries: {feedback_count}\n\n"

        prompt += f"Good: {good_count} ({good_count/feedback_count*100:.1f}%)\n"
        prompt += f"Satisfactory: {satisfactory_count} ({satisfactory_count/feedback_count*100:.1f}%)\n"
        prompt += f"Poor: {poor_count} ({poor_count/feedback_count*100:.1f}%)\n\n"

        if open_feedback_comments:
            prompt += "Summarized Open Feedback Comments:\n"
            for comment in open_feedback_comments:
                prompt += f"- {comment}\n"
            prompt += "\n"

    for channel_name, summary in channel_summaries.items():
        prompt += f"=== CHANNEL: {channel_name} SUMMARY ===\n"
        prompt += f"{summary}\n\n"

    prompt += """
        Please provide a structured analysis with the following format:

        -Feedback Analysis:
        [Analyze the feedback statistics and open comments. Provide 2-3 key insights about satisfaction levels and themes from feedback]

        -Channel Activity Analysis:
        [For EACH channel that has data, provide 1-2 key observations about the discussions, questions, or topics based on the summaries provided]

        -Summary & Recommendations:
        [Provide overall summary and 2-3 actionable recommendations for instructors]

        IMPORTANT:
        - Keep your TOTAL response under 1500 characters
        - Be concise and focus on the most important insights
        - Follow the exact structure above with these 3 sections
        - If a data source has no data, briefly mention it and skip analysis
        """

    return prompt

def get_summarization_prompt(data_type: str, content: str) -> str:
    return f"""
        Summarize the following {data_type} to identify key themes, recurring questions, or significant observations.
        Be concise but ensure all important insights are captured.

        {data_type}:
        {content}

        Provide a summary (max 1000 characters):
        """
