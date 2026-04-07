package org.example.project.ui.reusableComponents

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.example.project.TUMColors

@Composable
fun CustomAlertDialog(
    title: String,
    text: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
            }) { Text("Yes", color = TUMColors.tumBlue) }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("No", color = TUMColors.tumBlue)
            }
        },
        title = { Text(text = title)},
        text = { Text(text = text) }
    )
}

@Preview
@Composable
private fun MessageCardAllTypesPreview() {
    MaterialTheme {
        Surface(color = TUMColors.tumGray9) {
            CustomAlertDialog(
                title = "Title",
                text = "Text",
                onDismiss = {},
                onConfirm = {}
            )
        }
    }
}