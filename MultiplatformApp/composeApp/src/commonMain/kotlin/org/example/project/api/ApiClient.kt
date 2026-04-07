package org.example.project.api

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import org.example.project.data.LocalStorage
import org.example.project.model.AiFeedbackAnalysisRequest
import org.example.project.model.QAPair
import org.example.project.model.SaveQARequest

private object ApiConfig {
    var serverUrl: String = LocalStorage.getServerUrl()
    var apiKey: String = LocalStorage.getApiKey()
}

class ApiClient(private val client: HttpClient) {
    fun setServerUrl(newServerUrl: String) {
        ApiConfig.serverUrl = newServerUrl
    }

    fun setApiKey(newApiKey: String) {
        ApiConfig.apiKey = newApiKey
    }

    suspend fun checkServerStatus(): HttpResponse {
        return client.get(ApiConfig.serverUrl)
    }

    suspend fun checkBotStatus(): HttpResponse {
        return client.get("${ApiConfig.serverUrl}/api/bot-status") {
            parameter("api_key", ApiConfig.apiKey)
        }
    }

    suspend fun sendHello(member: String, message: String): HttpResponse {
        return client.post("${ApiConfig.serverUrl}/api/hello") {
            parameter("api_key", ApiConfig.apiKey)
            parameter("member", member)
            parameter("message", message)
        }
    }

    suspend fun start(): HttpResponse {
        return client.post("${ApiConfig.serverUrl}/api/start-bot") {
            parameter("api_key", ApiConfig.apiKey)
        }
    }

    suspend fun stop(): HttpResponse {
        return client.post("${ApiConfig.serverUrl}/api/stop-bot") {
            parameter("api_key", ApiConfig.apiKey)
        }
    }

    suspend fun fetchMembers(): HttpResponse {
       return client.get("${ApiConfig.serverUrl}/api/members") {
            parameter("api_key", ApiConfig.apiKey)
        }
    }

    suspend fun ping(): HttpResponse {
        return client.get("${ApiConfig.serverUrl}/api/ping") {
            parameter("api_key", ApiConfig.apiKey)
        }
    }

    suspend fun fetchRoles(): HttpResponse {
        return client.get("${ApiConfig.serverUrl}/api/roles") {
            parameter("api_key", ApiConfig.apiKey)
        }
    }

    suspend fun giveMemberRole(userId: String, roleId: String): HttpResponse {
        return client.post("${ApiConfig.serverUrl}/api/give-member-role") {
            parameter("api_key", ApiConfig.apiKey)
            parameter("role_id", roleId)
            parameter("user_id", userId)
        }
    }

    suspend fun removeMemberRole(userId: String, roleId: String): HttpResponse {
        return client.post("${ApiConfig.serverUrl}/api/remove-member-role") {
            parameter("api_key", ApiConfig.apiKey)
            parameter("role_id", roleId)
            parameter("user_id", userId)
        }
    }

    suspend fun clearMessages(channelId: String, limit: Int): HttpResponse {
        return client.post("${ApiConfig.serverUrl}/api/clear") {
            parameter("api_key", ApiConfig.apiKey)
            parameter("channel_id", channelId)
            parameter("limit", limit)
        }
    }

    suspend fun fetchChannels(): HttpResponse {
        return client.get("${ApiConfig.serverUrl}/api/channels") {
            parameter("api_key", ApiConfig.apiKey)
        }
    }

    suspend fun fetchAttendanceFiles(): HttpResponse {
        return client.get("${ApiConfig.serverUrl}/api/data/attendance") {
            parameter("api_key", ApiConfig.apiKey)
        }
    }

    suspend fun fetchAttendanceContent(fileName: String): HttpResponse {
        return client.get("${ApiConfig.serverUrl}/api/data/attendance") {
            parameter("api_key", ApiConfig.apiKey)
            parameter("file", fileName)
        }
    }

    suspend fun manageAttendance(groupId: String, targetUserId: String, status: String, code: String): HttpResponse {
        return client.post("${ApiConfig.serverUrl}/api/attendance") {
            parameter("api_key", ApiConfig.apiKey)
            parameter("status", status)
            parameter("target_user_id", targetUserId)
            parameter("code", code)
            parameter("group_id", groupId.lowercase())
        }
    }

    suspend fun fetchSettings(): HttpResponse {
        return client.get("${ApiConfig.serverUrl}/api/settings") {
            parameter("api_key", ApiConfig.apiKey)
        }
    }

    suspend fun updateDevelopmentMode(isDevelopmentMode: Boolean): HttpResponse {
        return client.post("${ApiConfig.serverUrl}/api/settings/bot/development_mode") {
            parameter("api_key", ApiConfig.apiKey)
            parameter("development_mode", isDevelopmentMode)
        }
    }

    suspend fun updateBotToken(developmentMode: String, token: String): HttpResponse {
        return client.post("${ApiConfig.serverUrl}/api/settings/bot/$developmentMode") {
            parameter("api_key", ApiConfig.apiKey)
            parameter(developmentMode, token)
        }
    }

