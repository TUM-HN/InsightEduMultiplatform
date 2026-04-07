package org.example.project.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.example.project.TUMColors
import org.example.project.model.PingResponse
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun PingResultDialog(
    response: PingResponse?,
    onOk: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.size(height = 200.dp, width = 300.dp),
        onDismissRequest = { },
        confirmButton = {
            Box(modifier = Modifier.fillMaxWidth().height(35.dp)) {
                TextButton(
                    modifier = Modifier.align(Alignment.Center),
                    onClick = onOk
                ) {
                    Text(
                        text = "OK",
                        color = TUMColors.tumBlue
                    )
                }
            }
        },
        title = {
            Text(
                text = "Ping Result",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            val message = response?.message ?: "Failed to contact bot."
            val latencyText = response?.latency?.let { "Latency: ${it}" } ?: "Latency: n/a"

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "$message\n$latencyText",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
                // Compact spacing and a thin separator line
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(Modifier.fillMaxWidth(), 0.5.dp, DividerDefaults.color)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    )
}

@Preview
@Composable
private fun PingScreen() {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.padding(16.dp)) {
                PingResultDialog(
                    response = PingResponse("Pong!", "123 ms"),
                    onOk = {}
                )
            }
        }
    }
}
