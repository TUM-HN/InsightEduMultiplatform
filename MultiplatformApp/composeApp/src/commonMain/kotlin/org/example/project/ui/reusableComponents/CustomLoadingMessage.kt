package org.example.project.ui.reusableComponents

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.example.project.TUMColors

@Composable
fun CustomLoadingMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Preview
@Composable
fun CustomLoadingMessagePreview() {
    MaterialTheme {
        Surface(color = TUMColors.tumGray9) {
            CustomCard(showTitle = false) {
                CustomLoadingMessage(text = "Loading...")
            }
        }
    }
}
