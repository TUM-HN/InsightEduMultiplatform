package org.example.project.ui.reusableComponents

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.example.project.TUMColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDatePicker(
    value: String,
    onDateSelected: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    val datePickerColors = DatePickerDefaults.colors(
        containerColor = TUMColors.tumGray9,
        titleContentColor = TUMColors.tumBlueDark,
        headlineContentColor = TUMColors.tumBlueDark,
        weekdayContentColor = TUMColors.tumGray4,
        subheadContentColor = TUMColors.tumBlueDark,
        navigationContentColor = TUMColors.tumBlue,
        yearContentColor = TUMColors.tumBlueDark,
        currentYearContentColor = TUMColors.tumBlue,
        selectedYearContentColor = Color.White,
        selectedYearContainerColor = TUMColors.tumBlue,
        dayContentColor = TUMColors.tumBlueDark,
        selectedDayContentColor = Color.White,
        selectedDayContainerColor = TUMColors.tumBlue,
        todayContentColor = TUMColors.tumBlue,
        todayDateBorderColor = TUMColors.tumBlue,
        dividerColor = TUMColors.tumGray7,
        dateTextFieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TUMColors.tumBlue,
            unfocusedBorderColor = TUMColors.tumGray4,
            cursorColor = TUMColors.tumBlue,
            focusedLabelColor = TUMColors.tumBlue,
            unfocusedLabelColor = TUMColors.tumGray4
        )
    )

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(text = label, style = MaterialTheme.typography.bodyMedium) },
            textStyle = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            readOnly = true,
            singleLine = true
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showDialog = true }
        )
    }

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val localDate = Instant.fromEpochMilliseconds(millis)
                                .toLocalDateTime(TimeZone.currentSystemDefault()).date
                            val formatted = "${localDate.year}-${localDate.monthNumber.toString().padStart(2, '0')}-${localDate.dayOfMonth.toString().padStart(2, '0')}"
                            onDateSelected(formatted)
                        }
                        showDialog = false
                    }
                ) {
                    Text("OK", color = TUMColors.tumBlue)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false }
                ) {
                    Text("Cancel", color = TUMColors.tumBlue)
                }
            },
            colors = datePickerColors
        ) {
            DatePicker(
                state = datePickerState,
                colors = datePickerColors
            )
        }
    }
}

@Preview
@Composable
private fun DatePickerFieldPreview() {
    MaterialTheme {
        Surface(color = TUMColors.tumGray9) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CustomDatePicker(
                    value = "2024-01-12", onDateSelected = {}, label = "Date", modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                CustomDatePicker(
                    value = "2024-01-12", onDateSelected = {}, label = "Date", modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
