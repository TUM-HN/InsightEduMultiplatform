package org.example.project.ui.reusableComponents

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.example.project.TUMColors
import kotlin.math.roundToInt

@Composable
fun DurationSlider(
    duration: Int,
    onDurationChanged: (Int) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 30f..300f,
    steps: Int = 53,
) {
    val minutes = duration / 60
    val seconds = duration % 60
    val durationText = if (minutes > 0) {
        if (seconds > 0) "$minutes min $seconds sec" else "$minutes min"
    } else {
        "$seconds sec"
    }

    CustomLabeledTextField(
        label = "Duration:",
        value = durationText,
        readOnly = true,
    )

    CustomHorizontalDivider()

    CustomSlider(
        value = duration.toFloat(),
        onValueChange = { onDurationChanged(it.roundToInt()) },
        valueRange = valueRange,
        steps = steps,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 30f..300f,
    steps: Int = 0,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth(),
            thumb = {
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(24.dp)
                            .border(width = 2.dp, color = TUMColors.tumGray8, shape = CircleShape),
                    ) {
                        drawCircle(
                            color = TUMColors.tumGray9,
                        )
                    }
                }
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(8.dp),
                    thumbTrackGapSize = 0.dp,
                    colors = SliderDefaults.colors(
                        disabledActiveTickColor = Color.Transparent,
                        disabledInactiveTrackColor = Color.Transparent,
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent,
                        activeTrackColor = TUMColors.tumBlue,
                        inactiveTrackColor = TUMColors.tumGray9
                    ),
                    drawStopIndicator = null
                )
            }
        )
    }
}

@Preview
@Composable
fun DurationSliderPreview() {
    MaterialTheme {
        Surface(color = TUMColors.tumGray9) {
            CustomCard(title = "Duration") {
                DurationSlider(90, {})
            }
        }
    }
}
