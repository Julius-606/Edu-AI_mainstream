# 📱 Android Studio Guide for Trace Learning System

## Prerequisites
- **Android Studio:** Hedgehog (2023.1.1), Iguana, Jellyfish, Koala, or Ladybug.
- **JDK:** OpenJDK 17 or 21 (recommended for Android Gradle Plugin 8.7+).
- **Android SDK:** Platform 35 (Android 15), build-tools 35.0.0.

## Project Structure
```
Trace/
├── build.gradle.kts          <-- Root Gradle project
├── settings.gradle.kts       <-- Includes ':app'
├── gradle/
│   ├── libs.versions.toml    <-- Central Version Catalog
│   └── wrapper/
│       └── gradle-wrapper.properties
└── app/
    ├── build.gradle.kts      <-- Module configuration (SDK 35, Jetpack Compose, Room)
    └── src/
        └── main/
            ├── AndroidManifest.xml
            ├── java/com/example/edu_ai/
            │   ├── MainActivity.kt
            │   ├── EduAIApplication.kt
            │   ├── data/       (Room DB, DAOs, Retrofit API)
            │   ├── ui/         (Compose Screens, ViewModels, Themes)
            │   └── repository/ (Offline-first data sync)
            └── res/
```

## How to Open and Compile in Android Studio

1. Launch Android Studio.
2. Select **Open** and choose the root directory of this repository (`Trace`).
3. Android Studio will automatically recognize the `settings.gradle.kts` and sync Gradle using the version catalog in `gradle/libs.versions.toml`.
4. To configure the backend URL used by the app, you can pass `-PbackendBaseUrl="..."` or set it in your `gradle.properties`:
   ```properties
   backendBaseUrl=https://huggingface.co/spaces/Agent606/Edu-AI/
   ```
5. Choose an emulator or physical device running Android 7.0 (API 24) or higher.
6. Click **Run 'app'** (`Shift + F10`).

## Key Android Architecture Highlights
- **Jetpack Compose:** Modern declarative UI with custom clinical themes, dark mode, dynamic backgrounds, and progress rings.
- **Room Database with KSP:** Complete local cache for offline usage with automatic CAS (Content Addressable Storage) blob storage.
- **Retrofit 2:** Communicates with both Hugging Face Spaces and local backends with bearer token authentication.
