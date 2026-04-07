import asyncio
import discord
from REST.AI.gemini_service import get_gemini_service
import REST.utils.bot_context as bc

_guilds = {}
_channels = {}
_members = {}
_roles = {}
_member_counts = {"online": 0, "offline": 0, "total": 0}

# Helper: get the current live bot instance
def _bot():
    return bc.get_live_bot()

# Functions to retrieve Discord server information
def get_guild_info():
    """Get information about all guilds the bot is connected to.
    The bot is usually connected to only one, but this is the correct way since bot.guilds returns a list.

    Returns:
        dict: Information about the guilds.
    """
    global _guilds

    try:
        # Collect guild data
        guilds_data = {}
        for guild in _bot().guilds:
            guilds_data[str(guild.id)] = {
                "id": str(guild.id),
                "name": guild.name,
                "member_count": guild.member_count,
                "icon_url": str(guild.icon.url) if guild.icon else None,
                "description": guild.description,
                "created_at": guild.created_at.isoformat() if guild.created_at else None,
                "owner_id": str(guild.owner_id) if guild.owner_id else None
            }

        # Store in global variable for caching
        _guilds = guilds_data
        return guilds_data
    except Exception as e:
        print(f"Error getting guild info: {e}")
        return None

def get_channels():
    """Get information about channels in the guilds.

    Returns:
        dict: Information about the channels.
    """
    global _channels

    try:
        # Collect channel data
        channels_data = {}
        for guild in _bot().guilds:
            guild_channels = []
            for channel in guild.channels:
                channel_data = {
                    "id": str(channel.id),
                    "name": channel.name,
                    "type": str(channel.type),
                    "position": channel.position,
                    "category_id": str(channel.category_id) if channel.category_id else None
                }

                if hasattr(channel, 'topic') and channel.topic:
                    channel_data["topic"] = channel.topic

                guild_channels.append(channel_data)

            channels_data[str(guild.id)] = guild_channels

        # Store in global variable for caching
        _channels = channels_data
        return channels_data
    except Exception as e:
        print(f"Error getting channels: {e}")
        return None

def get_members():
    """Get information about members in the guilds.

    Returns:
        dict: Information about the members.
    """
    global _members, _member_counts

    try:
        # Collect member data
        members_data = {}
        all_online = 0
        all_offline = 0
        all_total = 0

        for guild in _bot().guilds:
            guild_members = []
            online_count = 0
            offline_count = 0

            for member in guild.members:
                # Skip bots
                if member.bot:
                    continue

                member_roles = [str(role.id) for role in member.roles if role.id != guild.id]

                status = "offline"
                if member.status == discord.Status.online or member.status == discord.Status.idle or member.status == discord.Status.dnd:
                    status = "online"
                    online_count += 1
                else:
                   offline_count += 1

                member_data = {
                    "id": str(member.id),
                    "name": member.name,
                    "display_name": member.display_name,
                    "discriminator": member.discriminator,
                    "bot": member.bot,
                    "avatar_url": str(member.avatar.url) if member.avatar else None,
                    "roles": member_roles,
                    "joined_at": member.joined_at.isoformat() if member.joined_at else None,
                    "status": status
                }

                guild_members.append(member_data)

            members_data[str(guild.id)] = guild_members

            all_online += online_count
            all_offline += offline_count
            all_total += len(guild_members)

        # Update member counts
        _member_counts = {
            "online": all_online,
            "offline": all_offline,
            "total": all_total
        }

        # Store in global variable for caching
        _members = members_data
        return members_data
    except Exception as e:
        print(f"Error getting members: {e}")
        return None

def get_roles():
    """Get information about roles in the guilds.

    Returns:
        dict: Information about the roles.
    """
    global _roles

    try:
        # Collect role data
        roles_data = {}
        for guild in _bot().guilds:
            guild_roles = []
            for role in guild.roles:
                # Skip the @everyone role
                if role.name == "@everyone":
                    continue

                role_data = {
                    "id": str(role.id),
                    "name": role.name,
                    "color": str(role.color),
                    "position": role.position,
                    "mentionable": role.mentionable,
                    "permissions": str(role.permissions.value)
                }

                guild_roles.append(role_data)

            roles_data[str(guild.id)] = guild_roles

        # Store in global variable for caching
        _roles = roles_data
        return roles_data
    except Exception as e:
        print(f"Error getting roles: {e}")
        return None


def get_member_counts():
    """Get the count of online and offline members.

    Returns:
        dict: Counts of online and offline members.
    """
    global _member_counts

    try:
        # If member counts are already cached, return them
        if _member_counts["total"] > 0:
            return _member_counts

        # Otherwise, refresh member data to update counts
        get_members()
        return _member_counts
    except Exception as e:
        print(f"Error getting member counts: {e}")
        return {"online": 0, "offline": 0, "total": 0}


