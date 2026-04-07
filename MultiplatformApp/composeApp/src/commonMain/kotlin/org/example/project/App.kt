package org.example.project

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch
import org.example.project.data.DefaultAppContainer
import org.example.project.data.RepositoryResponse
import org.example.project.model.viewModels.ClearMessagesViewModel
import org.example.project.model.viewModels.AssignRoleViewModel
import org.example.project.model.viewModels.SendMessageViewModel
import org.example.project.model.PingResponse
import org.example.project.model.viewModels.AttendanceViewModel
import org.example.project.ui.screens.ClearMessagesScreen
import org.example.project.ui.CommandsList
import org.example.project.ui.screens.AssignRoleScreen
import org.example.project.ui.screens.SendMessageScreen
import org.example.project.ui.Header
import org.example.project.model.viewModels.HeaderUiState
import org.example.project.model.viewModels.HeaderViewModel
import org.example.project.model.viewModels.SettingsViewModel
import org.example.project.model.viewModels.SurveyViewModel
import org.example.project.model.viewModels.TutorFeedbackViewModel
import org.example.project.model.viewModels.ContentQuestionsViewModel
import org.example.project.ui.PingResultDialog
import org.example.project.ui.screens.AttendanceScreen
import org.example.project.ui.screens.SettingsScreen
import org.example.project.ui.screens.SurveyScreen
import org.example.project.ui.screens.TutorFeedbackScreen
import org.example.project.ui.screens.ContentQuestionsScreen
import org.example.project.ui.screens.QuizDetailScreen
import org.example.project.ui.screens.AttendanceDetailScreen
import org.example.project.ui.screens.SurveyDetailScreen
import org.example.project.ui.screens.FeedbackDetailScreen

