package org.example.project.ui.reusableComponents

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import org.example.project.TUMColors

@Composable
fun CustomTextField(
    message: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    isTitle: Boolean = false,
) {
    TextField(
        value = message,
        onValueChange = { onValueChange(it) },
        placeholder = {
            Text(
                placeholder,
                color = if (isTitle) TUMColors.tumGray4 else TUMColors.tumGray7,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = TUMColors.tumBlue,
        ),
        singleLine = false,
        minLines = 1,
        enabled = enabled,
        readOnly = isTitle,
    )
}

@Preview
@Composable
private fun CustomTextFieldPreview() {
    MaterialTheme {
        Surface(color = TUMColors.tumGray9) {
            CustomTextField(
                message = "Assign a role to a Discord member",
                placeholder = "",
                onValueChange = {},
                enabled = false,
            )
        }
    }
}