def assign_member_role(user_id: int, role_id: int):
    try:
        print(_bot().guilds[0].name)
        role = _bot().guilds[0].get_role(int(role_id))
        member = _bot().guilds[0].get_member(user_id)

        # Create a coroutine for adding roles and run it using asyncio
        async def add_role_coroutine():
            await member.add_roles(role)

        # Run the coroutine in the bot's event loop
        future = asyncio.run_coroutine_threadsafe(add_role_coroutine(), _bot().loop)
        # Wait for the result, with a timeout
        future.result(timeout=10)

        return {"status": "success", "message": "Role assigned successfully"}
    except Exception as e:
        print(f"Error assigning member role: {e}")
        return {"status": "failure", "message": "Error assigning member role: " + str(e)}


def remove_member_role(user_id: int, role_id: int):
    try:
        role = _bot().guilds[0].get_role(int(role_id))
        member = _bot().guilds[0].get_member(user_id)

        async def remove_role_coroutine():
            await member.remove_roles(role)

        future = asyncio.run_coroutine_threadsafe(remove_role_coroutine(), _bot().loop)
        future.result(timeout=10)

        return {"status": "success", "message": "Role removed successfully"}
    except Exception as e:
        print(f"Error removing member role: {e}")
        return {"status": "failure", "message": "Error removing member role: " + str(e)}


def send_qa_to_channel(channel_id: int, qa_pairs: list, duration: float, ai_feedback: bool = False):
    """
    Send Q&A quiz to a Discord channel.

    Args:
        channel_id: Discord channel ID
        qa_pairs: List of Q&A pairs [{"question": "...", "answer": "..."}]
        duration: Time in seconds the quiz will be active
        ai_feedback: Whether AI feedback is enabled for this quiz
    """
    try:
        from bot.ui.view import QAView

        channel = _bot().get_channel(channel_id)
        if not channel:
            return {"status": "failure", "message": f"Channel with ID {channel_id} not found"}

        view_instance = None

        async def send_message():
            nonlocal view_instance
            embed = discord.Embed(
                title="📝 Q&A Quiz Available!",
                description=f"A new quiz with {len(qa_pairs)} question(s) is ready.\nClick the button below to start!",
                color=discord.Color.blue()
            )
            embed.add_field(
                name="⏱️ Time Remaining",
                value=f"{int(duration)} seconds",
                inline=True
            )
            embed.add_field(
                name="📊 Questions",
                value=str(len(qa_pairs)),
                inline=True
            )
            embed.set_footer(text="Click 'Start Quiz' to begin answering questions")

            view = QAView(qa_pairs=qa_pairs, duration=duration, ai_feedback_enabled=ai_feedback)
            view_instance = view

            message = await channel.send(embed=embed, view=view)
            view.message = message

            view.start_countdown()

        future = asyncio.run_coroutine_threadsafe(send_message(), _bot().loop)
        future.result(timeout=10)

        return {"status": "success", "message": f"Q&A quiz sent to channel {channel_id}", "view": view_instance}
    except Exception as e:
        print(f"Error sending Q&A to channel: {e}")
        import traceback
        traceback.print_exc()
        return {"status": "failure", "message": f"Error sending Q&A to channel: {str(e)}"}


