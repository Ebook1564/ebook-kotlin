# Build Setup — Ebook Reader (Android)
## Prerequisites
| Tool | Version | Path |
|---|---|---|
| Java | **17** (OpenJDK) | `/usr/lib/jvm/java-17-openjdk-amd64` |
| Android SDK | **35** (compile), **24** (min), **36** (target) | Linux: `/home/aayush/android-sdk` / Windows: `C:\Users\we\AppData\Local\Android\Sdk` |
| Gradle | **8.11.1** (wrapper) | |
| Kotlin | **2.0.21** | |
| AGP | **8.9.0** | |
| Compose BOM | **2024.10.01** | |
| Room | **2.6.1** | |
## Build Commands
### Linux (WSL)
```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 \
ANDROID_HOME=/home/aayush/android-sdk \
./gradlew :app:assembleRelease --no-daemon -x lintVitalAnalyzeRelease
Windows (PowerShell)
$env:JAVA_HOME="C:\Program Files\Java\jdk-17"
$env:ANDROID_HOME="C:\Users\we\AppData\Local\Android\Sdk"
.\gradlew :app:assembleRelease --no-daemon -x lintVitalAnalyzeRelease
Debug APK
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/home/aayush/android-sdk \
./gradlew :app:assembleDebug --no-daemon
Project Structure
android/
├── app/
│   ├── src/main/
│   │   ├── kotlin/com/freeebookbundle/
│   │   │   ├── audio/          # AudiobookPlayerActivity
│   │   │   ├── db/             # Room entities, DAOs, AppDatabase
│   │   │   ├── epub/           # NativeEpubReader, EpubTextToolbar
│   │   │   ├── models/         # Book, CatalogEntry
│   │   │   ├── repository/     # CatalogRepository
│   │   │   ├── ui/             # All Compose screens
│   │   │   ├── utils/          # ServerConfig, FileDownloader
│   │   │   ├── viewmodel/      # HomeViewModel, LibraryViewModel, etc.
│   │   │   ├── voice/          # TTS (TtsVoiceManager)
│   │   │   ├── EbookApp.kt     # Application class
│   │   │   └── MainActivity.kt
│   │   ├── assets/
│   │   │   ├── catalog.json    # 10.4MB, 41K+ books (bundled)
│   │   │   └── reader.html     # EPUB reader webview
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
├── gradle.properties
├── gradle/
└── settings.gradle.kts
server/  (Python scripts for catalog/cover generation)
Server
Service	URL
Nginx	http://54.254.205.11:8080
API	Port 8000
TTS	Port 5000
Nginx locations
/            → /var/www/ebooks/   (root, serves catalog.json)
/epubs/      → auto-indexed EPUB files
/covers/     → book cover images
/audiobooks/ → per-chapter M4B files at /var/www/ebooks/audiobooks/{id}/
/api/        → API endpoint
Key Dependencies
- UI: Jetpack Compose (Material 3) + Coil (images)
- Navigation: Compose Navigation
- Paging: Paging 3 + Compose
- Database: Room (2.6.1) with KSP
- Serialization: Gson
- EPUB reader: Readium (via local nanohttpd server + WebView)
- TTS: Piper (local on-device via python)
Troubleshooting
Build fails with "JAVA_HOME is set to an invalid directory"
Set the correct JDK 17 path as shown in Build Commands above.
Room schema issues after catalog changes
Delete app data or increment DB version with migration. The DB is rebuilt from catalog.json on first launch.
Audiobooks show "No chapters found"
1. Verify nginx alias: location /audiobooks/ { alias /var/www/ebooks/audiobooks/; }
2. Verify chapters.json exists in each book's directory
3. Test: curl http://54.254.205.11:8080/audiobooks/19504/chapters.json
JPEG-encoded EPUB covers fail
Re-encode with PNG using the provided python script.