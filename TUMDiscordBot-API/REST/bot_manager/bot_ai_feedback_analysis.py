from flask import Blueprint, jsonify, request
from datetime import datetime
from pathlib import Path
import csv
import os
import asyncio
import logging
from typing import List, Dict

import REST.utils.bot_context as bc
from REST.api.api_validation import requires_api_key
from REST.AI.gemini_service import get_gemini_service
from REST.AI.prompts import get_feedback_analysis_prompt

logger = logging.getLogger('discord_bot')

ai_feedback_analysis_bp = Blueprint('ai_feedback_analysis', __name__)


def load_feedback_statistics(start_date: datetime, end_date: datetime) -> List[Dict[str, str]]:
    """Load feedback statistics from the corresponding data folder within the given time range."""
    try:
        project_root = Path(__file__).resolve().parents[2]
        feedback_dir = project_root / 'data' / 'tutor_session_feedback'

        logger.info(f"Looking for feedback files in: {feedback_dir}")

        if not feedback_dir.exists():
            logger.warning(f"Feedback directory does not exist: {feedback_dir}")
            return []

        all_feedback = []

        for filename in os.listdir(feedback_dir):
            if not filename.endswith('.csv'):
                continue

            file_path = feedback_dir / filename

            file_mtime = datetime.fromtimestamp(file_path.stat().st_mtime)
            logger.info(f"Checking file {filename}: modified {file_mtime}, range {start_date} to {end_date}")

            if start_date <= file_mtime <= end_date:
                try:
                    with open(file_path, 'r', encoding='utf-8') as f:
                        csv_reader = csv.DictReader(f)
                        rows = list(csv_reader)
                        all_feedback.extend(rows)
                        logger.info(f"Loaded {len(rows)} entries from {filename}")
                except Exception as e:
                    logger.error(f"Error reading {filename}: {e}")
                    continue

        logger.info(f"Total feedback entries loaded: {len(all_feedback)}")
        return all_feedback
    except Exception as e:
        logger.error(f"Error loading feedback statistics: {e}", exc_info=True)
        return []


async def load_channel_history(channel_id: int, start_date: datetime, end_date: datetime) -> List[Dict[str, str]]:
    """Load Discord channel messages history (with author and content) within the given time range."""
    logger.info(f"load_channel_history called: channel_id={channel_id}, start_date={start_date}, end_date={end_date}")
    logger.info(f"Bot running: {bc.bot_running}, mock_ctx exists: {bc.mock_ctx is not None}")

    if not bc.bot_running or not bc.mock_ctx:
        logger.warning("Bot not running or mock_ctx not available")
        return []

    try:
        channel = bc.mock_ctx.guild.get_channel(channel_id)
        logger.info(f"Channel object retrieved: {channel is not None}, type: {type(channel) if channel else None}")

        if not channel:
            logger.warning(f"Channel {channel_id} not found in guild")
            return []

        messages = []
        message_count = 0
        bot_message_count = 0
        filtered_out_count = 0

        logger.info(f"Starting to fetch messages from channel {channel_id}")
        async for message in channel.history(limit=5000, after=start_date):
            message_count += 1

            if message_count <= 3:
                logger.info(f"Message {message_count}: author={message.author.name}, bot={message.author.bot}, created_at={message.created_at}")

            if message.author.bot:
                bot_message_count += 1
                continue

            message_time = message.created_at.replace(tzinfo=None)

            if message_time > end_date:
                filtered_out_count += 1
                if filtered_out_count <= 3:
                    logger.info(f"Message filtered out: time={message_time} > end_date={end_date}")
                continue

            messages.append({
                'author': f"{message.author.display_name} ({message.author.name})",
                'content': message.content,
                'timestamp': message.created_at.isoformat()
            })

        logger.info(f"Channel {channel_id}: fetched {message_count} total messages, {bot_message_count} bot messages, {filtered_out_count} filtered by date, {len(messages)} final messages")
        return messages
    except Exception as e:
        logger.error(f"Error loading channel history for {channel_id}: {e}", exc_info=True)
        return []


