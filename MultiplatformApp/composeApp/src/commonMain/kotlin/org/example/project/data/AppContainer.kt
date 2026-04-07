package org.example.project.data

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.example.project.api.ApiClient

interface AppContainer {
    val discordBotRepository: DiscordBotRepository
}

class DefaultAppContainer() : AppContainer {
    private val client = HttpClient {
        expectSuccess = false
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    explicitNulls = false
                }
            )
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000  // 2 minutes
            connectTimeoutMillis = 30_000   // 30 seconds
            socketTimeoutMillis = 120_000   // 2 minutes
        }
    }
    private val botApiService = ApiClient(client)
    override val discordBotRepository: DiscordBotRepository by lazy {
        DiscordBotRepository(botApiService)
    }
}