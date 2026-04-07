"""
Contains various views used when interacting with the bot.

:copyright: (c) 2023-present Ivan Parmacli
:license: MIT, see LICENSE for more details.
"""

import discord
import logging
import asyncio
import REST.utils.bot_context as bc
from pathlib import Path

from discord.enums import ButtonStyle
from shared import SurveyEntry
from utility import save_survey_entry_to_csv
from datetime import datetime
from bot.ui.button import DynamicButton

# Get the logger configured in app.py
logger = logging.getLogger('discord_bot')


class FeedbackModal(discord.ui.Modal):
    """Modal for collecting open feedback text."""

    def __init__(self, parent_view, feedback_type: str):
        super().__init__(title=f"{feedback_type} Feedback")
        self.parent_view = parent_view
        self.feedback_type = feedback_type

        self.add_item(discord.ui.InputText(
            label="Additional Comments (Optional)",
            placeholder="Share any additional thoughts or feedback...",
            style=discord.InputTextStyle.long,
            required=False,
            max_length=1000
        ))

    async def callback(self, interaction: discord.Interaction):
        """Handle modal submission."""
        # Check if time has expired
        if self.parent_view.start_time:
            elapsed = (datetime.now() - self.parent_view.start_time).total_seconds()
            if elapsed >= self.parent_view.duration:
                await interaction.response.send_message(
                    "⏱️ Time has expired. Your submission cannot be accepted.",
                    ephemeral=True
                )
                return

        open_feedback_text = self.children[0].value

        if not open_feedback_text:
            open_feedback_text = "No feedback provided."

        member = interaction.guild.get_member(interaction.user.id)
        student_name = f"{member.display_name} ({interaction.user.name})"

        entry = SurveyEntry(
            student_name,
            {"Feedback": self.feedback_type},
            open_feedback=open_feedback_text
        )

        if interaction.user.id not in self.parent_view.users_interacted_with_view:
            self.parent_view.users_interacted_with_view.append(interaction.user.id)

        if self.feedback_type == "Good":
            self.parent_view.users_good_review.append(entry)
        elif self.feedback_type == "Satisfactory":
            self.parent_view.users_satisfactory_review.append(entry)
        elif self.feedback_type == "Poor":
            self.parent_view.users_poor_review.append(entry)

        save_survey_entry_to_csv(self.parent_view.path, entry)

        await interaction.response.edit_message(
            embed=self.parent_view.update_percentage(interaction.message.embeds[0])
        )


def _bot():
    return bc.get_live_bot()


