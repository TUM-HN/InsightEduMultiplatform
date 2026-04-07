package org.example.project.ui.reusableComponents

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.example.project.TUMColors

@Composable
fun HorizontalDistributionChart(
    items: List<Pair<String, Int>>,
    modifier: Modifier = Modifier,
    colorsPalette: List<Color?> = arrayOfNulls<Color>(items.size).toList(),
    backgroundColor: Color = TUMColors.tumGray7
) {
    val maxCount = items.maxOfOrNull { it.second } ?: 1

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.forEachIndexed { index, (label, count) ->
            if (count != 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(2f)
                    )

                    Box(
                        modifier = Modifier
                            .weight(3f)
                            .height(24.dp)
                    ) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            drawRoundRect(
                                color = backgroundColor,
                                cornerRadius = CornerRadius(4.dp.toPx())
                            )
                            val fraction = if (maxCount > 0) count.toFloat() / maxCount else 0f
                            drawRoundRect(
                                color = colorsPalette[index] ?: TUMColors.tumBlue,
                                size = size.copy(width = size.width * fraction),
                                cornerRadius = CornerRadius(4.dp.toPx())
                            )
                        }
                    }

                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
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
            HorizontalDistributionChart(
                items = listOf(
                    "Option 1" to 10,
                    "Option 2" to 20,
                    "Option 3" to 30
                ),
            )
        }
    }
}
