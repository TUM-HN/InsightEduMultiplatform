package org.example.project.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class PingResponse(
    val message: String,
    val latency: String,
)

// ----------------------------Member----------------------------

@Serializable
data class MemberDto(
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val bot: Boolean,
    val discriminator: String,
    @SerialName("display_name") val displayName: String,
    val id: String,
    @SerialName("joined_at") val joinedAt: String,
    val name: String,
    val roles: List<String> = emptyList(),
    val status: String,
)

@Serializable
data class MembersResponse(
    val data: Map<String, List<MemberDto>>,
    val status: String,
    val message: String? = null,
)

val MembersResponse.membersList: List<MemberDto>
    get() = data.values.flatten()

class Member(
    val avatarUrl: String?,
    val bot: Boolean,
    val discriminator: String,
    val displayName: String,
    val id: String,
    val joinedAt: String,
    val name: String,
    val roles: List<String>,
    val status: String,
) {
    override fun equals(other: Any?): Boolean = other is Member && other.id == this.id
    override fun hashCode(): Int = id.hashCode()
    override fun toString(): String = "Member(id=$id, name=$name, displayName=$displayName)"
}

fun MemberDto.toDomain(): Member = Member(
    avatarUrl, bot, discriminator, displayName, id, joinedAt, name, roles, status
)

// ----------------------------Role----------------------------

@Serializable
data class RoleDto(
    val color: String,
    val id: String,
    val mentionable: Boolean,
    val name: String,
    val permissions: String,
    val position: Int,
)

@Serializable
data class RolesResponse(
    val data: Map<String, List<RoleDto>>? = null,
    val status: String,
    val message: String? = null,
)

val RolesResponse.rolesList: List<RoleDto>
    get() = data?.values?.flatten() ?: emptyList()

class Role(
    val color: String,
    val id: String,
    val mentionable: Boolean,
    val name: String,
    val permissions: String,
    val position: Int,
) {
    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?): Boolean = other is Role && other.id == this.id
}

fun RoleDto.toDomain(): Role = Role(
    color, id, mentionable, name, permissions, position
)

// ----------------------------Channel----------------------------

@Serializable
data class ChannelDto(
    val categoryId: String? = null,
    val id: String,
    val name: String,
    val position: Int,
    val type: String,
)

@Serializable
data class ChannelsResponse(
    val data: Map<String, List<ChannelDto>>? = null,
    val status: String,
    val message: String? = null,
)

val ChannelsResponse.channelsList: List<ChannelDto>
    get() = data?.values?.flatten() ?: emptyList()

class Channel(
    val categoryId: String?,
    val id: String,
    val name: String,
    val position: Int,
    val type: String,
) {
    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?): Boolean = other is Channel && other.id == this.id
}

fun ChannelDto.toDomain(): Channel = Channel(
    categoryId, id, name, position, type
)

// ----------------------------Attendance----------------------------

@Serializable
data class FileDto(
    val created: String,
    val modified: String,
    val name: String,
    val size: Int,
) {
    val id: String
        get() = name
}

@Serializable
data class FilesResponse(
    val files: List<FileDto>,
)

@Serializable
data class AttendanceEntryDto(
    @SerialName("Attendance")
    val attendance: String,
)

@Serializable
data class AttendanceContentResponse(
    val content: List<AttendanceEntryDto>,
)

// ----------------------------Settings----------------------------

@Serializable
data class BotDto(
    val token: String,
    @SerialName("dev_token") val devToken: String,
    @SerialName("development_mode") val developmentMode: Boolean,
    @SerialName("gemini_api_key") val geminiApiKey: String? = null
)

@Serializable
data class SettingsDto(
    val bot: BotDto,
    val groups: List<String>,
    @SerialName("access_roles") val accessRoles: List<RoleDto>
)

@Serializable
data class GroupModel(
    val name: String,
    val isValid: Boolean,
    val attendanceActive: Boolean = false,
    val attendanceCount: String? = null,
    val attendanceCode: String = "",
)

data class Settings(
    val serverUrl: String,
    val apiKey: String,
    val developerMode: Boolean,
    val token: String,
    val devToken: String,
    val geminiApiKey: String,
    val groups: List<GroupModel>,
)

@Serializable
data class SettingsResponse(
    val data: SettingsDto? = null,
    val status: String,
    val message: String? = null,
)

// ----------------------------Feedback----------------------------

@Serializable
data class FeedbackEntryDto(
    @SerialName("Feedback")
    val feedback: String,
    @SerialName("Name")
    val name: String,
    @SerialName("Open Feedback")
    val openFeedback: String? = null,
)

@Serializable
data class FeedbackContentResponse(
    val content: List<FeedbackEntryDto>,
)

