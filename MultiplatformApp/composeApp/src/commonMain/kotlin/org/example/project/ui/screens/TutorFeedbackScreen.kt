package org.example.project.ui.screens

import androidx.compose.foundation.background
import org.example.project.model.viewModels.TutorFeedbackViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.example.project.TUMColors
import org.example.project.data.DefaultAppContainer
import org.example.project.ui.reusableComponents.CustomCard
import org.example.project.ui.reusableComponents.CustomDropdownMenu
import org.example.project.ui.reusableComponents.CustomHorizontalDivider
import org.example.project.ui.reusableComponents.CustomTextButton
import org.example.project.ui.reusableComponents.CustomToggle
import org.example.project.ui.reusableComponents.CustomTopBar
import org.example.project.ui.reusableComponents.DurationSlider
import org.example.project.ui.reusableComponents.FilesHistoryCard
import org.example.project.ui.reusableComponents.HorizontalDistributionChart
import org.example.project.utils.formatAttendanceDate
import androidx.compose.ui.tooling.preview.Preview
import org.example.project.ui.reusableComponents.CustomDatePicker
import org.example.project.ui.reusableComponents.CustomLoadingMessage

@Composable
fun TutorFeedbackScreen(
    viewModel: TutorFeedbackViewModel,
    onBack: () -> Unit,
    isTabletHorizontal: Boolean = false
) {
    LaunchedEffect(Unit) {
        viewModel.loadFeedbackUiState()
        viewModel.fetchFeedbackHistory()
    }

    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TUMColors.tumGray9)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        if (uiState.selectedFeedbackFile == null || isTabletHorizontal) {
            Column(modifier = Modifier.fillMaxSize()) {
                CustomTopBar("Tutor Feedback", onBack)
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
            FeedbackDetailScreen(
                viewModel = viewModel,
                onBack = { viewModel.clearSelectedHistory() }
            )
        }
    }
}

@Composable
private fun ContentCard(
    viewModel: TutorFeedbackViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    CustomCard(
        title = "Feedback Configuration",
        statusMessage = uiState.configurationStatusMessage,
        actionMessage = uiState.configurationActionMessage,
        onClearActionMessage = { viewModel.clearConfigurationActionMessage() },
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
            onItemSelected = { channel -> viewModel.onChannelSelected(channel) },
            items = uiState.channels,
            selectedItem = uiState.selectedChannel,
            menuTitle = "Channel",
            dropdownTitle = "Select a channel",
            itemLabel = { channel ->
                channel?.name ?: "Select a channel"
            }
        )

        CustomHorizontalDivider()

        DurationSlider(
            duration = uiState.duration,
            onDurationChanged = { viewModel.onDurationChanged(it) }
        )

        CustomHorizontalDivider()

        val enabled = uiState.selectedGroup != null && uiState.selectedChannel != null

        CustomTextButton(
            onClick = {
                viewModel.manageFeedback()
            },
            text = "Start Questionnaire",
            enabled = enabled,
            isSending = uiState.isSending,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }

    CustomCard(
        title = "AI Feedback Analysis",
        showRedStar = false,
        actionMessage = uiState.analysisActionMessage,
        onClearActionMessage = { viewModel.clearAnalysisActionMessage() },
    ) {
        Text(
            text = "Time Range:",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CustomDatePicker(
                value = uiState.startDate,
                onDateSelected = { date -> viewModel.onStartDateChanged(date) },
                label = "Start Date",
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            CustomDatePicker(
                value = uiState.endDate,
                onDateSelected = { date -> viewModel.onEndDateChanged(date) },
                label = "End Date",
                modifier = Modifier.weight(1f)
            )
        }

        CustomHorizontalDivider()

        CustomToggle(
            label = "Statistics:",
            checked = uiState.includeStatistics,
            onCheckedChange = { viewModel.onIncludeStatisticsChanged(it) },
            enabled = true
        )

        CustomHorizontalDivider()

        CustomDropdownMenu(
            onItemSelected = { channel ->
                if (channel != null) {
                    viewModel.onChannelSelectionToggled(channel)
                }
            },
            items = uiState.channels,
            selectedItem = null,
            menuTitle = "Channels:",
            dropdownTitle = "Select channels",
            itemLabel = { channel ->
                channel?.name ?: "Select channels"
            },
            isNoneOption = false,
            showRedStar = false,
            selectedItems = uiState.selectedAnalysisChannels,
            isMultiSelect = true
        )

        uiState.selectedAnalysisChannels.forEach { channel ->
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = TUMColors.tumGray8)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
                IconButton(onClick = { viewModel.onChannelSelectionToggled(channel) }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = TUMColors.tumRed
                    )
                }
            }
        }

        CustomHorizontalDivider()

        val canGenerateAnalysis = uiState.startDate.isNotEmpty() && 
                                  uiState.endDate.isNotEmpty() && 
                                  (uiState.includeStatistics || uiState.selectedAnalysisChannels.isNotEmpty())

        CustomTextButton(
            onClick = { viewModel.generateAiAnalysis() },
            text = "Generate Analysis",
            enabled = canGenerateAnalysis,
            isSending = uiState.isGeneratingAnalysis,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }

    if (uiState.aiAnalysis != null) {
        CustomCard(showTitle = false) {
            uiState.aiAnalysis?.let { analysis ->
                Text(
                    text = "AI Analysis:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp).padding(horizontal = 16.dp)
                )
                Text(
                    text = analysis,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp).padding(horizontal = 16.dp)
                )
            }
        }
    }

    FilesHistoryCard(
        title = "Feedback History",
        isFeedback = true,
        history = uiState.feedbackHistory,
        isLoading = uiState.isLoadingHistory,
        actionMessage = uiState.historyActionMessage,
        onClearActionMessage = { viewModel.clearHistoryActionMessage() },
        onItemClick = { viewModel.fetchFeedbackContent(it) },
        onReload = { viewModel.fetchFeedbackHistory() }
    )
    Spacer(Modifier.height(16.dp))
}


