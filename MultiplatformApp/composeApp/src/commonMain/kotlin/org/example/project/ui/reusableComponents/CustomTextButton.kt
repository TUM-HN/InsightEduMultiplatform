package org.example.project.ui.reusableComponents

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.example.project.TUMColors

@Composable
fun CustomTextButton(
    onClick: () -> Unit,
    text: String,
    enabled: Boolean,
    isSending: Boolean,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
    ) {
        if (isSending) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = TUMColors.tumBlue
            )
        } else {
            Text(
                text,
                color = if (enabled) TUMColors.tumBlue else TUMColors.tumGray7,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Preview
@Composable
private fun MessageCardAllTypesPreview() {
    MaterialTheme {
        Surface(color = TUMColors.tumGray9) {
            CustomCard(showTitle = false) {
                CustomTextButton(onClick = {}, text = "Button", enabled = true, isSending = false)
                CustomHorizontalDivider()
                CustomTextButton(onClick = {}, text = "Button", enabled = false, isSending = false)
                CustomHorizontalDivider()
                CustomTextButton(onClick = {}, text = "Button", enabled = true, isSending = true)
            }
        }
    }
}
