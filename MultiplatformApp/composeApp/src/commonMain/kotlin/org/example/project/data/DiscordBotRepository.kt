package org.example.project.data

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.encodeURLQueryComponent
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.example.project.api.ApiClient
import org.example.project.getPlatform
import org.example.project.model.AttendanceContentResponse
import org.example.project.model.ChannelDto
import org.example.project.model.ChannelsResponse
import org.example.project.model.FeedbackContentResponse
import org.example.project.model.FilesResponse
import org.example.project.model.MemberDto
import org.example.project.model.MembersResponse
import org.example.project.model.PingResponse
import org.example.project.model.RoleDto
import org.example.project.model.RolesResponse
import org.example.project.model.Settings
import org.example.project.model.SettingsResponse
import org.example.project.model.SurveyContentResponse
import org.example.project.model.GenerateQAResponse
import org.example.project.model.QAPair
import org.example.project.model.QAStatisticsResponse
import org.example.project.model.AiFeedbackAnalysisResponse
import org.example.project.model.channelsList
import org.example.project.model.membersList
import org.example.project.model.rolesList
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class BotStatus(
    val serverUrl: String = "",
    val refreshCounter: Int = 0,
    val isServerOnline: Boolean = false,
    val isBotRunning: Boolean = false,
    val errorMessage: String? = null,

    val members: List<MemberDto> = emptyList(),
    val roles: List<RoleDto> = emptyList(),
    val channels: List<ChannelDto> = emptyList(),
)

sealed class RepositoryResponse<out T> {
    abstract val message: String
    data class Success<out T>(val data: T, override val message: String) : RepositoryResponse<T>()
    data class Error(
        override val message: String,
        val isNetworkError: Boolean = false
    ) : RepositoryResponse<Nothing>()
}

class DiscordBotRepository(private val apiClient: ApiClient) {
    private val _botStatus = MutableStateFlow(BotStatus())
    val botStatus = _botStatus.asStateFlow()

    fun getInitialSettings(): Settings {
        _botStatus.update { it.copy(serverUrl = LocalStorage.getServerUrl()) }
        return Settings(
            serverUrl = LocalStorage.getServerUrl(),
            apiKey = LocalStorage.getApiKey(),
            developerMode = LocalStorage.getDeveloperMode(),
            token = LocalStorage.getToken(),
            devToken = LocalStorage.getDevToken(),
            geminiApiKey = LocalStorage.getGeminiApiKey(),
            groups = LocalStorage.getGroups(),
        )
    }

    fun updateSettings(newUrl: String, newKey: String) {
        LocalStorage.save(newUrl, newKey)
        apiClient.setServerUrl(newUrl)
        apiClient.setApiKey(newKey)
        _botStatus.update { it.copy(serverUrl = newUrl) }
    }