@ai_feedback_analysis_bp.route('/api/ai/feedback-analysis', methods=['POST'])
@requires_api_key
def get_ai_feedback_analysis():
    """
    Analyze feedback and channel data using AI.

    Request Body:
        use_statistics (bool): Include feedback statistics
        channel_ids (list): List of Discord channel IDs to include in analysis (can be empty)
        start_date (str): Start date in format DD-MM-YYYY or YYYY-MM-DD
        end_date (str): End date in format DD-MM-YYYY or YYYY-MM-DD
    """
    try:
        logger.info(f"AI feedback analysis request: method={request.method}")

        data = request.get_json()
        if not data:
            return jsonify({"status": "error", "message": "Request body is required"}), 400
        use_statistics = data.get('use_statistics', False)
        channel_ids = [int(cid) for cid in data.get('channel_ids', [])]
        start_date_str = data.get('start_date')
        end_date_str = data.get('end_date')

        logger.info(f"Parameters: use_statistics={use_statistics}, channel_ids={channel_ids}, "
                   f"start_date={start_date_str}, end_date={end_date_str}")

        if not start_date_str or not end_date_str:
            return jsonify({"status": "error", "message": "start_date and end_date are required"}), 400

        try:
            if '-' in start_date_str and start_date_str.index('-') < 3:
                start_date = datetime.strptime(start_date_str, "%d-%m-%Y")
                end_date = datetime.strptime(end_date_str, "%d-%m-%Y")
            else:
                start_date = datetime.strptime(start_date_str, "%Y-%m-%d")
                end_date = datetime.strptime(end_date_str, "%Y-%m-%d")

            end_date = end_date.replace(hour=23, minute=59, second=59)
        except ValueError:
            return jsonify({"status": "error", "message": "Invalid date format. Use DD-MM-YYYY or YYYY-MM-DD"}), 400

        if start_date >= end_date:
            return jsonify({"status": "error", "message": "startDate must be before endDate"}), 400

        feedback_data = []
        channel_data = {}

        if use_statistics:
            logger.info("Loading feedback statistics...")
            feedback_data = load_feedback_statistics(start_date, end_date)
            logger.info(f"Loaded {len(feedback_data)} feedback entries")

        if channel_ids:
            if not bc.bot_running:
                return jsonify({"status": "error", "message": "Discord bot is not running"}), 503

            async def fetch_channel_data():
                channel_messages = {}
                for channel_id in channel_ids:
                    logger.info(f"Loading channel history for channel ID: {channel_id}")
                    messages = await load_channel_history(channel_id, start_date, end_date)

                    channel = bc.mock_ctx.guild.get_channel(channel_id)
                    channel_name = channel.name if channel else f"Channel {channel_id}"

                    channel_messages[channel_name] = {
                        'id': channel_id,
                        'messages': messages
                    }
                    logger.info(f"Loaded {len(messages)} messages from channel {channel_id} ({channel_name})")
                return channel_messages

            loop = bc.get_live_loop()
            future = asyncio.run_coroutine_threadsafe(fetch_channel_data(), loop)
            channel_data = future.result(timeout=30)


        logger.info("Formatting data for AI analysis...")

        good_count = sum(1 for f in feedback_data if f.get('Feedback') == 'Good')
        satisfactory_count = sum(1 for f in feedback_data if f.get('Feedback') == 'Satisfactory')
        poor_count = sum(1 for f in feedback_data if f.get('Feedback') == 'Poor')

        open_feedback_comments = [entry.get('Open Feedback') for entry in feedback_data if entry.get('Open Feedback')]

        try:
            gemini_service = get_gemini_service()
        except ValueError:
            logger.error("Gemini API key not configured in settings")
            return jsonify({"status": "error", "message": "Gemini API key not configured"}), 500

        async def process_ai_analysis():
            summarized_open_feedback = []
            if open_feedback_comments:
                full_feedback_text = "\n".join(open_feedback_comments)
                if len(full_feedback_text) > 4000:
                    logger.info(f"Open feedback too large ({len(full_feedback_text)} chars), summarizing in chunks...")
                    chunks = [full_feedback_text[i:i + 4000] for i in range(0, len(full_feedback_text), 4000)]
                    for i, chunk in enumerate(chunks):
                        summary = await gemini_service.summarize_content(f"Feedback Part {i+1}", chunk)
                        summarized_open_feedback.append(summary)
                else:
                    summarized_open_feedback = open_feedback_comments

            channel_summaries = {}
            for channel_name, ch_data in channel_data.items():
                messages = ch_data['messages']
                if not messages:
                    channel_summaries[channel_name] = "No messages in this channel."
                    continue

                formatted_messages = "\n".join([f"[{m['timestamp']}] {m['author']}: {m['content']}" for m in messages])

                if len(formatted_messages) > 4000:
                    logger.info(f"Channel {channel_name} history too large ({len(formatted_messages)} chars), summarizing in chunks...")
                    chunks = [formatted_messages[i:i + 4000] for i in range(0, len(formatted_messages), 4000)]
                    chunk_summaries = []
                    for i, chunk in enumerate(chunks):
                        summary = await gemini_service.summarize_content(f"Discord Messages from {channel_name} (Part {i+1})", chunk)
                        chunk_summaries.append(summary)

                    if len(chunk_summaries) > 1:
                        combined_summary_text = "\n".join(chunk_summaries)
                        final_channel_summary = await gemini_service.summarize_content(f"Combined Summaries for {channel_name}", combined_summary_text)
                        channel_summaries[channel_name] = final_channel_summary
                    else:
                        channel_summaries[channel_name] = chunk_summaries[0]
                else:
                    channel_summaries[channel_name] = formatted_messages

            ai_prompt = get_feedback_analysis_prompt(
                feedback_count=len(feedback_data),
                good_count=good_count,
                satisfactory_count=satisfactory_count,
                poor_count=poor_count,
                open_feedback_comments=summarized_open_feedback,
                channel_summaries=channel_summaries
            )
            logger.info(f"AI prompt generated (length: {len(ai_prompt)} chars)")

            feedback_with_open = sum(1 for f in feedback_data if f.get('Open Feedback'))

            stats_header = "-Data Processed:\n"
            stats_header += f"Total feedback entries: {len(feedback_data)}\n"
            stats_header += f"Feedback with open comments: {feedback_with_open}\n"

            if channel_data:
                for channel_name, ch_data in channel_data.items():
                    stats_header += f"{channel_name}: {len(ch_data['messages'])} messages\n"

            stats_header += "\n---\n\n"

            try:
                logger.info("Sending request to Gemini AI...")
                analysis = await gemini_service.generate_feedback_analysis(ai_prompt)
                logger.info(f"AI analysis received (length: {len(analysis)} chars)")

                if len(analysis) > 1500:
                    logger.warning(f"AI analysis exceeds 1500 chars ({len(analysis)}), truncating and asking for more concise summary...")
                    analysis = analysis[:1497] + "..."

                return stats_header + analysis
            except Exception as e:
                logger.error(f"Failed to generate AI analysis: {e}", exc_info=True)
                return stats_header + "Failed to get AI analysis"

        if bc.bot_running:
            loop = bc.get_live_loop()
            future = asyncio.run_coroutine_threadsafe(process_ai_analysis(), loop)
            analysis = future.result(timeout=120)
        else:
            analysis = asyncio.run(process_ai_analysis())

        logger.info("AI feedback analysis completed successfully")

        channel_message_counts = {channel_name: len(ch_data['messages']) for channel_name, ch_data in channel_data.items()}

        return jsonify({
            "success": True,
            "status": "success",
            "analysis": analysis,
            "metadata": {
                "startDate": start_date_str,
                "endDate": end_date_str,
                "feedbackCount": len(feedback_data),
                "channelMessageCounts": channel_message_counts,
                "totalChannelMessages": sum(channel_message_counts.values())
            }
        })

    except Exception as e:
        logger.error(f"Error in AI feedback analysis: {str(e)}", exc_info=True)
        return jsonify({
            "status": "error",
            "message": f"An error occurred: {str(e)}"
        }), 500