@Composable
fun FeedbackDetailScreen(
    viewModel: TutorFeedbackViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val file = uiState.selectedFeedbackFile ?: return
    val groupName = file.name.split("_").firstOrNull() ?: file.name

    Column(modifier = Modifier.fillMaxSize()) {
        CustomTopBar(title = "$groupName ${formatAttendanceDate(file.created)}", onBack = onBack)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                CustomCard(
                    title = "Response Distribution",
                    actionMessage = uiState.historyActionMessage,
                    onClearActionMessage = { viewModel.clearHistoryActionMessage() },
                    showRedStar = false
                ) {
                    if (uiState.isLoadingDetails) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = TUMColors.tumBlue)
                        }
                    } else if (uiState.feedbackDetails.isEmpty()) {
                        CustomLoadingMessage("No attendees found")
                    } else {
                        val categories = listOf("Good", "Satisfactory", "Poor")
                        val countsMap = uiState.feedbackDetails
                            .groupingBy { it.feedback }
                            .eachCount()
                        val distribution = categories.map { category -> category to (countsMap[category] ?: 0) }

                        HorizontalDistributionChart(items = distribution, colorsPalette = listOf(TUMColors.tumGreen, TUMColors.tumYellow, TUMColors.tumRed))
                    }
                }
            }

            val openFeedbackEntries = uiState.feedbackDetails.filter { !it.openFeedback.isNullOrBlank() }
            if (openFeedbackEntries.isNotEmpty()) {
                item {
                    CustomCard(
                        title = "Open Feedback",
                        actionMessage = uiState.historyActionMessage,
                        onClearActionMessage = { viewModel.clearHistoryActionMessage() },
                        showRedStar = false
                    ) {
                        var hadOpenFeedback = false
                        var index = 1
                        openFeedbackEntries.forEach { entry ->
                            if (entry.openFeedback != "No feedback provided.") {
                                hadOpenFeedback = true
                                Text(
                                    text = "${index++}) ${entry.openFeedback}",
                                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp),
                                )
                            }
                        }
                        if (!hadOpenFeedback) {
                            CustomLoadingMessage("No open feedback")
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun TutorFeedbackScreenPreview() {
    val repo = DefaultAppContainer().discordBotRepository
    MaterialTheme {
        Surface(color = TUMColors.tumGray9) {
            Box(modifier = Modifier.padding(16.dp)) {
                TutorFeedbackScreen(TutorFeedbackViewModel(repo), {})
            }
        }
    }
}
