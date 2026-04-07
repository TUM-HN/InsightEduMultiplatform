package org.example.project.ui.reusableComponents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.example.project.TUMColors

@Composable
fun CustomToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) Color.Black else TUMColors.tumGray4,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = TUMColors.tumBlue,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = TUMColors.tumGray9,
                uncheckedBorderColor = TUMColors.tumGray8,
                disabledCheckedTrackColor = TUMColors.tumBlue.copy(alpha = 0.5f),
                disabledUncheckedTrackColor = TUMColors.tumGray7.copy(alpha = 0.5f),
                disabledUncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                disabledCheckedThumbColor = Color.White.copy(alpha = 0.5f),
                disabledUncheckedBorderColor = TUMColors.tumGray8.copy(alpha = 0.5f),
                disabledCheckedBorderColor = TUMColors.tumGray8.copy(alpha = 0.5f),
            )
        )
    }
}

@Preview
@Composable
private fun MessageCardAllTypesPreview() {
    MaterialTheme {
        Surface(color = TUMColors.tumGray9) {
            CustomCard(showTitle = false) {
                CustomToggle(label = "Label", checked = true, onCheckedChange = {})
                CustomHorizontalDivider()
                CustomToggle(label = "Label", checked = false, onCheckedChange = {})
            }
        }
    }
}
