import json
import logging
from pathlib import Path
from flask import Blueprint, jsonify, request
from REST.api.api_validation import requires_api_key

stats_bp = Blueprint('ai_statistics', __name__)

PROJECT_ROOT = Path(__file__).resolve().parents[2]

@stats_bp.route('/api/ai/qa-statistics', methods=['GET'])
@requires_api_key
def get_qa_statistics():
    """
    Get statistics from all Q&A quiz responses.

    Query Parameters:
        - api_key (str): API key for authentication
        - ai_feedback (bool): Filter by AI feedback enabled (true/false)
    """
    try:
        ai_feedback_filter = request.args.get('ai_feedback')

        if ai_feedback_filter is not None:
            ai_feedback_filter = ai_feedback_filter.lower() == 'true'

        qa_responses_dir = PROJECT_ROOT / 'data' / 'qa_responses'
        if not qa_responses_dir.exists():
            return jsonify({
                'success': True,
                'statistics': {
                    'total_quizzes': 0,
                    'total_responses': 0,
                    'quizzes_with_ai_feedback': 0,
                    'quizzes_without_ai_feedback': 0,
                    'average_responses_per_quiz': 0,
                    'ai_feedback_stats': None,
                    'quizzes': []
                }
            }), 200

        all_quizzes = []
        total_responses = 0
        quizzes_with_ai = 0
        quizzes_without_ai = 0
        ai_score_distribution = {
            'correct': 0,
            'mostly correct': 0,
            'mostly incorrect': 0,
            'incorrect': 0
        }
        total_ai_answers = 0

        for response_file in qa_responses_dir.glob('qa_responses_*.json'):
            try:
                with open(response_file, 'r', encoding='utf-8') as f:
                    data = json.load(f)

                timestamp = data.get('timestamp')

                ai_feedback_enabled = data.get('ai_feedback_enabled', False)

                if ai_feedback_filter is not None and ai_feedback_enabled != ai_feedback_filter:
                    continue

                if ai_feedback_enabled:
                    quizzes_with_ai += 1
                else:
                    quizzes_without_ai += 1

                total_responses += data.get('total_responses', 0)

                questions_stats = {}

                for response in data.get('responses', []):
                    for answer in response.get('answers', []):
                        question = answer.get('question')
                        correct_answer = answer.get('correct_answer')

                        if question not in questions_stats:
                            questions_stats[question] = {
                                'question': question,
                                'correct_answer': correct_answer,
                                'total_answers': 0,
                                'score_distribution': {
                                    'correct': 0,
                                    'mostly correct': 0,
                                    'mostly incorrect': 0,
                                    'incorrect': 0
                                }
                            }

                        questions_stats[question]['total_answers'] += 1

                        ai_eval = answer.get('ai_evaluation')
                        if ai_eval:
                            score = ai_eval.get('score', 'incorrect')
                            if score in questions_stats[question]['score_distribution']:
                                questions_stats[question]['score_distribution'][score] += 1

                            if score in ai_score_distribution:
                                ai_score_distribution[score] += 1
                                total_ai_answers += 1

                questions_list = list(questions_stats.values())

                all_quizzes.append({
                    'timestamp': timestamp,
                    'ai_feedback_enabled': ai_feedback_enabled,
                    'total_responses': data.get('total_responses', 0),
                    'total_questions': data.get('total_questions', 0),
                    'filename': response_file.name,
                    'questions': questions_list
                })

            except Exception as e:
                logging.warning(f"Error reading response file {response_file}: {e}")
                continue

        total_quizzes = len(all_quizzes)
        average_responses_per_quiz = (total_responses / total_quizzes) if total_quizzes > 0 else 0

        ai_feedback_stats = None
        if total_ai_answers > 0:
            correct_count = ai_score_distribution['correct'] + ai_score_distribution['mostly correct']
            average_correct_rate = (correct_count / total_ai_answers) * 100

            ai_feedback_stats = {
                'average_correct_rate': round(average_correct_rate, 2),
                'total_evaluated_answers': total_ai_answers,
                'score_distribution': ai_score_distribution
            }

        all_quizzes.sort(key=lambda x: x['timestamp'] if x['timestamp'] else '', reverse=True)

        return jsonify({
            'success': True,
            'statistics': {
                'total_quizzes': total_quizzes,
                'total_responses': total_responses,
                'quizzes_with_ai_feedback': quizzes_with_ai,
                'quizzes_without_ai_feedback': quizzes_without_ai,
                'average_responses_per_quiz': round(average_responses_per_quiz, 2),
                'ai_feedback_stats': ai_feedback_stats,
                'quizzes': all_quizzes
            }
        }), 200

    except ValueError as e:
        logging.error(f"Validation error: {str(e)}")
        return jsonify({
            'success': False,
            'error': f'Invalid query parameters: {str(e)}'
        }), 400
    except Exception as e:
        import traceback
        error_trace = traceback.format_exc()
        logging.error(f"Server error: {error_trace}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500
