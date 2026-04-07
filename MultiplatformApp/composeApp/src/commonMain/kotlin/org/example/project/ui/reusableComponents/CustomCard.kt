package org.example.project.ui.reusableComponents

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.example.project.TUMColors
import org.example.project.model.UiMessage

@Composable
fun CustomCard(
    title: String = "",
    statusMessage: UiMessage? = null,
    actionMessage: UiMessage? = null,
    onClearActionMessage: (() -> Unit)? = null,
    showRedStar: Boolean = true,
    showTitle: Boolean = true,
    titleButton: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    LaunchedEffect(actionMessage) {
        if (actionMessage != null && actionMessage.autoDismiss) {
            kotlinx.coroutines.delay(3000)
            onClearActionMessage?.invoke()
        }
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth()
    ) {
        if (showTitle) {
            ScreenTitle(title, showRedStar, titleButton)
        }

        if (actionMessage != null) {
            MessageCard(message = actionMessage, onDismiss = onClearActionMessage)
            Spacer(modifier = Modifier.height(8.dp))
        } else if (statusMessage != null) {
            MessageCard(message = statusMessage, onDismiss = null)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun ScreenTitle(
    text: String,
    showRedStar: Boolean = true,
    titleButton: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.padding(bottom = 8.dp, start = 16.dp, end = 16.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = TUMColors.tumGray4,
            fontWeight = FontWeight.SemiBold
        )
        if (showRedStar) {
            Text("*", color = Color.Red, modifier = Modifier.padding(start = 2.dp))
        }
        if (titleButton != null) {
            Spacer(modifier = Modifier.weight(1f))
            titleButton()
        }
    }
}

@Preview
@Composable
private fun MessageCardAllTypesPreview() {
    MaterialTheme {
        Surface(color = TUMColors.tumGray9) {
            CustomCard(
                title = "Test Preview",
                statusMessage = UiMessage.Error("This is a error message"),
            ) {
                CustomDropdownMenu(
                    onItemSelected = {},
                    items = listOf("Option 1", "Option 2", "Option 3"),
                    selectedItem = "Option 1",
                    menuTitle = "Menu Title",
                    dropdownTitle = "Dropdown Title",
                    itemLabel = { it ?: "None" },
                )
                CustomHorizontalDivider()
                CustomTextField(
                    message = "Assign a role to a Discord member",
                    placeholder = "",
                    onValueChange = {},
                    enabled = false,
                )
            }
        }
    }
}
