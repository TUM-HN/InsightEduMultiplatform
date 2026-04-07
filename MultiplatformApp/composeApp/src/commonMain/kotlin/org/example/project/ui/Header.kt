package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.example.project.TUMColors
import org.example.project.model.UiMessage
import org.example.project.model.viewModels.HeaderUiState
import org.example.project.ui.reusableComponents.CustomAlertDialog
import org.example.project.ui.reusableComponents.MessageCard

@Composable
fun Header(
    state: HeaderUiState,
    onStartStopClicked: () -> Unit,
    onSettings: () -> Unit,
    onClearData: () -> Unit = {},
    onDismissClearData: () -> Unit = {},
    onConfirmClearData: () -> Unit = {},
    onClearActionMessage: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(state.actionMessage) {
        val actionMessage = state.actionMessage
        if (actionMessage != null && actionMessage.autoDismiss) {
            kotlinx.coroutines.delay(3000)
            onClearActionMessage()
        }
    }

    if (state.showClearDataDialog) {
        CustomAlertDialog(
            title = "Clear All Data",
            text = "Are you sure you want to clear all data files? This action cannot be undone.",
            onDismiss = onDismissClearData,
            onConfirm = onConfirmClearData
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = TUMColors.tumGray9)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "DiscordBot Manager",
                style = MaterialTheme.typography.titleLarge,
                color = TUMColors.tumBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.padding(end = 16.dp, bottom = 16.dp).weight(5f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Dot(color = if (state.isBotRunning) TUMColors.tumGreen else TUMColors.tumGray4)
                        Text(
                            text = if (state.isBotRunning) "Active" else "InActive",
                            color = if (state.isBotRunning) TUMColors.tumGreen else TUMColors.tumGray4
                        )
                        Dot(color = if (state.isServerOnline) TUMColors.tumGreen else TUMColors.tumOrange)
                        Text(
                            text = if (state.isServerOnline) "Server Online" else "Server Offline",
                            color = if (state.isServerOnline) TUMColors.tumGreen else TUMColors.tumOrange
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Server URL: ${state.serverUrl}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TUMColors.tumGray4
                    )
                    Text(
                        "Status checks: ${state.refreshCounter}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TUMColors.tumGray4
                    )
                }

                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TUMColors.tumBlue,
                    modifier = Modifier
                        .size(30.dp)
                        .align(Alignment.Top)
                        .clickable(
                            onClick = { onSettings() }
                        )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = TUMColors.tumGray8),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Server Stats",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Stat("Online", state.onlineMembers, TUMColors.tumGreen)
                            Stat("Offline", state.offlineMembers, TUMColors.tumGray4)
                            Stat("Total", state.totalMembers, TUMColors.tumBlue)
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(19.dp)
                ) {
                    Button(
                        onClick = onClearData,
                        enabled = state.isServerOnline && !state.isLoadingBotAction,
                        colors = ButtonDefaults.buttonColors(containerColor = TUMColors.tumOrange),
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete Data",
                            tint = Color.White,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Delete Data")
                    }

                    val start = !state.isBotRunning
                    Button(
                        onClick = onStartStopClicked,
                        enabled = state.isServerOnline && !state.isLoadingBotAction,
                        colors = ButtonDefaults.buttonColors(containerColor = if (start) TUMColors.tumGreen else TUMColors.tumOrange),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                    ) {
                        Icon(
                            imageVector = if (start) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (start) "Start" else "Stop",
                            tint = Color.White,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (start) "Start Bot" else "Stop Bot")
                    }
                }
            }

            val message = state.actionMessage ?: state.statusMessage
            if (message != null) {
                Spacer(Modifier.height(16.dp))
                MessageCard(message = message)
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$value", color = color, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun Dot(color: Color) {
    Box(
        modifier = Modifier.size(10.dp).clip(CircleShape).background(color)
    )
}

@Preview
@Composable
private fun HeaderPreview() {
    Box(modifier = Modifier.padding(top = 0.dp)) {
        Header(
            state = HeaderUiState(statusMessage = UiMessage.Error("Server is offline")),
            onStartStopClicked = {},
            onSettings ={},
            onClearData = {},
            onDismissClearData = {},
            onConfirmClearData = {},
            onClearActionMessage = {}
        )
    }
}
