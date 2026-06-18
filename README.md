# NAPSAK – Real-Time Group Decision-Making App

NAPSAK is a modern, real-time collaborative decision-making application designed to help groups vote and decide on options interactively. The project ecosystem consists of a native **Android client** (built using Jetpack Compose, Clean Architecture, and Hilt) and a lightweight, responsive **Web client** (built using HTML5/JS and hosted on GitHub Pages). Both clients synchronize instantly through a shared Firebase Realtime Database backend.

---

## 🚀 Key Features

* **Real-Time Synchronization:** Multiplayer session synchronization and live voting lobby states are instantly updated across all clients via Firebase Realtime Database and Kotlin Coroutines `callbackFlow`.
* **Cross-Platform Access:** The lightweight web client hosted on GitHub Pages allows users without the Android app to participate in sessions directly from any mobile or desktop browser.
* **On-Device Image Processing Pipeline:** The local image pipeline prevents memory crashes (OOM) by validating file sizes (10MB limit), downscaling resolutions, and applying 75% JPEG compression on high-resolution images prior to upload.
* **Cost-Free Serverless Image Hosting:** Integrates the ImgBB REST API using OkHttp and Kotlinx Serialization to implement a cost-free, serverless image hosting solution, bypassing the need for paid cloud storage billing plans.
* **Offline Persistence & Local Caching:** Implements a local Room Database to cache default templates and persist user-created list configurations offline, reducing network call overhead.
* **Atomic Session Management:** Resolves multi-user race conditions during concurrent lobby joins by replacing bulk overwrites with atomic, transaction-based Firebase updates to ensure data integrity, while utilizing `onDisconnect` listeners for active participant tracking.
* **Build Security & Obfuscation:** Protects sensitive API keys by excluding them from version control, injecting them via Gradle `BuildConfig` from local properties, and configuring R8/ProGuard rules to minify code and optimize performance.

---

## 🏗️ Architecture & Package Structure

The Android client is architected following **Clean Architecture** and **MVVM** principles. This ensures a clean separation of concerns, modularity, and high testability.

```
app/src/main/java/com/napsak/app/
│
├── data/                  # Data Layer (Network, Local Database, Prefs, Repositories Implementation)
│   ├── datasource/        # Local & Preferences DataSources
│   ├── model/             # Data Transfer Objects (DTOs) for Firebase & Local storage
│   └── repository/        # Repository implementations (RoomRepositoryImpl, ImageRepositoryImpl)
│
├── domain/                # Domain Layer (Pure business logic, independent of Android framework)
│   ├── model/             # Clean Domain Models (Choice, Room, SavedChoiceList)
│   ├── repository/        # Repository interfaces defining data operations
│   └── usecase/           # Single-responsibility business rules (UploadImageUseCase, CreateRoomUseCase...)
│
├── di/                    # Dependency Injection Layer (Hilt Modules)
│   └── AppModule, NetworkModule, FirebaseModule
│
└── ui/                    # Presentation Layer (Jetpack Compose UI & ViewModels)
    ├── screens/           # Lobby, Voting, Home, CreateChoices, EditList Screens
    └── theme/             # Design Tokens, Color Palettes, Typography
```

---

## 🛠️ Tech Stack

### Android Client
* **Language:** Kotlin
* **UI Framework:** Jetpack Compose (Material Design 3)
* **Dependency Injection:** Dagger Hilt
* **Asynchronous & Flows:** Kotlin Coroutines, Flow (StateFlow, SharedFlow, callbackFlow)
* **Local Persistence:** Room Database, Preferences DataStore
* **Networking & Parsing:** OkHttp, Kotlinx Serialization
* **Image Loading:** Coil Compose
* **Obfuscation:** R8 / ProGuard

### Web Client
* **Frontend:** HTML5, Vanilla CSS3, Vanilla JavaScript (ES6+)
* **Hosting:** GitHub Pages
* **SDK:** Firebase Web SDK

### Backend
* **Database:** Firebase Realtime Database
* **Authentication:** Firebase Anonymous Authentication

---

## 🧪 Testing

The domain and presentation layers are built to be highly testable. Unit tests are implemented using **JUnit4** and **MockK** to verify UseCases and business logic in isolation.

To run the unit tests, execute:
```bash
./gradlew testDebugUnitTest
```

---

## ⚙️ Setup & Installation

### 1. Prerequisites
* Android Studio (Koala or newer)
* JDK 17 or 21
* A Firebase Project with Realtime Database and Anonymous Auth enabled.

### 2. Add Firebase to Android Client
Download your `google-services.json` from the Firebase Console and place it under the `/app` directory:
```
/NAPSAK/app/google-services.json
```

### 3. Add API Keys
Create or open your `local.properties` file in the root directory and add your ImgBB API key:
```properties
imgbb.api.key=YOUR_IMGBB_API_KEY_HERE
```
During build, Gradle will automatically inject this key into `BuildConfig.IMGBB_API_KEY`.

### 4. Build and Run
Click "Run" in Android Studio or install the debug APK via Gradle:
```bash
./gradlew installDebug
```

---
*Created by Ömer Emre Akpul*
