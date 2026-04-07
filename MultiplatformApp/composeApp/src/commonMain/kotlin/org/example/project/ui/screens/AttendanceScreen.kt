package org.example.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.example.project.TUMColors
import org.example.project.data.DefaultAppContainer
import org.example.project.model.viewModels.AttendanceViewModel
import org.example.project.ui.reusableComponents.CustomCard
import org.example.project.ui.reusableComponents.CustomDropdownMenu
import org.example.project.ui.reusableComponents.CustomHorizontalDivider
import org.example.project.ui.reusableComponents.CustomLabeledTextField
import org.example.project.ui.reusableComponents.CustomTextButton
import org.example.project.ui.reusableComponents.HistoryButton
import org.example.project.utils.formatAttendanceDate
import androidx.compose.ui.tooling.preview.Preview
import org.example.project.ui.reusableComponents.CustomLoadingMessage
import org.example.project.ui.reusableComponents.CustomTopBar

@Composable
fun AttendanceScreen(
    viewModel: AttendanceViewModel,
    onBack: () -> Unit,
    isTabletHorizontal: Boolean = false
) {
    LaunchedEffect(Unit) {
        viewModel.loadAttendanceUiState()
        viewModel.fetchAttendanceHistory()
    }

    val scrollState = rememberLazyListState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TUMColors.tumGray9)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = 16.dp)
    ) {
        val uiState by viewModel.uiState.collectAsState()
        if (uiState.selectedHistoryFile == null || isTabletHorizontal) {
            Column(modifier = Modifier.fillMaxSize()) {
                CustomTopBar("Attendance", onBack)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = scrollState
                ) {
                    item {
                        ContentCard(viewModel = viewModel)
                    }
                }
            }
        } else {
            AttendanceDetailScreen(
                viewModel = viewModel,
                onBack = { viewModel.clearSelectedHistory() }
            )
        }
    }
}

@Composable
private fun ContentCard(
    viewModel: AttendanceViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    CustomCard(
        title = "Attendance Setup",
        statusMessage = uiState.setupStatusMessage,
    ) {
        CustomDropdownMenu(
            onItemSelected = { group -> viewModel.onGroupSelected(group) },
            items = uiState.groups,
            selectedItem = uiState.selectedGroup,
            menuTitle = "Group",
            dropdownTitle = "Select a group",
            itemLabel = { group ->
                group?.name ?: "Select a group"
            }
        )

        CustomHorizontalDivider()

        CustomDropdownMenu(
            onItemSelected = { admin -> viewModel.onAdminSelected(admin) },
            items = uiState.admins,
            selectedItem = uiState.selectedAdmin,
            menuTitle = "Admin",
            dropdownTitle = "Select an admin",
            itemLabel = { admin ->
                if (admin != null) {
                    admin.displayName + " (" + admin.name + ")"
                } else "Select an admin"
            }
        )
    }

    CustomCard(
        title = "Attendance Status",
        actionMessage = uiState.attendanceActionMessage,
        onClearActionMessage = { viewModel.clearAttendanceActionMessage() },
        showRedStar = false,
    ) {

        CustomLabeledTextField(
            label = "Current Status:",
            value =
                if (uiState.selectedGroup != null && uiState.selectedGroup!!.attendanceActive) { "Active" }
                else { "Inactive" },
            isValid = (uiState.selectedGroup != null && uiState.selectedGroup!!.attendanceActive),
            textWeight = 4f,
            textFieldWeight = 2f,
            readOnly = true,
        )

        CustomHorizontalDivider()

        CustomLabeledTextField(
            label = "Attendance Code:",
            value = uiState.attendanceCode,
            onValueChange = { code ->
                viewModel.onAttendanceCodeChanged(code)
            },
            placeholder = "Type here",
            enabled = uiState.selectedGroup != null && !uiState.selectedGroup!!.attendanceActive,
            textWeight = 3f,
            textFieldWeight = 2f,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        CustomHorizontalDivider()

        val anyOtherGroupActive = uiState.groups.any { it.attendanceActive && it.name != uiState.selectedGroup?.name }
        val enabled = uiState.selectedGroup != null && uiState.selectedAdmin != null && uiState.attendanceCode.isNotEmpty() && !anyOtherGroupActive

        CustomTextButton(
            onClick = {
                viewModel.manageAttendance()
            },
            text = if (uiState.selectedGroup != null && uiState.selectedGroup!!.attendanceActive) "Stop Attendance" else "Start Attendance",
            enabled = enabled,
            isSending = uiState.isSending,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }

    CustomCard(
        title = "Attendance History",
        actionMessage = uiState.historyActionMessage,
        onClearActionMessage = { viewModel.clearHistoryActionMessage() },
        showRedStar = false,
        titleButton = {
            Box(modifier = Modifier.size(24.dp)) {
                if (uiState.isLoadingHistory) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).padding(4.dp),
                        color = TUMColors.tumBlue,
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(onClick = { viewModel.fetchAttendanceHistory() }) {
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
        if (uiState.attendanceHistory.isEmpty()) {
            CustomLoadingMessage("No history available")
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                uiState.attendanceHistory.forEach { file ->
                    val groupName = file.name.split("_").firstOrNull() ?: file.name
                    HistoryButton(
                        title = groupName,
                        subtitle = formatAttendanceDate(file.created),
                        rightBottomText = "",
                        onClick = { viewModel.fetchAttendanceContent(file) }
                    )
                }
            }
        }
    }
}


@Composable
fun AttendanceDetailScreen(
    viewModel: AttendanceViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val file = uiState.selectedHistoryFile ?: return
    val groupName = file.name.split("_").firstOrNull() ?: file.name

    Column(modifier = Modifier.fillMaxSize()) {
        CustomTopBar(title = "$groupName ${formatAttendanceDate(file.created)}", onBack = onBack)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                CustomCard(
                    title = "Attendees",
                    actionMessage = uiState.historyActionMessage,
                    onClearActionMessage = { viewModel.clearHistoryActionMessage() },
                    showRedStar = false
                ) {
                    if (uiState.isLoadingDetails) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = TUMColors.tumBlue)
                        }
                    } else if (uiState.attendanceDetails.isEmpty()) {
                        CustomLoadingMessage("No attendees found")
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            uiState.attendanceDetails.forEachIndexed { index, entry ->
                                Text(
                                    text = "${index + 1}. ${entry.attendance}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                if (index < uiState.attendanceDetails.size - 1) {
                                    CustomHorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun AttendanceScreenPreview() {
    val repo = DefaultAppContainer().discordBotRepository
    MaterialTheme {
        Surface(color = TUMColors.tumGray9) {
            Box(modifier = Modifier.padding(16.dp)) {
                AttendanceScreen(AttendanceViewModel(repo), {})
            }
        }
    }
}