// ----------------------------SimpleSurvey----------------------------

@Serializable
data class SurveyContentResponse(
    val content: List<SurveyEntryDto>,
)

@Serializable(with = SurveyEntrySerializer::class)
data class SurveyEntryDto(
    @SerialName("Name")
    val name: String,
    val additionalData: Map<String, String> = emptyMap()
) {
    val responsePairs: List<Pair<String, String>>
        get() = additionalData.filterKeys { it != "Name" }
            .map { it.key to it.value }
            .sortedBy { it.first }

    val responsePair: Pair<String, String>?
        get() = additionalData.entries.firstOrNull { it.key != "Name" }?.let { it.key to it.value }
}

object SurveyEntrySerializer : KSerializer<SurveyEntryDto> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SurveyEntryDto")

    override fun deserialize(decoder: Decoder): SurveyEntryDto {
        val jsonDecoder = decoder as JsonDecoder
        val jsonObject = jsonDecoder.decodeJsonElement() as JsonObject
        
        val name = jsonObject["Name"]?.jsonPrimitive?.content ?: ""
        val additionalData = jsonObject
            .filterKeys { it != "Name" }
            .mapValues { (_, value) -> value.jsonPrimitive.content }
        
        return SurveyEntryDto(name, additionalData)
    }

    override fun serialize(encoder: Encoder, value: SurveyEntryDto) {
        val jsonEncoder = encoder as JsonEncoder
        val jsonObject = buildJsonObject {
            put("Name", JsonPrimitive(value.name))
            value.additionalData.forEach { (key, v) ->
                put(key, JsonPrimitive(v))
            }
        }
        jsonEncoder.encodeJsonElement(jsonObject)
    }
}

// ----------------------------PDFQuestions----------------------------

@Serializable
data class SaveQARequest(
    @SerialName("qa_pairs") val qaPairs: List<QAPair>,
    @SerialName("channel_id") val channelId: String,
    val duration: Int,
    val aiFeedback: Boolean,
)

@Serializable
data class QAPair(
    val question: String,
    val answer: String
)

@Serializable
data class GenerateQAResponse(
    val success: Boolean,
    @SerialName("num_questions")
    val numQuestions: Int,
    @SerialName("qa_pairs")
    val qaPairs: List<QAPair>,
    @SerialName("pdf_text_length")
    val pdfTextLength: Int
)

// ----------------------------QA Statistics----------------------------

@Serializable
data class QAStatisticsResponse(
    val success: Boolean,
    val statistics: QAStatistics
)

@Serializable
data class QAStatistics(
    @SerialName("total_quizzes") val totalQuizzes: Int,
    @SerialName("total_responses") val totalResponses: Int,
    @SerialName("quizzes_with_ai_feedback") val quizzesWithAiFeedback: Int,
    @SerialName("quizzes_without_ai_feedback") val quizzesWithoutAiFeedback: Int,
    @SerialName("average_responses_per_quiz") val averageResponsesPerQuiz: Double,
    @SerialName("ai_feedback_stats") val aiFeedbackStats: AIFeedbackStats? = null,
    val quizzes: List<QuizStatistics> = emptyList()
)

@Serializable
data class AIFeedbackStats(
    @SerialName("average_correct_rate") val averageCorrectRate: Double,
    @SerialName("total_evaluated_answers") val totalEvaluatedAnswers: Int,
    @SerialName("score_distribution") val scoreDistribution: ScoreDistribution
)

@Serializable
data class ScoreDistribution(
    val correct: Int = 0,
    @SerialName("mostly correct") val mostlyCorrect: Int = 0,
    @SerialName("mostly incorrect") val mostlyIncorrect: Int = 0,
    val incorrect: Int = 0
)

@Serializable
data class QuizStatistics(
    val timestamp: String,
    @SerialName("ai_feedback_enabled") val aiFeedbackEnabled: Boolean,
    @SerialName("total_responses") val totalResponses: Int,
    @SerialName("total_questions") val totalQuestions: Int,
    val filename: String,
    val questions: List<QuestionStatistics> = emptyList()
)

@Serializable
data class QuestionStatistics(
    val question: String,
    @SerialName("correct_answer") val correctAnswer: String,
    @SerialName("total_answers") val totalAnswers: Int,
    @SerialName("score_distribution") val scoreDistribution: ScoreDistribution
)

// ----------------------------AI Feedback Analysis----------------------------

@Serializable
data class AiFeedbackAnalysisRequest(
    @SerialName("use_statistics") val useStatistics: Boolean,
    @SerialName("channel_ids") val channelIds: List<String>,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String
)

@Serializable
data class AiFeedbackAnalysisResponse(
    val success: Boolean = false,
    val analysis: String = "",
    val message: String? = null
)
