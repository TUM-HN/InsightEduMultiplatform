package org.example.project.ui.reusableComponents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.example.project.TUMColors
import org.example.project.model.UiMessage

@Composable
fun MessageCard(
    message: UiMessage,
    onDismiss: (() -> Unit)? = null,
) {
    val messageColor = when (message) {
        is UiMessage.Success -> TUMColors.tumGreen
        is UiMessage.Info -> Color.Gray
        is UiMessage.Warning -> TUMColors.tumRed
        is UiMessage.Error -> TUMColors.tumRed
    }

    val backgroundColor = Color.White//TUMColors.tumGray9

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 32.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message.text,
                color = messageColor,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            if (onDismiss != null && !message.autoDismiss) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Dismiss", color = messageColor, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Preview
@Composable
private fun MessageCardAllTypesPreview() {
    MaterialTheme {
        Surface(color = TUMColors.tumGray9) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Message Card Types",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                MessageCard(
                    message = UiMessage.Success("Bot started successfully!"),
                    onDismiss = null
                )

                MessageCard(
                    message = UiMessage.Info("Server status checks: 42"),
                    onDismiss = {}
                )

                MessageCard(
                    message = UiMessage.Warning("Please select start and end dates"),
                    onDismiss = {}
                )

                MessageCard(
                    message = UiMessage.Error("Server is offline.", isNetworkError = true),
                    onDismiss = {}
                )
            }
        }
    }
}