def schedule_send_qa_results(view, duration: float, qa_pairs: list, ai_feedback: bool = False, gemini_api_key: str = None):
    """
    Schedule sending correct answers to participants after the quiz duration expires.

    Args:
        view: QAView instance containing participant responses
        duration: Time in seconds to wait before sending results
        qa_pairs: List of Q&A pairs with correct answers
        ai_feedback: Whether to use AI to provide detailed feedback
        gemini_api_key: Gemini API key for AI feedback (required if ai_feedback is True)
    """

    async def send_results_after_timeout_async():
        await asyncio.sleep(duration + 5)

        try:
            participants = view.responses

            if not participants:
                print("No participants completed the quiz")
                return

            gemini_service = None
            if ai_feedback and gemini_api_key:
                try:
                    gemini_service = get_gemini_service(gemini_api_key)
                    print("AI feedback enabled for quiz results")
                except Exception as e:
                    print(f"Failed to initialize Gemini service: {e}. Falling back to basic feedback.")

            if ai_feedback and gemini_service and participants:
                print(f"Generating AI feedback for {len(participants)} participants...")

                questions_to_evaluate = {} # {question_index: {question, correct_answer, user_answers: [{participant_index, answer_data}]}}
                
                for p_idx, participant in enumerate(participants):
                    for a_idx, answer_data in enumerate(participant['answers']):
                        if a_idx not in questions_to_evaluate:
                            questions_to_evaluate[a_idx] = {
                                'question': answer_data['question'],
                                'correct_answer': answer_data['correct_answer'],
                                'items': []
                            }
                        questions_to_evaluate[a_idx]['items'].append({
                            'p_idx': p_idx,
                            'a_idx': a_idx,
                            'user_answer': answer_data['user_answer']
                        })

                semaphore = asyncio.Semaphore(3) # Limit concurrent API calls

                async def process_question_batch(q_idx, q_data):
                    async with semaphore:
                        question = q_data['question']
                        correct_answer = q_data['correct_answer']
                        items = q_data['items']
                        user_answers = [item['user_answer'] for item in items]
                        
                        try:
                            evaluations = await gemini_service.evaluate_answers_batch(question, correct_answer, user_answers)
                            
                            for item, evaluation in zip(items, evaluations):
                                p_idx = item['p_idx']
                                a_idx = item['a_idx']
                                score = evaluation.get('score', 'incorrect')
                                feedback = evaluation.get('feedback', 'No feedback available')
                                
                                participants[p_idx]['answers'][a_idx]['ai_evaluation'] = {
                                    'score': score,
                                    'feedback': feedback
                                }
                        except Exception as e:
                            print(f"Error in batch evaluation for question {q_idx + 1}: {e}")

                await asyncio.gather(*(process_question_batch(q_idx, q_data) for q_idx, q_data in questions_to_evaluate.items()))

            for participant in participants:
                user_id = participant['user_id']
                student_name = participant['student']

                if ai_feedback and gemini_service:
                    message_parts = ["🤖 Quiz Results - AI Feedback:\n"]
                else:
                    message_parts = ["📊 Quiz Results - Correct Answers:\n"]

                for i, answer_data in enumerate(participant['answers'], 1):
                    question = answer_data['question']
                    correct_answer = answer_data['correct_answer']
                    user_answer = answer_data['user_answer']
                    ai_eval = answer_data.get('ai_evaluation')

                    message_parts.append(f"\n{'='*50}")
                    message_parts.append(f"Question {i}: {question}")
                    message_parts.append(f"{'='*50}")

                    if ai_eval:
                        score = ai_eval.get('score', 'incorrect')
                        feedback = ai_eval.get('feedback', 'No feedback available')
                        
                        score_emoji = {
                            "correct": "✅",
                            "mostly correct": "✓",
                            "mostly incorrect": "⚠️",
                            "incorrect": "❌"
                        }.get(score, "❓")

                        message_parts.append(f"\n{score_emoji} Score: {score.upper()}")
                        message_parts.append(f"\n📝 Your Answer:")
                        message_parts.append(f"{user_answer}")
                        message_parts.append(f"\n✅ Correct Answer:")
                        message_parts.append(f"{correct_answer}")
                        message_parts.append(f"\n💬 AI Feedback:")
                        message_parts.append(f"{feedback}")
                    elif ai_feedback and gemini_service:
                        message_parts.append(f"\n⚠️ AI feedback temporarily unavailable for this question.")
                        message_parts.append(f"\n📝 Your Answer: {user_answer}")
                        message_parts.append(f"✅ Correct Answer: {correct_answer}")
                    else:
                        message_parts.append(f"\n✅ Correct Answer: {correct_answer}")
                        message_parts.append(f"📝 Your Answer: {user_answer}")

                message = "\n".join(message_parts)

                def chunk_message(text, max_length=2000):
                    chunks = []
                    while len(text) > max_length:
                        split_pos = text.rfind('\n', 0, max_length)
                        if split_pos == -1:
                            split_pos = max_length
                        chunks.append(text[:split_pos])
                        text = text[split_pos:].lstrip('\n')
                    if text:
                        chunks.append(text)
                    return chunks

                message_chunks = chunk_message(message)

                try:
                    user = await _bot().fetch_user(user_id)
                    for chunk in message_chunks:
                        await user.send(chunk)
                        if len(message_chunks) > 1:
                            await asyncio.sleep(0.5)
                    print(f"Successfully sent results to {student_name} (ID: {user_id})")
                except discord.errors.Forbidden:
                    print(f"Cannot send DM to {student_name} - user has DMs disabled")
                except Exception as e:
                    print(f"Error sending DM to {student_name}: {e}")

            print(f"Finished sending results to {len(participants)} participant(s)")

            if ai_feedback and gemini_service and hasattr(view, 'path'):
                try:
                    import json
                    with open(view.path, 'w', encoding='utf-8') as f:
                        json.dump({
                            'timestamp': view.responses[0].get('timestamp') if view.responses else None,
                            'ai_feedback_enabled': True,
                            'total_responses': len(view.responses),
                            'total_questions': len(qa_pairs),
                            'responses': view.responses
                        }, f, indent=2, ensure_ascii=False)
                    print(f"Updated responses file with AI evaluations: {view.path}")
                except Exception as e:
                    print(f"Error updating responses file with AI evaluations: {e}")

        except Exception as e:
            print(f"Error in send_results_after_timeout: {e}")
            import traceback
            traceback.print_exc()

    asyncio.run_coroutine_threadsafe(send_results_after_timeout_async(), _bot().loop)
    print(f"Scheduled sending results to participants after {duration} seconds")