    suspend fun refreshStatus() {
        val serverStatus = checkServerStatus()
        if (serverStatus !is RepositoryResponse.Success) {
            _botStatus.update {
                it.copy(
                    isServerOnline = false,
                    isBotRunning = false,
                    errorMessage = serverStatus.message,
                    refreshCounter = it.refreshCounter + 1,
                )
            }
            return
        }

        val botStatus = checkBotStatus()
        val isBotRunning = botStatus is RepositoryResponse.Success

        if (isBotRunning) {
            coroutineScope {
                val membersDef = async { fetchMembers() }
                val rolesDef = async { fetchRoles() }
                val channelsDef = async { fetchChannels() }

                val membersRes = membersDef.await()
                val rolesRes = rolesDef.await()
                val channelsRes = channelsDef.await()

                _botStatus.update {
                    it.copy(
                        isServerOnline = true,
                        isBotRunning = true,
                        members = if (membersRes is RepositoryResponse.Success) membersRes.data.membersList else emptyList(),
                        roles = if (rolesRes is RepositoryResponse.Success) rolesRes.data.rolesList else emptyList(),
                        channels = if (channelsRes is RepositoryResponse.Success) channelsRes.data.channelsList else emptyList(),
                        refreshCounter = it.refreshCounter + 1,
                        errorMessage = null,
                    )
                }
            }
        } else {
            _botStatus.update {
                it.copy(
                    isServerOnline = true,
                    isBotRunning = false,
                    errorMessage = botStatus.message,
                    members = emptyList(),
                    roles = emptyList(),
                    channels = emptyList(),
                )
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun logFailure(context: String, t: Throwable) {
        val platform = try { getPlatform().name } catch (_: Throwable) { "unknown" }
        val timeStamp = Clock.System.now().toString()
        val errorMessage = t.message ?: "Unknown error"
        val cause = t.cause?.let { " | Cause: ${it::class.simpleName}: ${it.message}" } ?: ""
        println("❌ [$platform][$timeStamp][DiscordBotRepository] $context FAILED ❌")
        println("   Type: ${t::class.simpleName}")
        println("   Message: $errorMessage$cause")
    }

    @OptIn(ExperimentalTime::class)
    private fun logSuccess(context: String) {
        val platform = try { getPlatform().name } catch (_: Throwable) { "unknown" }
        val timeStamp = Clock.System.now().toString()
        println("✅ [$platform][$timeStamp][DiscordBotRepository] $context SUCCEEDED ✅")
    }

    private fun isServerOnline(): Boolean {
        return _botStatus.value.isServerOnline
    }

    private suspend fun <T> safeCall(
        context: String,
        successMessage: String,
        errorMessage: String,
        request: suspend () -> HttpResponse,
        map: suspend (HttpResponse) -> T,
        requiresServerOnline: Boolean = true
    ): RepositoryResponse<T> {
        if (requiresServerOnline && !isServerOnline()) {
            logFailure(context, IllegalStateException("Server is offline."))
            return RepositoryResponse.Error(errorMessage, isNetworkError = true)
        }

        return try {
            val resp = request()
            if (resp.status.isSuccess()) {
                val data = map(resp)
                logSuccess(context)
                RepositoryResponse.Success(data, successMessage)
            } else {
                val errorBody = try { resp.bodyAsText() } catch (_: Exception) { "" }
                val detail = if (errorBody.isNotEmpty()) ": $errorBody" else ""
                val fullErrorMsg = "HTTP ${resp.status.value} ${resp.status.description}$detail"
                val exception = IllegalStateException(fullErrorMsg)
                logFailure(context, exception)
                RepositoryResponse.Error(errorMessage, isNetworkError = false)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logFailure(context, e)

            val isNetworkError = when (e) {
                is io.ktor.client.network.sockets.ConnectTimeoutException,
                is io.ktor.client.plugins.HttpRequestTimeoutException -> true
                else -> false
            }

            var newErrorMessage = when (e) {
                is io.ktor.client.network.sockets.ConnectTimeoutException -> "Connection timeout. Please check your server URL.\n $errorMessage"
                is io.ktor.client.plugins.HttpRequestTimeoutException -> "Request timeout. The server took too long to respond.\n $errorMessage"
                is kotlinx.serialization.SerializationException -> "Failed to parse server response.\n $errorMessage"
                else -> "An unexpected error occurred.\n $errorMessage"
            }
            if (context == "checkServerStatus()") newErrorMessage = "Server is offline."
            RepositoryResponse.Error(newErrorMessage, isNetworkError = isNetworkError)
        }
    }

    // ----------------------------Api Calls----------------------------

    suspend fun checkServerStatus(): RepositoryResponse<Boolean> =
        safeCall(
            context = "checkServerStatus()",
            successMessage = "Server is online.",
            errorMessage = "Server is offline.",
            request = { apiClient.checkServerStatus() },
            map = { true },
            requiresServerOnline = false
        )

    suspend fun checkBotStatus(): RepositoryResponse<Boolean> =
        safeCall(
            context = "checkBotStatus()",
            successMessage = "Bot is running.",
            errorMessage = "Bot is not running.",
            request = { apiClient.checkBotStatus() },
            map = { true }
        )

    suspend fun sendMessage(memberID: String, message: String): RepositoryResponse<Boolean> =
        safeCall(
            context = "sendMessage()",
            successMessage = "Message sent successfully.",
            errorMessage = "Failed to send message.",
            request = { apiClient.sendHello(memberID, message) },
            map = { true }
        )

    suspend fun sendStart(): RepositoryResponse<Boolean> =
        safeCall(
            context = "startBot()",
            successMessage = "Bot started successfully.",
            errorMessage = "Failed to start bot.",
            request = { apiClient.start() },
            map = { true }
        )

    suspend fun stopBot(): RepositoryResponse<Boolean> =
        safeCall(
            context = "stopBot()",
            successMessage = "Bot stopped successfully.",
            errorMessage = "Failed to stop bot.",
            request = { apiClient.stop() },
            map = { true }
        )

    suspend fun fetchMembers(): RepositoryResponse<MembersResponse> =
        safeCall(
            context = "fetchMembers()",
            successMessage = "Members fetched successfully.",
            errorMessage = "Failed to fetch members.",
            request = { apiClient.fetchMembers() },
            map = { it.body<MembersResponse>() }
        )

    suspend fun ping(): RepositoryResponse<PingResponse> =
        safeCall(
            context = "ping()",
            successMessage = "Ping successful.",
            errorMessage = "Ping failed.",
            request = { apiClient.ping() },
            map = { it.body<PingResponse>() },
            requiresServerOnline = false
        )

    suspend fun fetchRoles(): RepositoryResponse<RolesResponse> =
        safeCall(
            context = "fetchRoles()",
            successMessage = "Roles fetched successfully.",
            errorMessage = "Failed to fetch roles.",
            request = { apiClient.fetchRoles() },
            map = { it.body<RolesResponse>() }
        )

    suspend fun giveMemberRole(userId: String, roleId: String): RepositoryResponse<Boolean> =
        safeCall(
            context = "giveMemberRole()",
            successMessage = "Role assigned successfully.",
            errorMessage = "Failed to assign role.",
            request = { apiClient.giveMemberRole(userId, roleId) },
            map = { true }
        )

    suspend fun removeMemberRole(userId: String, roleId: String): RepositoryResponse<Boolean> =
        safeCall(
            context = "removeMemberRole()",
            successMessage = "Role removed successfully.",
            errorMessage = "Failed to remove role.",
            request = { apiClient.removeMemberRole(userId, roleId) },
            map = { true }
        )

    suspend fun clearMessages(channelId: String, limit: Int): RepositoryResponse<Boolean> =
        safeCall(
            context = "clearMessages()",
            successMessage = "Messages cleared successfully.",
            errorMessage = "Failed to clear messages.",
            request = { apiClient.clearMessages(channelId, limit) },
            map = { true }
        )

    suspend fun fetchChannels(): RepositoryResponse<ChannelsResponse> =
        safeCall(
            context = "fetchChannels()",
            successMessage = "Channels fetched successfully.",
            errorMessage = "Failed to fetch channels.",
            request = { apiClient.fetchChannels() },
            map = { it.body<ChannelsResponse>() }
        )

    suspend fun fetchAttendanceFiles(): RepositoryResponse<FilesResponse> =
        safeCall(
            context = "fetchAttendanceFiles()",
            successMessage = "Attendance files fetched successfully.",
            errorMessage = "Failed to fetch attendance files.",
            request = { apiClient.fetchAttendanceFiles() },
            map = { it.body<FilesResponse>() }
        )

    suspend fun fetchAttendanceContent(fileName: String): RepositoryResponse<AttendanceContentResponse> =
        safeCall(
            context = "fetchAttendanceContent()",
            successMessage = "Attendance content fetched successfully.",
            errorMessage = "Failed to fetch attendance content.",
            request = { apiClient.fetchAttendanceContent(fileName) },
            map = { it.body<AttendanceContentResponse>() }
        )

    suspend fun manageAttendance(groupId: String, targetUserId: String, status: String, code: String): RepositoryResponse<Boolean> =
        safeCall(
            context = "manageAttendance()",
            successMessage = "Attendance managed successfully.",
            errorMessage = "Failed to manage attendance.",
            request = { apiClient.manageAttendance(groupId, targetUserId, status, code) },
            map = { true }
        )

    suspend fun fetchSettings(): RepositoryResponse<SettingsResponse> =
        safeCall(
            context = "fetchSettings()",
            successMessage = "Settings fetched successfully.",
            errorMessage = "Failed to fetch settings.",
            request = { apiClient.fetchSettings() },
            map = { it.body<SettingsResponse>() }
        )

    suspend fun updateDevelopmentMode(isDevelopmentMode: Boolean): RepositoryResponse<Boolean> =
        safeCall(
            context = "updateDevelopmentMode()",
            successMessage = "Updated development mode successfully.",
            errorMessage = "Failed to update development mode.",
            request = { apiClient.updateDevelopmentMode(isDevelopmentMode) },
            map = { true }
        )

    suspend fun updateBotToken(isDevelopmentMode: Boolean, token: String): RepositoryResponse<Boolean> {
        val developmentMode = if (isDevelopmentMode) "dev_token" else "token"
        val token = token.encodeURLQueryComponent()
        return safeCall(
            context = "updateBotToken()",
            successMessage = "Updated bot token successfully.",
            errorMessage = "Failed to update bot token.",
            request = { apiClient.updateBotToken(developmentMode, token) },
            map = { true }
        )
    }

    suspend fun updateGeminiApiKey(geminiApiKey: String): RepositoryResponse<Boolean> {
        return safeCall(
            context = "updateGeminiApiKey()",
            successMessage = "Updated Gemini API key successfully.",
            errorMessage = "Failed to update Gemini API key.",
            request = { apiClient.updateGeminiApiKey(geminiApiKey) },
            map = { true }
        )
    }

    suspend fun updateGroups(groups: List<String>): RepositoryResponse<Boolean> =
        safeCall(
            context = "updateGroups()",
            successMessage = "Groups updated successfully.",
            errorMessage = "Failed to update groups.",
            request = { apiClient.updateGroups(groups) },
            map = { true }
        )

    suspend fun fetchFeedbackFiles(): RepositoryResponse<FilesResponse> =
        safeCall(
            context = "fetchFeedbackFiles()",
            successMessage = "Feedback files fetched successfully.",
            errorMessage = "Failed to fetch feedback files.",
            request = { apiClient.fetchFeedbackFiles() },
            map = { it.body<FilesResponse>() }
        )

    suspend fun fetchFeedbackContent(fileName: String): RepositoryResponse<FeedbackContentResponse> =
        safeCall(
            context = "fetchFeedbackContent()",
            successMessage = "Feedback content fetched successfully.",
            errorMessage = "Failed to fetch feedback content.",
            request = { apiClient.fetchFeedbackContent(fileName) },
            map = { it.body<FeedbackContentResponse>() }
        )

    suspend fun manageFeedback(groupId: String, channelId: String, duration: Int): RepositoryResponse<Boolean> =
        safeCall(
            context = "manageFeedback()",
            successMessage = "Feedback managed successfully.",
            errorMessage = "Failed to manage feedback.",
            request = { apiClient.manageFeedback(groupId, channelId, duration) },
            map = { true }
        )

    suspend fun createSimpleSurvey(message: String, mainTopic: String, channelId: String, buttonType: String, duration: Int): RepositoryResponse<Boolean> =
        safeCall(
            context = "createSimpleSurvey()",
            successMessage = "Survey created successfully.",
            errorMessage = "Failed to create survey.",
            request = { apiClient.createSimpleSurvey(message, mainTopic, channelId, buttonType, duration) },
            map = { true }
        )

    suspend fun fetchSurveyFiles(): RepositoryResponse<FilesResponse> =
        safeCall(
            context = "fetchSurveyFiles()",
            successMessage = "Survey files fetched successfully.",
            errorMessage = "Failed to fetch survey files.",
            request = { apiClient.fetchSurveyFiles() },
            map = { it.body<FilesResponse>() }
        )

    suspend fun createComplexSurvey(message: String, mainTopic: String, channelId: String, questions: List<Pair<String, String>>, duration: Int): RepositoryResponse<Boolean> =
        safeCall(
            context = "createComplexSurvey",
            successMessage = "Complex survey created",
            errorMessage = "Failed to create complex survey",
            request = { apiClient.createComplexSurvey(message, mainTopic, channelId, questions, duration) },
            map = { true }
        )

    suspend fun fetchSurveyContent(fileName: String): RepositoryResponse<SurveyContentResponse> =
        safeCall(
            context = "fetchSurveyContent()",
            successMessage = "Survey content fetched successfully.",
            errorMessage = "Failed to fetch survey content.",
            request = { apiClient.fetchSurveyContent(fileName) },
            map = { it.body<SurveyContentResponse>() }
        )

    suspend fun generateQA(
        fileName: String,
        fileBytes: ByteArray,
        numQuestions: Int
    ): RepositoryResponse<GenerateQAResponse> =
        safeCall(
            context = "generateQA()",
            successMessage = "Questions & answers generated successfully.",
            errorMessage = "Failed to generate questions & answers.",
            request = { apiClient.generateQA(fileName, fileBytes, numQuestions) },
            map = { it.body<GenerateQAResponse>() }
        )

    suspend fun saveQAPairs(
        qaPairs: List<QAPair>,
        channelId: String,
        duration: Int,
        aiFeedback: Boolean
    ): RepositoryResponse<Boolean> =
        safeCall(
            context = "saveQAPairs()",
            successMessage = "Q&A pairs sent to Discord successfully.",
            errorMessage = "Failed to send Q&A pairs.",
            request = { apiClient.saveQAPairs(qaPairs, channelId, duration, aiFeedback) },
            map = { true }
        )

    suspend fun fetchQAStatistics(aiFeedbackOnly: Boolean = true): RepositoryResponse<QAStatisticsResponse> =
        safeCall(
            context = "fetchQAStatistics()",
            successMessage = "QA statistics fetched successfully.",
            errorMessage = "Failed to fetch QA statistics.",
            request = { apiClient.fetchQAStatistics(aiFeedbackOnly) },
            map = { it.body<QAStatisticsResponse>() }
        )

    suspend fun getAiFeedbackAnalysis(
        useStatistics: Boolean,
        channelIds: List<String>,
        startDate: String,
        endDate: String
    ): RepositoryResponse<AiFeedbackAnalysisResponse> =
        safeCall(
            context = "getAiFeedbackAnalysis()",
            successMessage = "AI feedback analysis generated successfully.",
            errorMessage = "Failed to generate AI feedback analysis.",
            request = { apiClient.getAiFeedbackAnalysis(useStatistics, channelIds, startDate, endDate) },
            map = { it.body<AiFeedbackAnalysisResponse>() }
        )

    suspend fun clearAllData(): RepositoryResponse<Boolean> =
        safeCall(
            context = "clearAllData()",
            successMessage = "All data cleared successfully.",
            errorMessage = "Failed to clear all data.",
            request = { apiClient.clearAllData() },
            map = { true }
        )
}
