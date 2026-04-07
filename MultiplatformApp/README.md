# InsightEdu Multiplatform

InsightEdu is a Kotlin Multiplatform application (Android & iOS) designed to manage Discord bots in an educational context. 
Built with Compose Multiplatform, it provides a centralized interface for educators to interact with students, 
manage course-related data, and leverage AI for educational content generation and analysis.

## 🚀 Features

- **Discord Bot Management**: Monitor bot status, start/stop the bot, and manage Discord bot tokens.
- **Communication**: Send messages to specific members.
- **Role & Member Management**: Fetch the member list, manage roles (assign/remove), and retrieve server roles.
- **Channel Administration**: List available channels and perform bulk message deletion.
- **Attendance Tracking**: Manage student attendance records and view history.
- **Surveys & Feedback**: Create simple (reaction-based) or complex (multi-question) surveys and manage tutor feedback sessions.
- **AI-Powered Q&A**: Generate question-and-answer pairs automatically from PDF documents using Gemini AI integration.
- **Statistics & Analysis**: Visualize student performance and feedback through charts and AI-driven analysis of gathered data.
- **Cross-Platform**: Unified UI for both Android and iOS devices.

## 🏗️ System Architecture

- **Presentation Layer**: Built with **Compose Multiplatform**, providing a truly unified UI for Android and iOS. Following the **MVVM (Model-View-ViewModel)** pattern, shared **ViewModels** manage screen state and interact with the data layer.
- **Domain/Data Layer**: Centralized logic in `commonMain` handles API communication, data parsing, and business rules.
    - **AppContainer**: Acts as a simple dependency injection point.
    - **DiscordBotRepository**: The primary entry point for all bot-related operations, attendance tracking, and AI features.
    - **ApiClient**: Utilizes **Ktor** for asynchronous network requests to the backend server.
- **Local Persistence**: **Multiplatform Settings** is used for secure and efficient storage of configuration data (API keys, tokens, etc.) across different platforms.
- **External Integrations**:
    - **Discord API**: Communicates via a dedicated backend for server management.
    - **Google Gemini AI**: Leveraged for automated Q&A generation and data analysis.
    - **FileKit**: Provides cross-platform file system access for document processing.

## 📂 Project Structure

- `composeApp/src/commonMain`: Shared code for all platforms (Logic, UI components, ViewModels, API interaction).
- `composeApp/src/androidMain`: Android-specific implementations and resources.
- `composeApp/src/iosMain`: iOS-specific implementations.
- `iosApp`: Entry point and project files for the iOS application.

## 🛠️ Built With

- [Kotlin Multiplatform (KMP)](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Ktor](https://ktor.io/) - For network communication and API interaction.
- [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization) - For JSON handling.
- [Multiplatform Settings](https://github.com/russhwolf/multiplatform-settings) - For local data persistence.
- [FileKit](https://github.com/vinceglb/FileKit) - For cross-platform file picking (PDF support).
- [Material 3](https://m3.material.io/) - Modern UI components.

## ⚙️ Configuration

Before running the app, ensure you configure the following settings within the app's settings screen:
1. **Server URL**: The backend API endpoint.
2. **API Key**: Authentication for the backend services.
3. **Bot Token**: Your Discord Bot token (Production/Development).
4. **Gemini API Key**: Required for AI Q&A generation features. [Google AI Studio](https://aistudio.google.com/api-keys)

## 🏗️ Build and Run

### Android
To build and run the development version of the Android app:
- Use the run configuration from your IDE.

### iOS
To build and run the development version of the iOS app:
- Open the `iosApp` directory in Xcode and run it from there.
- Or use the run configuration directly from your IDE if configured.

### Local Testing (Development)
The app uses a default local server URL `http://127.0.0.1:5000` for testing purposes:
- **iOS**: This works "out of the box" when running on the simulator.
- **Android**: `127.0.0.1` refers to the emulator itself, not your host machine. To test with a local backend on Android:
  - Use [ngrok](https://ngrok.com/) to expose your local server.
  - Run a command like `ngrok http 5000` and use the provided HTTPS URL in the app's settings. This also allows testing on **physical devices**,
  as they can reach the backend running on your machine through the ngrok tunnel.
  - Alternatively, use your machine's local IP address (e.g., `10.0.2.2` for the default Android emulator to reach the host).

---
