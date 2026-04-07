package org.example.project.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import org.example.project.TUMColors
import org.example.project.data.DefaultAppContainer
import org.example.project.model.QAPair
import org.example.project.model.QuestionStatistics
import org.example.project.model.QuizStatistics
import org.example.project.model.ScoreDistribution
import org.example.project.model.UiMessage
import org.example.project.model.viewModels.ContentQuestionsViewModel
import org.example.project.ui.reusableComponents.CustomCard
import org.example.project.ui.reusableComponents.CustomDropdownMenu
import org.example.project.ui.reusableComponents.CustomHorizontalDivider
import org.example.project.ui.reusableComponents.CustomTextButton
import org.example.project.ui.reusableComponents.CustomToggle
import org.example.project.ui.reusableComponents.CustomTopBar
import org.example.project.ui.reusableComponents.DurationSlider
import org.example.project.ui.reusableComponents.HistoryButton
import org.example.project.ui.reusableComponents.NumberStepperItem
import org.example.project.ui.reusableComponents.HorizontalDistributionChart
import org.example.project.utils.formatAttendanceDate
import androidx.compose.ui.tooling.preview.Preview
import org.example.project.ui.reusableComponents.CustomLoadingMessage

@Composable
fun ContentQuestionsScreen(
    viewModel: ContentQuestionsViewModel,
    onBack: () -> Unit,
    onSelectPdf: (onFileSelected: (String, ByteArray) -> Unit) -> Unit,
    isTabletHorizontal: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadChannels()
        viewModel.fetchQAStatistics()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TUMColors.tumGray9)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = 16.dp)
    ) {
        if (uiState.selectedQuiz != null && !isTabletHorizontal) {
            QuizDetailScreen(
                quiz = uiState.selectedQuiz!!,
                onBack = { viewModel.clearSelectedQuiz() }
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                CustomTopBar("Content Questions", onBack)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState
                ) {
                    item {
                        CustomCard(
                            title = "Generate Questions from PDF",
                            actionMessage = uiState.generateActionMessage,
                            showRedStar = false,
                            onClearActionMessage = { viewModel.clearGenerateActionMessage() },
                        ) {
                            if (uiState.selectedFileName != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.Description,
                                            contentDescription = null,
                                            tint = TUMColors.tumBlue
                                        )
                                        Text(
                                            text = uiState.selectedFileName ?: "",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                    IconButton(onClick = { viewModel.clearFile() }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = "Remove file",
                                            tint = TUMColors.tumRed
                                        )
                                    }
                                }
                            } else {
                                Button(
                                    onClick = {
                                        onSelectPdf { fileName, bytes ->
                                            viewModel.onFileSelected(fileName, bytes)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors()
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.UploadFile,
                                        contentDescription = null,
                                        tint = TUMColors.tumBlue
                                    )
                                    Text(
                                        text = "Select PDF File",
                                        modifier = Modifier.padding(start = 8.dp),
                                        color = TUMColors.tumBlue
                                    )
                                }
                            }

                            CustomHorizontalDivider()

                            NumberStepperItem(
                                value = uiState.numberOfQuestions,
                                onValueChange = { newValue -> viewModel.onNumberOfQuestionsChanged(newValue) },
                                range = 1..5
                            )

                            CustomHorizontalDivider()

                            CustomTextButton(
                                onClick = { viewModel.submitContentQuestions() },
                                text = if (uiState.generatedQAPairs.isNotEmpty()) "Regenerate Questions" else "Generate Questions",
                                enabled = uiState.selectedFileName != null,
                                isSending = uiState.isSending,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }

                    item {
                        CustomCard(
                            title = "Questionary settings",
                            statusMessage = uiState.settingsStatusMessage,
                        ) {
                            CustomToggle(
                                label = "AI Feedback",
                                checked = uiState.aiFeedback,
                                onCheckedChange = { viewModel.onAiFeedbackChange(it) },
                            )

                            CustomDropdownMenu(
                                onItemSelected = { channel -> viewModel.onChannelSelected(channel) },
                                items = uiState.channels,
                                selectedItem = uiState.selectedChannel,
                                menuTitle = "Channel",
                                dropdownTitle = "Select a channel",
                                itemLabel = { channel -> channel?.name ?: "Select a channel" }
                            )

                            DurationSlider(
                                duration = uiState.duration,
                                onDurationChanged = { viewModel.onDurationChanged(it) },
                                valueRange = 60f..600f
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Questions & Answers",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                            Row {
                                Button(
                                    onClick = {
                                        viewModel.addNewQAPair()
                                        coroutineScope.launch {
                                            val targetIndex = 3 + uiState.generatedQAPairs.size
                                            listState.animateScrollToItem(targetIndex)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = TUMColors.tumBlue),
                                    enabled = uiState.numberOfQuestions != 5,
                                    modifier = Modifier.padding(end = 16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Add,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add")
                                }
                            }
                        }
                    }

                    if (uiState.generatedQAPairs.isNotEmpty()) {
                        itemsIndexed(uiState.generatedQAPairs) { index, qaPair ->
                            val isEditing = uiState.editingIndex == index
                            val modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp)
                            CustomCard(
                                title = "Question ${index + 1}",
                                showRedStar = false,
                            ) {
                                if (isEditing) {
                                    OutlinedTextField(
                                        value = uiState.editingQuestion,
                                        onValueChange = { viewModel.updateEditingQuestion(it) },
                                        label = { Text("Question") },
                                        supportingText = {
                                            Text(
                                                text = "${uiState.editingQuestion.length}/100",
                                                modifier = Modifier.fillMaxWidth(),
                                                style = MaterialTheme.typography.labelSmall,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                                            )
                                        },
                                        modifier = modifier,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = TUMColors.tumBlue,
                                            unfocusedBorderColor = TUMColors.tumBlue,
                                            focusedLabelColor = TUMColors.tumBlue,
                                            unfocusedLabelColor = TUMColors.tumBlue
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = uiState.editingAnswer,
                                        onValueChange = { viewModel.updateEditingAnswer(it) },
                                        label = { Text("Answer") },
                                        modifier = modifier,
                                        minLines = 2,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = TUMColors.tumBlue,
                                            unfocusedBorderColor = TUMColors.tumBlue,
                                            focusedLabelColor = TUMColors.tumBlue,
                                            unfocusedLabelColor = TUMColors.tumBlue
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { viewModel.cancelEditing() }) {
                                            Text("Cancel", color = Color.Black)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = { viewModel.saveEditing() },
                                            colors = ButtonDefaults.buttonColors(containerColor = TUMColors.tumBlue)
                                        ) {
                                            Text("Save")
                                        }
                                    }
                                } else {
                                    Column {
                                        Text(
                                            text = "Q: ${qaPair.question}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = modifier.padding(bottom = 8.dp)
                                        )
                                        Text(
                                            text = "A: ${qaPair.answer}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = modifier
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            IconButton(onClick = { viewModel.startEditing(index) }) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Edit,
                                                    contentDescription = "Edit",
                                                    tint = TUMColors.tumBlue
                                                )
                                            }
                                            IconButton(onClick = { viewModel.deleteQAPair(index) }) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Delete,
                                                    contentDescription = "Delete",
                                                    tint = TUMColors.tumBlue
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            CustomCard(
                                showTitle = false,
                                statusMessage = uiState.settingsStatusMessage,
                                actionMessage = uiState.publishActionMessage,
                                onClearActionMessage = { viewModel.clearPublishActionMessage() },
                            ) {
                                Button(
                                    onClick = { viewModel.sendQuestionsToDiscord() },
                                    enabled = !uiState.isSending && uiState.selectedChannel != null && uiState.generatedQAPairs.isNotEmpty() && uiState.editingIndex == null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TUMColors.tumBlue)
                                ) {
                                    if (uiState.isSending) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text("Send Questions to Discord")
                                }
                            }
                        }
                    }

                    item {
                        QuizHistoryCard(
                            quizHistory = uiState.quizHistory,
                            isLoading = uiState.isLoadingHistory,
                            actionMessage = uiState.historyActionMessage,
                            onClearActionMessage = { viewModel.clearHistoryActionMessage() },
                            onQuizClick = { viewModel.selectQuiz(it) },
                            onReload = { viewModel.fetchQAStatistics() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuizHistoryCard(
    quizHistory: List<QuizStatistics>,
    isLoading: Boolean,
    actionMessage: UiMessage? = null,
    onClearActionMessage: (() -> Unit)? = null,
    onQuizClick: (QuizStatistics) -> Unit,
    onReload: () -> Unit
) {
    CustomCard(
        title = "Quiz Statistics (AI Feedback)",
        showRedStar = false,
        statusMessage = null,
        actionMessage = actionMessage,
        onClearActionMessage = onClearActionMessage,
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
        if (quizHistory.isEmpty()) {
            CustomLoadingMessage("No quizzes with AI feedback found")
        } else {
            Box {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    quizHistory.forEach { quiz ->
                        HistoryButton(
                            title = formatAttendanceDate(quiz.timestamp),
                            subtitle = "${quiz.totalQuestions} questions • ${quiz.totalResponses} responses",
                            rightBottomText = "",
                            onClick = { onQuizClick(quiz) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuizDetailScreen(
    quiz: QuizStatistics,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        CustomTopBar(
            title = formatAttendanceDate(quiz.timestamp),
            onBack = onBack
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            quiz.questions.forEachIndexed { index, question ->
                item {
                    CustomCard(
                        title = "${index + 1}) " + question.question,
                        showRedStar = false,
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append("Correct Answer: ")
                                }
                                append(question.correctAnswer)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        val distribution = question.scoreDistribution.toDistributionList()
                        HorizontalDistributionChart(
                            items = distribution,
                            colorsPalette = listOf(
                                TUMColors.tumGreen,
                                TUMColors.tumYellow,
                                TUMColors.tumOrange.copy(alpha = 0.8f),
                                TUMColors.tumRed,
                            ),
                        )
                    }
                }
            }
        }
    }
}

private fun ScoreDistribution.toDistributionList(): List<Pair<String, Int>> {
    return listOf(
        "correct" to correct,
        "mostly correct" to mostlyCorrect,
        "mostly incorrect" to mostlyIncorrect,
        "incorrect" to incorrect
    )
}

@Preview
@Composable
private fun ContentQuestionsScreenPreview() {
    val sampleQAPairs = listOf(
        QAPair(
            question = "What is the main purpose of Kotlin Multiplatform?",
            answer = "Kotlin Multiplatform allows sharing code between different platforms like Android, iOS, web, and desktop while still enabling platform-specific implementations when needed."
        ),
        QAPair(
            question = "What are the benefits of using Compose Multiplatform?",
            answer = "Compose Multiplatform enables building declarative UIs that can be shared across platforms, reducing code duplication and maintaining consistent user experiences."
        ),
        QAPair(
            question = "How does state management work in Jetpack Compose?",
            answer = "Jetpack Compose uses a reactive state model where UI automatically recomposes when state changes, typically managed through State, MutableState, or StateFlow objects."
        )
    )

    val repo = DefaultAppContainer().discordBotRepository
    MaterialTheme {
        Surface(color = TUMColors.tumGray9) {
            ContentQuestionsScreen(
                viewModel = ContentQuestionsViewModel(repo).apply {
                    setQAPairs(sampleQAPairs)
                },
                onBack = {},
                onSelectPdf = {}
            )
        }
    }
}

private val sampleQuiz = QuizStatistics(
    timestamp = "Tue 3 March 17:14",
    totalQuestions = 1,
    totalResponses = 8,
    aiFeedbackEnabled = true,
    filename = "",
    questions = listOf(
        QuestionStatistics(
            question = "What is the main purpose of Kotlin Multiplatform?",
            correctAnswer = "Kotlin Multiplatform allows sharing code between different platforms like Android, iOS, web, and desktop while still enabling platform-specific implementations when needed.",
            totalAnswers = 8,
            scoreDistribution = ScoreDistribution(
                correct = 4,
                mostlyCorrect = 2,
                mostlyIncorrect = 1,
                incorrect = 1
            )
        )
    )
)

@Preview
@Composable
private fun QuizDetailScreenPreview() {
    MaterialTheme {
        Surface(color = TUMColors.tumGray9) {
            QuizDetailScreen(quiz = sampleQuiz, onBack = {})
        }
    }
}
