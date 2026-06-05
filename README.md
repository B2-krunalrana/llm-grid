<div align="center">
<img width="1200" height="475" alt="LLM Grid Banner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# LLM Grid - AI Chat Application

A modern Android application that provides an interactive chat interface powered by AI models through OpenRouter API. This app features local storage, offline capabilities, and a clean, intuitive UI built with Jetpack Compose.

## Features

- **Interactive Chat Interface** - Real-time chat with AI models
- **Local Data Storage** - SQLite database for chat history and messages
- **Model Caching** - Efficient model management and caching
- **Security** - Secure key management for API credentials
- **Modern UI** - Built with Jetpack Compose and Material Design
- **OpenRouter Integration** - Access to multiple AI models via OpenRouter API

## Folder Structure

```
llm-grid/
├── app/                              # Main Android application module
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt                    # Main entry point
│   │   │   │   ├── data/
│   │   │   │   │   ├── ChatRepository.kt              # Data repository
│   │   │   │   │   ├── local/
│   │   │   │   │   │   ├── AppDatabase.kt             # Room database setup
│   │   │   │   │   │   ├── ChatDao.kt                 # Chat data access
│   │   │   │   │   │   ├── ChatMessage.kt             # Message entity
│   │   │   │   │   │   ├── ChatSession.kt             # Session entity
│   │   │   │   │   │   ├── KeyManager.kt              # API key management
│   │   │   │   │   │   └── ModelCache.kt              # Model caching
│   │   │   │   │   └── network/
│   │   │   │   │       ├── OpenRouterApi.kt           # OpenRouter API client
│   │   │   │   │       └── RetrofitClient.kt          # Retrofit configuration
│   │   │   │   ├── ui/
│   │   │   │   │   ├── ChatScreen.kt                  # Chat UI screen
│   │   │   │   │   ├── ChatViewModel.kt               # UI state management
│   │   │   │   │   └── theme/
│   │   │   │   │       ├── Color.kt                   # Color palette
│   │   │   │   │       ├── Theme.kt                   # Theme configuration
│   │   │   │   │       └── Type.kt                    # Typography
│   │   │   ├── res/                                   # Resources (drawables, values)
│   │   │   └── AndroidManifest.xml                    # App manifest
│   │   ├── test/                                      # Unit tests
│   │   └── androidTest/                               # Instrumented tests
│   ├── build.gradle.kts                               # Module build configuration
│   └── proguard-rules.pro                             # ProGuard rules
├── gradle/                           # Gradle configuration
│   └── libs.versions.toml             # Dependency versions
├── build.gradle.kts                  # Root build configuration
├── settings.gradle.kts               # Settings configuration
├── gradle.properties                 # Gradle properties
├── metadata.json                     # App metadata
└── README.md                         # This file
```

## Prerequisites

- [Android Studio](https://developer.android.com/studio) (Latest version recommended)
- JDK 11 or higher
- Android SDK 30+
- Gradle 8.0+

## Setup Steps

### 1. Clone/Open the Project

1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project

### 2. Configure API Keys

1. Create a `.env` file in the project root directory
2. Add your OpenRouter API key:
   ```
   OPENROUTER_API_KEY=your_api_key_here
   ```

### 3. Build Configuration

1. Open `app/build.gradle.kts`
2. Remove or comment out the following line if it exists:
   ```
   signingConfig = signingConfigs.getByName("debugConfig")
   ```

### 4. Build and Run

1. Sync Gradle files by clicking **File → Sync Now**
2. Click **Build → Build Bundle(s) / APK(s)** to build the project
3. Click the **Run** button or use **Shift + F10** to run on an emulator or connected device
4. Select your target device and click **OK**

## Development

### Running Tests

- **Unit Tests**: Right-click on test file → **Run**
- **Instrumented Tests**: Right-click on androidTest file → **Run**

### Building for Production

1. Generate a signed APK: **Build → Generate Signed Bundle / APK**
2. Follow the wizard to sign with your keystore
3. The signed APK will be available in `app/release/`

## Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Database**: Room (SQLite)
- **Networking**: Retrofit
- **API**: OpenRouter API
- **Testing**: JUnit, Robolectric
- **Build System**: Gradle

## Contributing

We welcome contributions from the community! If you have suggestions, bug reports, or want to contribute to improving this app, please reach out:

- **LinkedIn**: [Krunal Rana](https://www.linkedin.com/in/krunal-rana/)
- **Email**: work.krunalrana@gmail.com

Feel free to connect and discuss ideas, improvements, or any enhancements you'd like to make!

## License

This project is provided as-is for educational and development purposes.