@Composable
fun App() {
    MaterialTheme {
        val appContainer = remember { DefaultAppContainer() }
        val discordBotRepository = remember { appContainer.discordBotRepository }
        val headerViewModel = remember { HeaderViewModel(discordBotRepository) }
        val headerUiState = headerViewModel.uiState.collectAsState()
        val sendMessageViewModel = remember { SendMessageViewModel(discordBotRepository) }
        val assignRoleViewModel = remember { AssignRoleViewModel(discordBotRepository) }
        val clearMessagesViewModel = remember { ClearMessagesViewModel(discordBotRepository) }
        val settingsViewModel = remember { SettingsViewModel(discordBotRepository) }
        val attendanceViewModel = remember { AttendanceViewModel(discordBotRepository) }
        val tutorFeedbackViewModel = remember { TutorFeedbackViewModel(discordBotRepository) }
        val simpleSurveyViewModel = remember { SurveyViewModel(discordBotRepository, true) }
        val complexSurveyViewModel = remember { SurveyViewModel(discordBotRepository, false) }
        val contentQuestionsViewModel = remember { ContentQuestionsViewModel(discordBotRepository) }

        var currentScreen by remember { mutableStateOf("home") }
        var pingResponse by remember { mutableStateOf<PingResponse?>(null) }
        var showPingDialog by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val listState = rememberLazyListState()
        val focusManager = LocalFocusManager.current
        
        var pendingFileCallback by remember { mutableStateOf<((String, ByteArray) -> Unit)?>(null) }
        val pdfPickerLauncher = rememberFilePickerLauncher(
            type = FileKitType.File(extensions = listOf("pdf"))
        ) { file ->
            if (file != null) {
                scope.launch {
                    val bytes = file.readBytes()
                    pendingFileCallback?.invoke(file.name, bytes)
                }
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .background(TUMColors.tumGray9)
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                },
            contentAlignment = Alignment.TopCenter,
        ) {
            val isTabletHorizontal = maxWidth > maxHeight && maxWidth >= 900.dp
            val contentQuestionsUiState by contentQuestionsViewModel.uiState.collectAsState()
            val selectedQuiz = contentQuestionsUiState.selectedQuiz
            val attendanceUiState by attendanceViewModel.uiState.collectAsState()
            val selectedAttendanceFile = attendanceUiState.selectedHistoryFile
            val simpleSurveyUiState by simpleSurveyViewModel.uiState.collectAsState()
            val selectedSimpleSurveyFile = simpleSurveyUiState.selectedSurveyFile
            val complexSurveyUiState by complexSurveyViewModel.uiState.collectAsState()
            val selectedComplexSurveyFile = complexSurveyUiState.selectedSurveyFile
            val tutorFeedbackUiState by tutorFeedbackViewModel.uiState.collectAsState()
            val selectedFeedbackFile = tutorFeedbackUiState.selectedFeedbackFile

            if (isTabletHorizontal) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        val leftScreen = if (currentScreen == "contentQuestions") {
                            "contentQuestions"
                        } else if (currentScreen == "attendance") {
                            "attendance"
                        } else if (currentScreen == "simpleSurvey") {
                            "simpleSurvey"
                        } else if (currentScreen == "complexSurvey") {
                            "complexSurvey"
                        } else if (currentScreen == "tutorFeedback") {
                            "tutorFeedback"
                        } else {
                            "home"
                        }

                        when (leftScreen) {
                            "home" -> {
                                HomeScreen(
                                    listState = listState,
                                    headerUiState = headerUiState.value,
                                    headerViewModel = headerViewModel,
                                    onSettings = { currentScreen = "settings" },
                                    discordBotRepository = discordBotRepository,
                                    scope = scope,
                                    onPing = { response ->
                                        pingResponse = response
                                        showPingDialog = true
                                    },
                                    onNavigate = { currentScreen = it }
                                )
                            }
                            "contentQuestions" -> {
                                ContentQuestionsScreen(
                                    viewModel = contentQuestionsViewModel,
                                    onBack = { 
                                        currentScreen = "home"
                                        contentQuestionsViewModel.clearSelectedQuiz()
                                    },
                                    onSelectPdf = { onFileSelected ->
                                        pendingFileCallback = onFileSelected
                                        pdfPickerLauncher.launch()
                                    },
                                    isTabletHorizontal = true
                                )
                            }
                            "attendance" -> {
                                AttendanceScreen(
                                    viewModel = attendanceViewModel,
                                    onBack = {
                                        currentScreen = "home"
                                        attendanceViewModel.clearSelectedHistory()
                                    },
                                    isTabletHorizontal = true
                                )
                            }
                            "simpleSurvey" -> {
                                SurveyScreen(
                                    viewModel = simpleSurveyViewModel,
                                    isSimple = true,
                                    onBack = {
                                        currentScreen = "home"
                                        simpleSurveyViewModel.clearSelectedHistory()
                                    },
                                    isTabletHorizontal = true
                                )
                            }
                            "complexSurvey" -> {
                                SurveyScreen(
                                    viewModel = complexSurveyViewModel,
                                    isSimple = false,
                                    onBack = {
                                        currentScreen = "home"
                                        complexSurveyViewModel.clearSelectedHistory()
                                    },
                                    isTabletHorizontal = true
                                )
                            }
                            "tutorFeedback" -> {
                                TutorFeedbackScreen(
                                    viewModel = tutorFeedbackViewModel,
                                    onBack = {
                                        currentScreen = "home"
                                        tutorFeedbackViewModel.clearSelectedHistory()
                                    },
                                    isTabletHorizontal = true
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        if (selectedQuiz != null && currentScreen == "contentQuestions") {
                            QuizDetailScreen(
                                quiz = selectedQuiz,
                                onBack = { contentQuestionsViewModel.clearSelectedQuiz() }
                            )
                        } else if (selectedAttendanceFile != null && currentScreen == "attendance") {
                            AttendanceDetailScreen(
                                viewModel = attendanceViewModel,
                                onBack = { attendanceViewModel.clearSelectedHistory() }
                            )
                        } else if (selectedSimpleSurveyFile != null && currentScreen == "simpleSurvey") {
                            SurveyDetailScreen(
                                viewModel = simpleSurveyViewModel,
                                onBack = { simpleSurveyViewModel.clearSelectedHistory() }
                            )
                        } else if (selectedComplexSurveyFile != null && currentScreen == "complexSurvey") {
                            SurveyDetailScreen(
                                viewModel = complexSurveyViewModel,
                                onBack = { complexSurveyViewModel.clearSelectedHistory() }
                            )
                        } else if (selectedFeedbackFile != null && currentScreen == "tutorFeedback") {
                            FeedbackDetailScreen(
                                viewModel = tutorFeedbackViewModel,
                                onBack = { tutorFeedbackViewModel.clearSelectedHistory() }
                            )
                        } else {
                            when (currentScreen) {
                                "home" -> {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("Select a command from the list", color = Color.Gray)
                                    }
                                }
                                "send" -> SendMessageScreen(viewModel = sendMessageViewModel, onBack = { currentScreen = "home" })
                                "giveRole" -> AssignRoleScreen(viewModel = assignRoleViewModel, onBack = { currentScreen = "home" })
                                "clearMessages" -> ClearMessagesScreen(viewModel = clearMessagesViewModel, onBack = { currentScreen = "home" })
                                "settings" -> SettingsScreen(viewModel = settingsViewModel, onBack = { currentScreen = "home" })
                                "attendance" -> {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("Select a history file to see details", color = Color.Gray)
                                    }
                                }
                                "tutorFeedback" -> {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("Select a feedback file to see details", color = Color.Gray)
                                    }
                                }
                                "simpleSurvey" -> {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("Select a survey file to see details", color = Color.Gray)
                                    }
                                }
                                "complexSurvey" -> {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("Select a survey file to see details", color = Color.Gray)
                                    }
                                }
                                "contentQuestions" -> {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("Select a quiz to see details", color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                when (currentScreen) {
                    "home" -> {
                        HomeScreen(
                            listState = listState,
                            headerUiState = headerUiState.value,
                            headerViewModel = headerViewModel,
                            onSettings = { currentScreen = "settings" },
                            discordBotRepository = discordBotRepository,
                            scope = scope,
                            onPing = { response ->
                                pingResponse = response
                                showPingDialog = true
                            },
                            onNavigate = { currentScreen = it }
                        )
                    }
                    "send" -> SendMessageScreen(viewModel = sendMessageViewModel, onBack = { currentScreen = "home" })
                    "giveRole" -> AssignRoleScreen(viewModel = assignRoleViewModel, onBack = { currentScreen = "home" })
                    "clearMessages" -> ClearMessagesScreen(viewModel = clearMessagesViewModel, onBack = { currentScreen = "home" })
                    "settings" -> SettingsScreen(viewModel = settingsViewModel, onBack = { currentScreen = "home" })
                    "attendance" -> AttendanceScreen(viewModel = attendanceViewModel, onBack = { currentScreen = "home" })
                    "tutorFeedback" -> TutorFeedbackScreen(viewModel = tutorFeedbackViewModel, onBack = { currentScreen = "home" })
                    "simpleSurvey" -> SurveyScreen(viewModel = simpleSurveyViewModel, isSimple = true, onBack = { currentScreen = "home" })
                    "complexSurvey" -> SurveyScreen(viewModel = complexSurveyViewModel, isSimple = false, onBack = { currentScreen = "home" })
                    "contentQuestions" -> {
                        ContentQuestionsScreen(
                            viewModel = contentQuestionsViewModel,
                            onBack = { currentScreen = "home" },
                            onSelectPdf = { onFileSelected ->
                                pendingFileCallback = onFileSelected
                                pdfPickerLauncher.launch()
                            }
                        )
                    }
                }
            }

            if (showPingDialog) {
                PingResultDialog(
                    response = pingResponse,
                    onOk = { showPingDialog = false }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    listState: androidx.compose.foundation.lazy.LazyListState,
    headerUiState: HeaderUiState,
    headerViewModel: HeaderViewModel,
    onSettings: () -> Unit,
    discordBotRepository: org.example.project.data.DiscordBotRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    onPing: (PingResponse?) -> Unit,
    onNavigate: (String) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().safeContentPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Header(
                modifier = Modifier.fillMaxWidth(),
                state = headerUiState,
                onStartStopClicked = { headerViewModel.onStartStopClicked() },
                onSettings = onSettings,
                onClearData = { headerViewModel.onClearDataClicked() },
                onDismissClearData = { headerViewModel.onDismissClearDataDialog() },
                onConfirmClearData = { headerViewModel.onConfirmClearData() },
                onClearActionMessage = { headerViewModel.clearActionMessage() }
            )
        }
        item {
            CommandsList(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                onPing = {
                    scope.launch {
                        val response = discordBotRepository.ping()
                        onPing(if (response is RepositoryResponse.Success) response.data else null)
                    }
                },
                onSendMessage = { onNavigate("send") },
                onGiveRole = { onNavigate("giveRole") },
                onClearMessages = { onNavigate("clearMessages") },
                onAttendance = { onNavigate("attendance") },
                onTutorFeedback = { onNavigate("tutorFeedback") },
                onSimpleSurvey = { onNavigate("simpleSurvey") },
                onComplexSurvey = { onNavigate("complexSurvey") },
                onContentQuestions = { onNavigate("contentQuestions") }
            )
        }
    }
}
