package org.example.project.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.serialization.json.Json
import org.example.project.model.GroupModel

object LocalStorage {
    private const val KEY_SERVER_URL = "server_url"
    private const val KEY_API_KEY = "api_key"
    private const val GROUPS_KEY = "groups_key"
    private const val KEY_DEVELOPER_MODE = "developer_mode"
    private const val KEY_TOKEN = "token"
    private const val KEY_DEV_TOKEN = "dev_token"
    private const val KEY_GEMINI_API_KEY = "gemini_api_key"
    private val settings: Settings = Settings()

    private const val DEFAULT_URL = "http://127.0.0.1:5000"
    private const val DEFAULT_KEY = "025002"
    
    fun getServerUrl(): String {
        return settings.getString(KEY_SERVER_URL, DEFAULT_URL)
    }

    fun getApiKey(): String {
        return settings.getString(KEY_API_KEY, DEFAULT_KEY)
    }

    fun getDeveloperMode(): Boolean {
        return settings.getBoolean(KEY_DEVELOPER_MODE, false)
    }

    fun getToken(): String {
        return settings.getString(KEY_TOKEN, "")
    }

    fun getDevToken(): String {
        return settings.getString(KEY_DEV_TOKEN, "")
    }

    fun getGeminiApiKey(): String {
        return settings.getString(KEY_GEMINI_API_KEY, "")
    }
    
    fun getGroups(): List<GroupModel> {
        val json = settings.getStringOrNull(GROUPS_KEY)
        val defaultGroups = List(10) { i ->
            GroupModel(name = "Group ${i + 1}", isValid = true)
        }
        return if (json != null) {
            try {
                Json.decodeFromString<List<GroupModel>>(json)
            } catch (e: Exception) {
                defaultGroups
            }
        } else {
            defaultGroups
        }
    }

    fun getValidGroupsNames(): List<String> {
        return getGroups().filter { it.isValid }.map { it.name }
    }

    fun save(serverUrl: String, apiKey: String) {
        settings[KEY_SERVER_URL] = serverUrl
        settings[KEY_API_KEY] = apiKey
    }

    fun saveBotSettings(developerMode: Boolean, token: String, devToken: String, geminiApiKey: String) {
        settings[KEY_DEVELOPER_MODE] = developerMode
        settings[KEY_TOKEN] = token
        settings[KEY_DEV_TOKEN] = devToken
        settings[KEY_GEMINI_API_KEY] = geminiApiKey
    }
    
    fun saveGroups(groups: List<GroupModel>) {
        settings[GROUPS_KEY] = Json.encodeToString(groups)
    }
}
