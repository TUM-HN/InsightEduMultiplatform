package org.example.project.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FindInPage
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Description
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import org.example.project.TUMColors

private data class CommandUi(
    val title: String,
    val subtitle: String,
    val type: CommandType,
    val onClick: () -> Unit,
)

private enum class CommandType {
    Ping,
    SendMessage,
    AssignRole,
    ClearMessages,
    Attendance,
    TutorFeedback,
    SimpleSurvey,
    ComplexSurvey,
    ContentQuestions,
}

@Composable
fun CommandsList(
    onPing: () -> Unit = {},
    onSendMessage: () -> Unit = {},
    onGiveRole: () -> Unit = {},
    onClearMessages: () -> Unit = {},
    onAttendance: () -> Unit = {},
    onTutorFeedback: () -> Unit = {},
    onSimpleSurvey: () -> Unit = {},
    onComplexSurvey: () -> Unit = {},
    onContentQuestions: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val simpleCommands = listOf(
        CommandUi(
            title = "Ping",
            subtitle = "Check bot response time",
            type = CommandType.Ping,
            onClick = onPing,
        )
    )
    val memberCommands = listOf(
        CommandUi(
            title = "Send message",
            subtitle = "Send a personalized message",
            type = CommandType.SendMessage,
            onClick = onSendMessage,
        ),
        CommandUi(
            title = "Assign Role",
            subtitle = "Assign a role to a member",
            type = CommandType.AssignRole,
            onClick = onGiveRole,
        )
    )
    val channelCommands = listOf(
        CommandUi(
            title = "Clear Messages",
            subtitle = "Delete messages from a channel",
            type = CommandType.ClearMessages,
            onClick = onClearMessages,
        )
    )
    val groupCommands = listOf(
        CommandUi(
            title = "Attendance",
            subtitle = "Track attendance in a chanel",
            type = CommandType.Attendance,
            onClick = onAttendance,
        ),
        CommandUi(
            title = "Tutor Feedback",
            subtitle = "Get feedback on a session",
            type = CommandType.TutorFeedback,
            onClick = onTutorFeedback,
        )
    )
    val surveyCommands = listOf(
        CommandUi(
            title = "Simple Survey",
            subtitle = "Track attendance in a channel",
            type = CommandType.SimpleSurvey,
            onClick = onSimpleSurvey,
        ),
        CommandUi(
            title = "Complex Survey",
            subtitle = "Get feedback on a session",
            type = CommandType.ComplexSurvey,
            onClick = onComplexSurvey,
        )
    )
    val contentCommands = listOf(
        CommandUi(
            title = "Content Questions",
            subtitle = "Generate questions from PDF",
            type = CommandType.ContentQuestions,
            onClick = onContentQuestions,
        )
    )

    Surface(
        color = TUMColors.tumGray9,
        shape = RoundedCornerShape(6.dp),
    ) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionHeader(text = "Simple Commands")
            simpleCommands.forEach { cmd ->
                CommandButton(command = cmd, modifier = Modifier.fillMaxWidth())
            }

            SectionHeader(text = "Member Commands")
            memberCommands.forEach { cmd ->
                CommandButton(command = cmd, modifier = Modifier.fillMaxWidth())
            }

            SectionHeader(text = "Channel Commands")
            channelCommands.forEach { cmd ->
                CommandButton(command = cmd, modifier = Modifier.fillMaxWidth())
            }

            SectionHeader(text = "Group Commands")
            groupCommands.forEach { cmd ->
                CommandButton(command = cmd, modifier = Modifier.fillMaxWidth())
            }

            SectionHeader(text = "Survey Commands")
            surveyCommands.forEach { cmd ->
                CommandButton(command = cmd, modifier = Modifier.fillMaxWidth())
            }

            SectionHeader(text = "Content Commands")
            contentCommands.forEach { cmd ->
                CommandButton(command = cmd, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun CommandButton(
    command: CommandUi,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = command.onClick,
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        //border = ButtonDefaults.outlinedButtonBorder(true),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White)//TUMColors.tumGray9)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            val icon = when (command.type) {
                CommandType.Ping -> Icons.Outlined.Speed
                CommandType.SendMessage -> Icons.Outlined.ChatBubbleOutline
                CommandType.AssignRole -> Icons.Outlined.PersonAdd
                CommandType.ClearMessages -> Icons.Outlined.DeleteOutline
                CommandType.Attendance -> Icons.AutoMirrored.Outlined.List
                CommandType.TutorFeedback -> Icons.Outlined.StarBorder
                CommandType.SimpleSurvey -> Icons.AutoMirrored.Outlined.Assignment
                CommandType.ComplexSurvey -> Icons.Outlined.FindInPage
                CommandType.ContentQuestions -> Icons.Outlined.Description
            }
            Icon(imageVector = icon, contentDescription = null, tint = TUMColors.tumBlue)
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    text = command.title,
                    color = TUMColors.tumBlue,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = command.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = TUMColors.tumBlue
            )
        }
    }
}

@Preview
@Composable
private fun CommandsListPreview() {
    MaterialTheme {
        Surface {
            CommandsList()
        }
    }
}
