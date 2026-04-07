package org.example.project.ui.reusableComponents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.example.project.TUMColors

@Composable
fun NumberStepperItem(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Number of Messages: $value",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Black
        )

        Row(
            modifier = Modifier
                .background(
                    color = TUMColors.tumGray9,
                    shape = RoundedCornerShape(50)
                ).border(
                    width = 2.dp,
                    color = TUMColors.tumGray8,
                    shape = RoundedCornerShape(50)
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp))
                    .clickable(enabled = value > range.first) {
                        onValueChange(value - 1)
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease",
                    modifier = Modifier.size(20.dp),
                    tint = if (value > range.first) Color.Black else Color.Gray
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(Color.Gray.copy(alpha = 0.5f))
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(topEnd = 50.dp, bottomEnd = 50.dp))
                    .clickable(enabled = value < range.last) {
                        onValueChange(value + 1)
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase",
                    modifier = Modifier.size(20.dp),
                    tint = if (value < range.last) Color.Black else Color.Gray
                )
            }
        }
    }
}

@Preview
@Composable
private fun MessageCardAllTypesPreview() {
    MaterialTheme {
        Surface(color = TUMColors.tumGray9) {
            NumberStepperItem(value = 1, onValueChange = {}, range = 1..10)
        }
    }
}