    suspend fun updateGeminiApiKey(geminiApiKey: String): HttpResponse {
        return client.post("${ApiConfig.serverUrl}/api/settings/bot/gemini_api_key") {
            parameter("api_key", ApiConfig.apiKey)
            parameter("gemini_api_key", geminiApiKey)
        }
    }

    suspend fun updateGroups(groups: List<String>): HttpResponse {
        return client.post("${ApiConfig.serverUrl}/api/settings/groups") {
            parameter("api_key", ApiConfig.apiKey)
            contentType(ContentType.Application.Json)
            setBody(mapOf("groups" to groups))
        }
    }

    suspend fun fetchFeedbackFiles(): HttpResponse {
        return client.get("${ApiConfig.serverUrl}/api/data/feedback") {
            parameter("api_key", ApiConfig.apiKey)
        }
    }

    suspend fun fetchFeedbackContent(fileName: String): HttpResponse {
        return client.get("${ApiConfig.serverUrl}/api/data/feedback") {
            parameter("api_key", ApiConfig.apiKey)
            parameter("file", fileName)
        }
    }

    suspend fun manageFeedback(groupId: String, channelId: String, duration: Int): HttpResponse {
        return client.post("${ApiConfig.serverUrl}/api/tutor-session-feedback") {
            parameter("api_key", ApiConfig.apiKey)
            parameter("group_id", groupId.lowercase())
            parameter("channel_id", channelId)
            parameter("duration", duration)
        }
    }

    suspend fun createSimpleSurvey(message: String, mainTopic: String, channelId: String, buttonType: String, duration: Int): HttpResponse {
        return client.post("${ApiConfig.serverUrl}/api/create-simple-survey") {
            parameter("api_key", ApiConfig.apiKey)
            parameter("message", message)
            parameter("main_topic", mainTopic)
            parameter("channel_id", channelId)
            parameter("button_type", buttonType.lowercase())
            parameter("duration", duration)
        }
    }

    suspend fun createComplexSurvey(message: String, mainTopic: String, channelId: String, questions: List<Pair<String, String>>, duration: Int): HttpResponse {
        return client.post("${ApiConfig.serverUrl}/api/create-complex-survey") {
            parameter("api_key", ApiConfig.apiKey)
            parameter("message", message)
            parameter("main_topic", mainTopic)
            parameter("channel_id", channelId)
            parameter("duration", duration)
            questions.forEachIndexed { index, (questionText, buttonType) ->
                val questionNumber = index + 1
                parameter("question_$questionNumber", questionText)
                parameter("button_$questionNumber", buttonType)
            }
        }
    }


    suspend fun fetchSurveyFiles(): HttpResponse {
        return client.get("${ApiConfig.serverUrl}/api/data/surveys") {
            parameter("api_key", ApiConfig.apiKey)
        }
    }

    suspend fun fetchSurveyContent(fileName: String): HttpResponse {
        return client.get("${ApiConfig.serverUrl}/api/data/surveys") {
            parameter("api_key", ApiConfig.apiKey)
            parameter("file", fileName)
        }
    }

    suspend fun generateQA(fileName: String, fileBytes: ByteArray, numQuestions: Int): HttpResponse {
        return client.submitFormWithBinaryData(
            url = "${ApiConfig.serverUrl}/api/ai/generate-qa?api_key=${ApiConfig.apiKey}",
            formData = formData {
                append("pdf", fileBytes, Headers.build {
                    append(HttpHeaders.ContentType, "application/pdf")
                    append(HttpHeaders.ContentDisposition, "form-data; name=\"pdf\"; filename=\"$fileName\"")
                })
                append("num_questions", numQuestions.toString())
            }
        ) {
            accept(ContentType.Application.Json)
        }
    }

    suspend fun saveQAPairs(qaPairs: List<QAPair>, channelId: String, duration: Int, aiFeedback: Boolean): HttpResponse {
        return client.post("${ApiConfig.serverUrl}/api/ai/save-qa") {
            parameter("api_key", ApiConfig.apiKey)
            contentType(ContentType.Application.Json)
            setBody(SaveQARequest(qaPairs, channelId, duration, aiFeedback))
        }
    }

    suspend fun fetchQAStatistics(aiFeedbackOnly: Boolean = true): HttpResponse {
        return client.get("${ApiConfig.serverUrl}/api/ai/qa-statistics") {
            parameter("api_key", ApiConfig.apiKey)
            parameter("ai_feedback", aiFeedbackOnly)
        }
    }

    suspend fun getAiFeedbackAnalysis(
        useStatistics: Boolean,
        channelIds: List<String>,
        startDate: String,
        endDate: String
    ): HttpResponse {
        return client.post("${ApiConfig.serverUrl}/api/ai/feedback-analysis") {
            parameter("api_key", ApiConfig.apiKey)
            contentType(ContentType.Application.Json)
            setBody(AiFeedbackAnalysisRequest(useStatistics, channelIds, startDate, endDate))
        }
    }

    suspend fun clearAllData(): HttpResponse {
        return client.delete("${ApiConfig.serverUrl}/api/data/clear") {
            parameter("api_key", ApiConfig.apiKey)
        }
    }
}