class TutorSessionView(discord.ui.View):
    """Represents a custom UI view.
    A view that is used to collect the student's feedback regarding the specified tutor session with the use of embeded message and buttons.
    The students see the survey as anonymous, but we still save the name of the student for each entry.

    Parameters
    ----------
    *items: :class:`Item`
        The initial items attached to this view.
    timeout: Optional[:class:`float`]
        Timeout in seconds from last interaction with the UI before no longer accepting input.
        If ``None`` then there is no timeout.
    group_id: :class:`str`
        Tutor group id, e.g. 'g5'

    Attributes
    ----------
    timeout: Optional[:class:`float`]
        Timeout from last interaction with the UI before no longer accepting input.
        If ``None`` then there is no timeout.
    children: List[:class:`Item`]
        The list of children attached to this view.
    disable_on_timeout: :class:`bool`
        Whether to disable the view when the timeout is reached. Defaults to ``False``.
    message: Optional[:class:`.Message`]
        The message that this view is attached to.
        If ``None`` then the view has not been sent with a message.
    parent: Optional[:class:`.Interaction`]
        The parent interaction which this view was sent from.
        If ``None`` then the view was not sent using :meth:`InteractionResponse.send_message`.
    group_id: :class:`str`
        Tutor group id.
    """

    def __init__(self, group_id: str, duration):
        super().__init__(timeout=duration, disable_on_timeout=True)
        self.group_id = group_id
        self.duration = duration
        self.start_time = None
        self.message = None
        self.countdown_task = None
        self.users_interacted_with_view = []
        self.users_good_review = []
        self.users_satisfactory_review = []
        self.users_poor_review = []
        self.good_feedback_percentage = 0
        self.mid_feedback_percentage = 0
        self.bad_feedback_percentage = 0
        current_time = datetime.now().strftime("%Y-%m-%d_%H-%M")
        # Resolve absolute path for tutor session feedback
        project_root = Path(__file__).resolve().parents[2]
        feedback_dir = project_root / 'data' / 'tutor_session_feedback'
        feedback_dir.mkdir(exist_ok=True, parents=True)
        self.path = str(feedback_dir / f"{group_id}_{current_time}.csv")

    @discord.ui.button(label="Good", style=ButtonStyle.primary)
    async def good_callback(
            self, button: discord.ui.Button, interaction: discord.Interaction
    ):
        if interaction.user.id not in self.users_interacted_with_view:
            # Show modal for open feedback
            modal = FeedbackModal(self, "Good")
            await interaction.response.send_modal(modal)
        else:
            await interaction.response.send_message("You have already submitted your feedback.", ephemeral=True)

    @discord.ui.button(label="Satisfactory", style=ButtonStyle.primary)
    async def satisfactory_callback(
            self, button: discord.ui.Button, interaction: discord.Interaction
    ):
        if interaction.user.id not in self.users_interacted_with_view:
            # Show modal for open feedback
            modal = FeedbackModal(self, "Satisfactory")
            await interaction.response.send_modal(modal)
        else:
            await interaction.response.send_message("You have already submitted your feedback.", ephemeral=True)

    @discord.ui.button(label="Poor", style=ButtonStyle.primary)
    async def poor_callback(
            self, button: discord.ui.Button, interaction: discord.Interaction
    ):
        if interaction.user.id not in self.users_interacted_with_view:
            # Show modal for open feedback
            modal = FeedbackModal(self, "Poor")
            await interaction.response.send_modal(modal)
        else:
            await interaction.response.send_message("You have already submitted your feedback.", ephemeral=True)

    async def on_timeout(self) -> None:
        self.disable_all_items()

        if self.countdown_task and not self.countdown_task.done():
            self.countdown_task.cancel()

        if self.message:
            try:
                embed = self.message.embeds[0]
                timer_field_index = -1
                for i, field in enumerate(embed.fields):
                    if "Time Remaining" in field.name:
                        timer_field_index = i
                        break

                if timer_field_index != -1:
                    embed.set_field_at(
                        timer_field_index,
                        name="⏱️ Time Remaining",
                        value="Feedback Period Ended",
                        inline=True
                    )
                embed.color = discord.Color.dark_gray()
                embed.set_footer(text="Feedback period has ended. Thank you for your responses!")
                await self.message.edit(embed=embed, view=self)
            except:
                pass

        # Count total entries across all feedback types
        total_entries = len(self.users_good_review) + len(self.users_satisfactory_review) + len(self.users_poor_review)

        # Log feedback completion information
        logger.info(f"DEBUG: Completed tutor session feedback (group {self.group_id}) with {total_entries} responses. Data saved to {self.path}")

        return await super().on_timeout()

    def update_percentage(self, embed: discord.Embed) -> discord.Embed:
        """
        Updates the percentage for each option in the embed according to the number of entires in the lists and returns the updated Embed variable.

        Args:
            embed :class:`discord.Embed`: Original Embed.

        Returns:
            :class:`discord.Embed`: Updated Embed.
        """
        total_interacted = len(self.users_interacted_with_view)
        if total_interacted == 0:
            return embed

        for i, field in enumerate(embed.fields):
            if "Participants" in field.name:
                embed.set_field_at(i, name="Participants: " + str(total_interacted), value=field.value, inline=field.inline)

            if "Time Remaining" in field.name:
                continue

            match field.name:
                case "Good":
                    val = "`" + str(format(len(self.users_good_review) / (total_interacted / 100), ".2f")) + " %`"
                    embed.set_field_at(i, name=field.name, value=val, inline=field.inline)
                case "Satisfactory":
                    val = "`" + str(format(len(self.users_satisfactory_review) / (total_interacted / 100), ".2f")) + " %`"
                    embed.set_field_at(i, name=field.name, value=val, inline=field.inline)
                case "Poor":
                    val = "`" + str(format(len(self.users_poor_review) / (total_interacted / 100), ".2f")) + " %`"
                    embed.set_field_at(i, name=field.name, value=val, inline=field.inline)
        return embed

    def start_countdown(self):
        """Start the countdown timer to update the embed."""
        self.start_time = datetime.now()

        def create_countdown_task():
            async def countdown_loop():
                last_updated_second = -1
                time_ended_shown = False
                while True:
                    try:
                        now = datetime.now()
                        elapsed = (now - self.start_time).total_seconds()
                        remaining = max(0, self.duration - elapsed)

                        if remaining <= 0:
                            if not time_ended_shown and self.message:
                                await asyncio.sleep(1.0)
                                try:
                                    embed = self.message.embeds[0]
                                    embed = self.update_percentage(embed)

                                    timer_field_index = -1
                                    for i, field in enumerate(embed.fields):
                                        if "Time Remaining" in field.name:
                                            timer_field_index = i
                                            break

                                    if timer_field_index != -1:
                                        embed.set_field_at(
                                            timer_field_index,
                                            name="⏱️ Time Remaining",
                                            value="Time Ended",
                                            inline=True
                                        )
                                    embed.color = discord.Color.dark_gray()
                                    await self.message.edit(embed=embed)
                                    time_ended_shown = True
                                except:
                                    pass
                            break

                        current_second = int(remaining)
                        should_update = current_second != last_updated_second

                        if should_update and self.message:
                            last_updated_second = current_second

                            embed = self.message.embeds[0]
                            embed = self.update_percentage(embed)

                            minutes = int(remaining // 60)
                            seconds = int(remaining % 60)
                            time_str = f"{minutes}m {seconds}s" if minutes > 0 else f"{seconds}s"

                            timer_field_index = -1
                            for i, field in enumerate(embed.fields):
                                if "Time Remaining" in field.name:
                                    timer_field_index = i
                                    break

                            if timer_field_index != -1:
                                embed.set_field_at(
                                    timer_field_index,
                                    name="⏱️ Time Remaining",
                                    value=time_str,
                                    inline=True
                                )

                                if remaining < 60:
                                    embed.color = discord.Color.red()
                                elif remaining < 300:
                                    embed.color = discord.Color.orange()

                                try:
                                    await self.message.edit(embed=embed)
                                except:
                                    break

                        await asyncio.sleep(0.5)

                    except Exception as e:
                        logger.error(f"Error in countdown loop for TutorSessionView: {e}")
                        break

            return countdown_loop()

        bot = bc.get_live_bot()
        self.countdown_task = bot.loop.create_task(create_countdown_task())


class AnnouncementView(discord.ui.View):
    """Represents a custom UI view.
    Initial view that is attached to the main message.\n
    Allows students to participate in the survey.

    Parameters
    ----------
    *items: :class:`Item`
        The initial items attached to this view.
    timeout: Optional[:class:`float`]
        Timeout in seconds from last interaction with the UI before no longer accepting input.
        If ``None`` then there is no timeout.
    topic: :class:`str`
        The main topic of the current survey.
    guild: :class:`discord.Guild`
        The guild associated with the view.
    views_queue: :class:`list`
        The views queue that will be used to continue the current interaction and extend the survey.


    Attributes
    ----------
    timeout: Optional[:class:`float`]
        Timeout from last interaction with the UI before no longer accepting input.
        If ``None`` then there is no timeout.
    children: List[:class:`Item`]
        The list of children attached to this view.
    disable_on_timeout: :class:`bool`
        Whether to disable the view when the timeout is reached. Defaults to ``False``.
    message: Optional[:class:`.Message`]
        The message that this view is attached to.
        If ``None`` then the view has not been sent with a message.
    parent: Optional[:class:`.Interaction`]
        The parent interaction which this view was sent from.
        If ``None`` then the view was not sent using :meth:`InteractionResponse.send_message`.
    users_interacted_with_view: :class:`list`
        The list of the user ids that interracted with the view.
    topic: :class:`str`
        The main topic of the current survey.
    guild: :class:`discord.Guild`
        The guild associated with the view.
    views_queue: :class:`list`
        The views queue that will be used to continue the current interaction and extend the survey.
    """

    def __init__(self, topic: str, guild: discord.Guild, views_queue: list, duration: float | None = 80):
        # Use the provided duration so the announcement stays interactive for at least as long as the survey views
        super().__init__(timeout=duration, disable_on_timeout=True)
        self.users_interacted_with_view = []
        self.topic = topic
        self.guild = guild
        # Store a *template* of the views queue so it can be duplicated for every participant
        # We keep the original list intact to make sure that subsequent participants
        # always receive the full survey (Q1, Q2, Q3, ...).
        self.views_queue_template = views_queue

    @discord.ui.button(label="Participate", style=ButtonStyle.green)
    async def participate_callback(
            self, button: discord.ui.Button, interaction: discord.Interaction
    ):
        await interaction.response.edit_message(view=self)
        if interaction.user.id not in self.users_interacted_with_view:
            self.users_interacted_with_view.append(interaction.user.id)
            # Create a *fresh* copy of the template views queue for this participant.
            participant_queue: list[discord.ui.View] = []

            # Helper to clone a view based on its type
            def _clone_view(original_view):
                common_kwargs = {
                    "duration": original_view.timeout,
                    "guild": self.guild,
                    "topic": original_view.topic,
                    "display_message": original_view.display_message,
                    "views_queue": None,  # Placeholder; will set after queue is built
                    "disable_after_interaction": getattr(original_view, "disable_after_interaction", False),
                }

                if hasattr(original_view, "type") and original_view.type == "Difficulty":
                    return DifficultyView(**common_kwargs)
                else:  # Default to ScoreView
                    return ScoreView(**common_kwargs)

            # Build new queue with cloned views
            for v in self.views_queue_template:
                participant_queue.append(_clone_view(v))

            # Now that the list is complete, assign the shared queue reference to each view
            for v in participant_queue:
                v.views_queue = participant_queue

            # Pop the first view (Q1) for the participant
            first_view = participant_queue.pop(0)

            await interaction.user.send(
                content=f"```{first_view.display_message}```", view=first_view
            )
        else:
            await interaction.user.send("You've already taken the survey.")


class DifficultyView(discord.ui.View):
    """Represents a custom UI view.
    A view that is used to gather the student's opinion on how challenging the assigned topic was.

    Parameters
    ----------
    *items: :class:`Item`
        The initial items attached to this view.
    timeout: Optional[:class:`float`]
        Timeout in seconds from last interaction with the UI before no longer accepting input.
        If ``None`` then there is no timeout.
    guild: :class:`discord.Guild`
        The guild associated with the view.
    topic: :class:`str`
        The main topic of the current survey.
    message: :class:`str`
        The message that will be displayed with the view.
    views_queue: :class:`list`
        The views queue that will be used to continue the current interaction and extend the survey.
    disable_after_interaction: :class:`bool`
        If the view element(s) must be disabled after the interaction is complete.

    Attributes
    ----------
    timeout: Optional[:class:`float`]
        Timeout from last interaction with the UI before no longer accepting input.
        If ``None`` then there is no timeout.
    children: List[:class:`Item`]
        The list of children attached to this view.
    disable_on_timeout: :class:`bool`
        Whether to disable the view when the timeout is reached. Defaults to ``False``.
    message: Optional[:class:`.Message`]
        The message that this view is attached to.
        If ``None`` then the view has not been sent with a message.
    parent: Optional[:class:`.Interaction`]
        The parent interaction which this view was sent from.
        If ``None`` then the view was not sent using :meth:`InteractionResponse.send_message`.
    users_interacted_with_view: :class:`list`
        The list of the user ids that interracted with the view.
    type: :class:`str`
        The view type.
    guild: :class:`discord.Guild`
        The guild associated with the view.
    topic: :class:`str`
        The main topic of the current survey.
    display_message: :class:`str`
        The message that will be displayed with the view.
    views_queue: :class:`list`
        The views queue that will be used to continue the current interaction and extend the survey.
    disable_after_interaction: :class:`bool`
        If the view element(s) must be disabled after the interaction is complete.
    survey_entry: :class:`shared.entry.SurveyEntry`
        The survey entry that contains the student's name and answers.
    all_survey_entries: :class:`list`
        The list to store all survey entries.
    """

    def __init__(
            self,
            duration: float | None = 30,
            guild: discord.Guild | None = None,
            topic: str | None = "Empty",
            display_message: str | None = "Empty",
            views_queue: list | None = None,
            disable_after_interaction: bool | None = False
    ):
        super().__init__(timeout=duration, disable_on_timeout=True)
        self.users_interacted_with_view = []
        self.type = "Difficulty"
        self.guild = guild
        self.topic = topic
        self.display_message = display_message
        self.views_queue = views_queue
        self.disable_after_interaction = disable_after_interaction
        self.survey_entry = SurveyEntry()
        self.all_survey_entries = []  # List to store all survey entries
        self.children.append(
            DynamicButton(
                label="Very Easy", style=ButtonStyle.green, view_reference=self
            )
        )
        self.children.append(
            DynamicButton(label="Easy", style=ButtonStyle.primary, view_reference=self)
        )
        self.children.append(
            DynamicButton(
                label="Medium", style=ButtonStyle.primary, view_reference=self
            )
        )
        self.children.append(
            DynamicButton(label="Hard", style=ButtonStyle.primary, view_reference=self)
        )
        self.children.append(
            DynamicButton(label="Very Hard", style=ButtonStyle.red, view_reference=self)
        )

    async def on_timeout(self) -> None:
        """Save all survey entries when the view times out."""
        self.disable_all_items()
        
        # Determine survey type (CS for complex, SS for simple)
        survey_type = "CS" if hasattr(self, 'from_complex') and self.from_complex else "SS"
        
        # Generate filename with timestamp
        current_time = datetime.now().strftime("%Y-%m-%d_%H-%M")
        # Resolve absolute path for exercise feedback
        project_root = Path(__file__).resolve().parents[2]
        survey_dir = project_root / 'data' / 'exercise_feedback'
        survey_dir.mkdir(exist_ok=True, parents=True)
        path = str(survey_dir / f"{survey_type}_{self.topic}_{current_time}.csv")
        
        # Save all collected entries
        entry_count = len(self.all_survey_entries)
        for entry in self.all_survey_entries:
            save_survey_entry_to_csv(path=path, entry=entry)
        
        # Log survey completion information
        logger.info(f"DEBUG: Saved {entry_count} responses for {survey_type} survey on topic '{self.topic}' to {path}")
        
        return await super().on_timeout()


class ScoreView(discord.ui.View):
    """Represents a custom UI view.
    A view that is used to collect students' opinions about the expected grade they will receive for a specified topic.

    Parameters
    ----------
    *items: :class:`Item`
        The initial items attached to this view.
    timeout: Optional[:class:`float`]
        Timeout in seconds from last interaction with the UI before no longer accepting input.
        If ``None`` then there is no timeout.
    guild: :class:`discord.Guild`
        The guild associated with the view.
    topic: :class:`str`
        The main topic of the current survey.
    message: :class:`str`
        The message that will be displayed with the view.
    views_queue: :class:`list`
        The views queue that will be used to continue the current interaction and extend the survey.
    disable_after_interaction: :class:`bool`
        If the view element(s) must be disabled after the interaction is complete.

    Attributes
    ----------
    timeout: Optional[:class:`float`]
        Timeout from last interaction with the UI before no longer accepting input.
        If ``None`` then there is no timeout.
    children: List[:class:`Item`]
        The list of children attached to this view.
    disable_on_timeout: :class:`bool`
        Whether to disable the view when the timeout is reached. Defaults to ``False``.
    message: Optional[:class:`.Message`]
        The message that this view is attached to.
        If ``None`` then the view has not been sent with a message.
    parent: Optional[:class:`.Interaction`]
        The parent interaction which this view was sent from.
        If ``None`` then the view was not sent using :meth:`InteractionResponse.send_message`.
    users_interacted_with_view: :class:`list`
        The list of the user ids that interracted with the view.
    type: :class:`str`
        The view type.
    guild: :class:`discord.Guild`
        The guild associated with the view.
    topic: :class:`str`
        The main topic of the current survey.
    display_message: :class:`str`
        The message that will be displayed with the view.
    views_queue: :class:`list`
        The views queue that will be used to continue the current interaction and extend the survey.
    disable_after_interaction: :class:`bool`
        If the view element(s) must be disabled after the interaction is complete.
    survey_entry: :class:`shared.entry.SurveyEntry`
        The survey entry that contains the student's name and answers.
    all_survey_entries: :class:`list`
        The list to store all survey entries.
    """

    def __init__(
            self,
            duration: float | None = 30,
            guild: discord.Guild | None = None,
            topic: str | None = "Empty",
            display_message: str | None = "Empty",
            views_queue: list | None = None,
            disable_after_interaction: bool | None = False,
    ):
        super().__init__(timeout=duration, disable_on_timeout=True)
        self.users_interacted_with_view = []
        self.type = "Score"
        self.guild = guild
        self.topic = topic
        self.display_message = display_message
        self.views_queue = views_queue
        self.disable_after_interaction = disable_after_interaction
        self.survey_entry = SurveyEntry()
        self.all_survey_entries = []  # List to store all survey entries
        self.children.append(
            DynamicButton(label="20%", style=ButtonStyle.red, view_reference=self)
        )
        self.children.append(
            DynamicButton(label="40%", style=ButtonStyle.primary, view_reference=self)
        )
        self.children.append(
            DynamicButton(label="60%", style=ButtonStyle.primary, view_reference=self)
        )
        self.children.append(
            DynamicButton(label="80%", style=ButtonStyle.primary, view_reference=self)
        )
        self.children.append(
            DynamicButton(label="100%", style=ButtonStyle.green, view_reference=self)
        )

    async def on_timeout(self) -> None:
        """Save all survey entries when the view times out."""
        self.disable_all_items()
        
        # Only save entries if there are any and this is the final view
        if self.all_survey_entries and (self.views_queue is None or not self.views_queue):
            # Determine survey type (CS for complex, SS for simple)
            survey_type = "CS" if hasattr(self, 'from_complex') and self.from_complex else "SS"
            
            # Generate filename with timestamp
            current_time = datetime.now().strftime("%Y-%m-%d_%H-%M")
            # Resolve absolute path for exercise feedback
            project_root = Path(__file__).resolve().parents[2]
            survey_dir = project_root / 'data' / 'exercise_feedback'
            survey_dir.mkdir(exist_ok=True, parents=True)
            path = str(survey_dir / f"{survey_type}_{self.topic}_{current_time}.csv")
            
            # Save all collected entries
            entry_count = len(self.all_survey_entries)
            for entry in self.all_survey_entries:
                save_survey_entry_to_csv(path=path, entry=entry)
            
            # Log survey completion information
            logger.info(f"DEBUG: Saved {entry_count} responses for {survey_type} survey on topic '{self.topic}' to {path}")

        return await super().on_timeout()


class QAAllQuestionsModal(discord.ui.Modal):
    """Modal for collecting all Q&A responses at once (up to 5 questions per modal)."""

    def __init__(self, qa_pairs: list, parent_view, user_id: int):
        super().__init__(title=f"Quiz - {len(qa_pairs)} Question(s)")
        self.qa_pairs = qa_pairs
        self.parent_view = parent_view
        self.user_id = user_id

        # Add text inputs for each question (Discord limit: 5 inputs per modal)
        for i, qa_pair in enumerate(qa_pairs[:5]):
            question = qa_pair['question']

            # Discord label limit is 45 characters
            # Use label for short prefix and placeholder for full question
            text_input = discord.ui.InputText(
                label=f"Question {i+1}",  # Simple label
                placeholder=question if len(question) <= 100 else question[:97] + "...",  # Full question in placeholder (100 char limit)
                style=discord.InputTextStyle.long,
                required=True,
                max_length=1000
            )
            self.add_item(text_input)

    async def callback(self, interaction: discord.Interaction):
        """Handle the modal submission."""
        if self.parent_view.start_time:
            elapsed = (datetime.now() - self.parent_view.start_time).total_seconds()
            if elapsed >= self.parent_view.duration:
                if self.user_id in self.parent_view.users_in_progress:
                    del self.parent_view.users_in_progress[self.user_id]

                await interaction.response.send_message(
                    "⏱️ Time has expired. Your submission cannot be accepted.",
                    ephemeral=True
                )
                return

        student_name = f"{interaction.guild.get_member(self.user_id).display_name} ({interaction.user.name})"

        answers = []
        for i, item in enumerate(self.children):
            if i < len(self.qa_pairs):
                answers.append({
                    'question': self.qa_pairs[i]['question'],
                    'user_answer': item.value,
                    'correct_answer': self.qa_pairs[i]['answer']
                })

        self.parent_view.responses.append({
            'user_id': self.user_id,
            'student': student_name,
            'timestamp': datetime.now().isoformat(),
            'answers': answers
        })

        if self.user_id in self.parent_view.users_in_progress:
            del self.parent_view.users_in_progress[self.user_id]

        await interaction.response.send_message(
            f"✅ Quiz completed! You answered {len(answers)} question(s). Thank you for participating!",
            ephemeral=True
        )

        logger.info(f"User {student_name} completed the quiz with {len(answers)} questions")


class QAAnswerButtonView(discord.ui.View):
    """View with a button to open the answer modal."""
    def __init__(self, qa_pairs: list, parent_view, user_id: int):
        super().__init__(timeout=300)
        self.qa_pairs = qa_pairs
        self.parent_view = parent_view
        self.user_id = user_id

    @discord.ui.button(label="Open Answer Form", style=ButtonStyle.primary, emoji="✍️")
    async def open_form(self, button: discord.ui.Button, interaction: discord.Interaction):
        """Open the modal to collect answers."""
        if interaction.user.id != self.user_id:
            await interaction.response.send_message(
                "This quiz is for another user!",
                ephemeral=True
            )
            return

        if any(r['user_id'] == self.user_id for r in self.parent_view.responses):
            await interaction.response.send_message(
                "You have already completed this quiz!",
                ephemeral=True
            )
            return

        if self.user_id not in self.parent_view.users_in_progress:
            await interaction.response.send_message(
                "You are no longer taking this quiz or the time has expired.",
                ephemeral=True
            )
            return

        modal = QAAllQuestionsModal(self.qa_pairs, self.parent_view, self.user_id)
        await interaction.response.send_modal(modal)


class QAView(discord.ui.View):
    """Main view for Q&A quiz with Start button."""
    def __init__(self, qa_pairs: list, duration: float, ai_feedback_enabled: bool = False):
        super().__init__(timeout=duration, disable_on_timeout=True)
        self.qa_pairs = qa_pairs
        self.responses = []
        self.users_in_progress = {}
        self.ai_feedback_enabled = ai_feedback_enabled
        self.duration = duration
        self.start_time = None
        self.message = None
        self.countdown_task = None

        current_time = datetime.now().strftime("%Y-%m-%d_%H-%M")
        project_root = Path(__file__).resolve().parents[2]
        qa_responses_dir = project_root / 'data' / 'qa_responses'
        qa_responses_dir.mkdir(exist_ok=True, parents=True)
        self.path = str(qa_responses_dir / f"qa_responses_{current_time}.json")

    @discord.ui.button(label="Start Quiz", style=ButtonStyle.primary, emoji="📝")
    async def start_quiz(self, button: discord.ui.Button, interaction: discord.Interaction):
        """Start the quiz for the user."""
        user_id = interaction.user.id

        if any(r['user_id'] == user_id for r in self.responses):
            await interaction.response.send_message(
                "You have already completed this quiz!",
                ephemeral=True
            )
            return

        if user_id in self.users_in_progress:
            await interaction.response.send_message(
                "You are already taking this quiz! Please complete your current quiz.",
                ephemeral=True
            )
            return

        if len(self.qa_pairs) > 5:
            await interaction.response.send_message(
                f"⚠️ This quiz has {len(self.qa_pairs)} questions, but Discord modals support a maximum of 5 questions at once. Please contact the instructor to split this quiz.",
                ephemeral=True
            )
            return

        self.users_in_progress[user_id] = True

        embed = discord.Embed(
            title="📝 Quiz Questions",
            description="Please read all questions carefully before answering:",
            color=discord.Color.blue()
        )

        for i, qa_pair in enumerate(self.qa_pairs):
            embed.add_field(
                name=f"Question {i+1}",
                value=qa_pair['question'],
                inline=False
            )

        embed.set_footer(text="Click the button below to open the answer form.")

        answer_view = QAAnswerButtonView(self.qa_pairs, self, user_id)

        await interaction.response.send_message(embed=embed, view=answer_view, ephemeral=True)

    def start_countdown(self):
        """Start the countdown timer to update the embed."""
        self.start_time = datetime.now()

        def create_countdown_task():
            async def countdown_loop():
                last_updated_second = -1
                time_ended_shown = False
                while True:
                    try:
                        now = datetime.now()
                        elapsed = (now - self.start_time).total_seconds()
                        remaining = max(0, self.duration - elapsed)

                        if remaining <= 0:
                            # Show "Time Ended" once after 1 second
                            if not time_ended_shown and self.message:
                                await asyncio.sleep(1.0)
                                try:
                                    embed = self.message.embeds[0]
                                    embed.set_field_at(
                                        0,
                                        name="⏱️ Time Remaining",
                                        value="Time Ended",
                                        inline=True
                                    )
                                    embed.color = discord.Color.dark_gray()
                                    await self.message.edit(embed=embed)
                                    time_ended_shown = True
                                except:
                                    pass
                            break

                        current_second = int(remaining)
                        should_update = current_second != last_updated_second

                        if should_update and self.message:
                            last_updated_second = current_second
                            embed = self.message.embeds[0]

                            minutes = int(remaining // 60)
                            seconds = int(remaining % 60)
                            time_str = f"{minutes}m {seconds}s" if minutes > 0 else f"{seconds}s"

                            embed.set_field_at(
                                0,
                                name="⏱️ Time Remaining",
                                value=time_str,
                                inline=True
                            )

                            if remaining < 60:
                                embed.color = discord.Color.red()
                            elif remaining < 300:
                                embed.color = discord.Color.orange()

                            try:
                                await self.message.edit(embed=embed)
                            except:
                                break

                        await asyncio.sleep(0.5)

                    except Exception as e:
                        logger.error(f"Error in countdown loop: {e}")
                        break

            return countdown_loop()

        bot = bc.get_live_bot()
        self.countdown_task = bot.loop.create_task(create_countdown_task())

    async def on_timeout(self):
        """Save all responses when the view times out."""
        self.disable_all_items()

        if self.countdown_task and not self.countdown_task.done():
            self.countdown_task.cancel()

        if self.message:
            try:
                embed = self.message.embeds[0]
                embed.set_field_at(
                    0,
                    name="⏱️ Time Remaining",
                    value="Quiz Ended",
                    inline=True
                )
                embed.color = discord.Color.dark_gray()
                embed.set_footer(text="This quiz has ended. Thank you to all participants!")
                await self.message.edit(embed=embed, view=self)
            except:
                pass

        if self.responses:
            import json
            with open(self.path, 'w', encoding='utf-8') as f:
                json.dump({
                    'timestamp': datetime.now().isoformat(),
                    'ai_feedback_enabled': self.ai_feedback_enabled,
                    'total_responses': len(self.responses),
                    'total_questions': len(self.qa_pairs),
                    'responses': self.responses
                }, f, indent=2, ensure_ascii=False)

            logger.info(f"Saved {len(self.responses)} Q&A responses to {self.path}")

        return await super().on_timeout()
