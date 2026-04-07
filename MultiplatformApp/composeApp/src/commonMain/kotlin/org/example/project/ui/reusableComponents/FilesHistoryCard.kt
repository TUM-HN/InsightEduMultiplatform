package org.example.project.ui.reusableComponents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.example.project.TUMColors
import org.example.project.model.FileDto
import org.example.project.model.UiMessage
import org.example.project.utils.formatAttendanceDate

@Composable
fun FilesHistoryCard(
    history: List<FileDto>,
    isLoading: Boolean,
    onItemClick: (FileDto) -> Unit,
    title: String,
    isFeedback: Boolean,
    onReload: () -> Unit,
    statusMessage: UiMessage? = null,
    actionMessage: UiMessage? = null,
    onClearActionMessage: (() -> Unit)? = null,
) {
    CustomCard(
        title = title,
        statusMessage = statusMessage,
        actionMessage = actionMessage,
        onClearActionMessage = onClearActionMessage,
        showRedStar = false,
        titleButton = {
            Box(modifier = Modifier.size(24.dp)) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).padding(4.dp),
                        color = TUMColors.tumBlue,
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(onClick = onReload) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Reload Statistics",
                            tint = TUMColors.tumBlue
                        )
                    }
                }
            }
        }
    ) {
        if (history.isEmpty()) {
            CustomLoadingMessage("No history available")
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                history.forEach { file ->
                    val groupName = if (isFeedback) {
                        file.name.split("_")[0]
                    } else {
                        file.name.split("_")[1]
                    }
                    HistoryButton(
                        title = groupName,
                        subtitle = formatAttendanceDate(file.created),
                        rightBottomText = "",
                        onClick = { onItemClick(file) },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun FileHistoryCardPreview() {
    MaterialTheme {
        Surface(color = TUMColors.tumGray9) {
            CustomCard(showTitle = false) {
                FilesHistoryCard(
                    history = emptyList(),
                    isLoading = false,
                    onItemClick = {},
                    title = "Test Preview History",
                    isFeedback = false,
                    onReload = {},
                )
                FilesHistoryCard(
                    history = listOf(
                        FileDto(
                            created = "created",
                            modified = "modified",
                            name = "test_1",
                            size = 1,
                        )
                    ),
                    isLoading = false,
                    onItemClick = {},
                    title = "Test Preview History",
                    isFeedback = false,
                    onReload = {},
                )
            }
        }
    }
}
