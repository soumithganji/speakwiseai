# SpeakWise AI ChatBot

<div align="center">

[![Platform](https://img.shields.io/badge/Platform-Android-green?style=for-the-badge&logo=android)](https://developer.android.com/)
[![API](https://img.shields.io/badge/Min%20SDK-19-orange?style=for-the-badge)](https://developer.android.com/studio/releases/platforms)
[![GPT](https://img.shields.io/badge/Powered%20By-GPT--3.5%20Turbo-purple?style=for-the-badge&logo=openai)](https://openai.com/)

**An intelligent AI-powered chatbot Android application leveraging OpenAI's GPT-3.5 Turbo model for natural language conversations with voice input/output capabilities.**

</div>

---

##  Table of Contents

- Overview
- Features
- Architecture
- Tech Stack
- Project Structure
- Getting Started
- Configuration
- API Integration
- Firebase Services
- Monetization
- Security
- Build & Deployment

---

##  Overview

SpeakWise AI is a production-ready Android chatbot application that provides users with an intuitive interface to interact with OpenAI's GPT-3.5 Turbo language model. The app supports both text and voice interactions, making AI assistance accessible to everyone. **Free to use with ads**, with an optional subscription to remove ads and unlock unlimited features.

### Key Highlights

- **Conversational AI**: Powered by OpenAI's GPT-3.5 Turbo for intelligent, context-aware responses
- **Voice Integration**: Speech-to-text input and text-to-speech output for hands-free interaction
- **Persistent Chat History**: Save and resume conversations at any time
- **Home Screen Widgets**: Quick access to start new chats directly from the home screen
- **Premium Features**: Subscription-based model with free tier and premium upgrades
- **Multi-language Support**: English and Chinese language localization

---

##  Features

### Core Features

| Feature | Description |
|---------|-------------|
| **Chat Interface** | Clean, intuitive messaging UI with real-time response streaming |
| **Voice Input** | Speech-to-text functionality for voice-based queries |
| **Text-to-Speech** | AI responses can be read aloud using system TTS |
| **Chat History** | All conversations are saved locally for future reference |
| **Multiple Chats** | Manage multiple conversation threads independently |
| **Home Widgets** | Small and large home screen widgets for quick access |

### Premium Features

| Feature | Free Tier | Premium |
|---------|-----------|---------|
| Daily Prompts | Limited | Unlimited |
| Token Limit | 3,000 tokens | Unlimited |
| Ads | Banner/Interstitial | Ad-free |
| Response Speed | Standard | Priority |

### User Experience

- **Modern Material Design** UI with smooth animations
- **Dark Mode** support for comfortable night usage
- **Language Selection** between English and Chinese
- **In-app Updates** via Google Play Core library
- **Push Notifications** via OneSignal integration
- **In-app Review** prompts for user feedback

---

##  Architecture

The application follows a clean, activity-based architecture with separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                      Presentation Layer                     │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │   Splash    │  │    Main     │  │       Chat          │  │
│  │  Activity   │─▶│  Activity   │─▶│     Activity        │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│                          │                    │             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │   Premium   │  │   Adapters  │  │      Widgets        │  │
│  │  Activity   │  │  (Chat/Msg) │  │  (Small/Large)      │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
├─────────────────────────────────────────────────────────────┤
│                       Business Layer                        │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                     Common (Singleton)                  ││
│  │  • API Key Management    • Token Management             ││
│  │  • Chat Persistence      • Encryption/Decryption        ││
│  │  • User Preferences      • Subscription State           ││
│  └─────────────────────────────────────────────────────────┘│
├─────────────────────────────────────────────────────────────┤
│                        Data Layer                           │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐ │
│  │ SharedPrefs    │  │   Firestore    │  │   OpenAI API   │ │
│  │ (Local Chat)   │  │  (Remote Cfg)  │  │   (Chat API)   │ │
│  └────────────────┘  └────────────────┘  └────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### Component Overview

| Component | Responsibility |
|-----------|----------------|
| `MyApplication` | App lifecycle, App Open Ads, initialization |
| `SplashActivity` | Startup screen, app open ad display |
| `MainActivity` | Chat list, navigation, settings, native ads |
| `ChatActivity` | Conversation UI, API calls, voice features |
| `PremiumActivity` | Subscription management, billing |
| `Common` | Singleton utility class for shared functionality |
| `BillingClientLifecycle` | Google Play Billing integration |

---

##  Tech Stack

### Core Technologies

| Technology | Version | Purpose |
|------------|---------|---------|
| **Java** | 1.8 | Primary development language |
| **Android SDK** | 33 (Min: 19) | Target platform |
| **Gradle** | 7.4.2 | Build system |
| **View Binding** | Enabled | Type-safe view access |
| **Data Binding** | Enabled | UI data binding |


##  Project Structure

```
speakwiseai/
├── app/
│   ├── src/main/
│   │   ├── java/chat/gpt/speakwise/gpt3/ai/chatbot/
│   │   │   └── app/
│   │   │       ├── Activities/
│   │   │       │   ├── BaseActivity.java       # Base activity class
│   │   │       │   ├── ChatActivity.java       # Main chat interface
│   │   │       │   ├── MainActivity.java       # Home screen/chat list
│   │   │       │   ├── PremiumActivity.java    # Subscription screen
│   │   │       │   └── SplashActivity.java     # Launch screen
│   │   │       ├── Adapters/
│   │   │       │   ├── ChatAdapter.java        # Chat list adapter
│   │   │       │   └── MessageAdapter.java     # Message list adapter
│   │   │       ├── Application/
│   │   │       │   └── MyApplication.java      # App class with ads
│   │   │       ├── CallBacks/
│   │   │       │   ├── DeleteChatCallBack.java
│   │   │       │   ├── OnPreferencesClearedListener.java
│   │   │       │   └── SubscriptionSuccessCallBack.java
│   │   │       ├── Models/
│   │   │       │   └── Message.java            # Chat message model
│   │   │       └── Utils/
│   │   │           ├── AppWidgetLarge.java     # Large home widget
│   │   │           ├── AppWidgetSmall.java     # Small home widget
│   │   │           ├── BillingClientLifecycle.java # Billing logic
│   │   │           ├── Common.java             # Utility singleton
│   │   │           ├── LinearLayoutManagerWrapper.java
│   │   │           └── SingleLiveEvent.java
│   │   ├── res/
│   │   │   ├── anim/                           # Animations
│   │   │   ├── drawable/                       # App icons, backgrounds
│   │   │   ├── font/                           # Custom fonts
│   │   │   ├── layout/                         # XML layouts
│   │   │   ├── values/                         # Strings, colors, themes
│   │   │   ├── values-zh/                      # Chinese localization
│   │   │   └── xml/                            # Widget configurations
│   │   └── AndroidManifest.xml
│   └── build.gradle                            # App-level build config
├── build.gradle                                # Project-level build config
├── gradle.properties
├── settings.gradle
└── speakwise.keystore                          # Release signing key
```

---

##  Getting Started

### Prerequisites

- **Android Studio** Arctic Fox (2020.3.1) or later
- **JDK** 8 or higher
- **Android SDK** 33 (compile SDK)
- **Minimum SDK** 19 (Android 4.4 KitKat)
- **OpenAI API Key**
- **Firebase Project** with google-services.json
- **Google Play Console** access (for billing)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/speakwiseai.git
   cd speakwiseai
   ```

2. **Configure Firebase**
   - Create a Firebase project at [Firebase Console](https://console.firebase.google.com)
   - Download `google-services.json` and place it in `app/` directory
   - Enable Firestore, Analytics, Crashlytics, and Performance Monitoring

3. **Configure OpenAI API**
   - The API key is managed via Firebase Remote Config
   - Create a document in Firestore with your encrypted API key

4. **Build the project**
   ```bash
   ./gradlew assembleDebug
   ```

5. **Run on device/emulator**
   ```bash
   ./gradlew installDebug
   ```

### Running on Physical Device

```bash
# List connected devices
adb devices

# Install and run
./gradlew installDebug
adb shell am start -n chat.gpt.speakwise.gpt3.ai.chatbot/.app.Activities.SplashActivity
```

---

##  Configuration

### Remote Configuration (Firebase Firestore)

The app retrieves configuration from Firestore on startup:

```javascript
// Firestore Document: config/data
{
  "key": "<encrypted_openai_api_key>",
  "prompts": false,                    // Enable/disable prompt limits
  "promptCount": 10,                   // Max prompts for free users
  "maxTokens": 3000,                   // Token limit for free users
  "appVersionCode": 27,                // Current app version
  "isBlocked": false,                  // Block app functionality
  "isMandatory": false,                // Force update flag
  "temperature": 0.7                   // GPT temperature setting
}
```

### Build Configuration

Key build settings in `app/build.gradle`:

| Setting | Value |
|---------|-------|
| Application ID | `chat.gpt.speakwise.gpt3.ai.chatbot.app` |
| Min SDK | 19 |
| Target SDK | 33 |
| Version Code | 27 |
| Version Name | 1.13 |
| MultiDex | Enabled |
| Supported Languages | English, Chinese |

---

##  API Integration

### OpenAI Chat Completions API

The app uses OpenAI's Chat Completions endpoint for AI responses:

```java
// API Endpoint
POST https://api.openai.com/v1/chat/completions

// Request Headers
Authorization: Bearer <API_KEY>
Content-Type: application/json

// Request Body
{
  "model": "gpt-3.5-turbo",
  "messages": [
    {"role": "user", "content": "Hello!"},
    {"role": "assistant", "content": "Hi there!"},
    {"role": "user", "content": "How are you?"}
  ],
  "temperature": 0.7,
  "max_tokens": 3000  // For free users only
}
```

### Network Layer

- **OkHttp 4.10.0** for HTTP requests
- 15-second connection timeout
- 60-second read timeout
- Async callback-based response handling

---

##  Firebase Services

| Service | Purpose |
|---------|---------|
| **Firebase Analytics** | User behavior tracking, event logging |
| **Firebase Crashlytics** | Crash reporting and diagnostics |
| **Firebase Firestore** | Remote configuration storage |
| **Firebase Performance** | App performance monitoring |
| **Firebase App Check** | Play Integrity verification |

### Key Events Logged

```java
// Analytics Events
FirebaseAnalytics.logEvent("response_received", null);
FirebaseAnalytics.logEvent("chat_created", null);
FirebaseAnalytics.logEvent("premium_purchased", null);

// Crashlytics
FirebaseCrashlytics.getInstance().recordException(exception);
FirebaseCrashlytics.getInstance().log("API Response Error");
```

---

##  Monetization

### Advertising (Google AdMob)

| Ad Type | Placement |
|---------|-----------|
| **App Open Ads** | Displayed when app comes to foreground |
| **Banner Ads** | Bottom of chat screen |
| **Interstitial Ads** | Between chat sessions |
| **Native Ads** | Integrated in main activity |

### In-App Subscriptions (Google Play Billing)

| Plan | Duration | Features |
|------|----------|----------|
| Weekly | 7 days | Ad-free, unlimited prompts |
| Monthly | 30 days | Ad-free, unlimited prompts |
| Yearly | 365 days | Ad-free, unlimited prompts (75% savings) |

All subscriptions include:
- ✅ Unlimited prompts
- ✅ Higher token limits
- ✅ Ad-free experience
- ✅ Priority response speed

### Push Notifications (OneSignal)

Integrated for user engagement and retention campaigns.

---

##  Security

### API Key Protection

- API keys are **not hardcoded** in the source
- Keys are stored encrypted in Firebase Firestore
- AES encryption/decryption with Base64 encoding
- Keys are fetched at runtime and decrypted

```java
// Encryption/Decryption mechanism in Common.java
public String decrypt(String secret, String encodedKey) {
    // AES decryption with SHA-256 key generation
}
```

### App Integrity

- **Firebase App Check** with Play Integrity provider
- Protects backend resources from abuse
- Blocks requests from tampered apps

### Data Storage

- Chat history stored in SharedPreferences (local)
- No sensitive data transmitted to third parties
- User preferences stored locally

---

##  Build & Deployment

### Debug Build

```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Release Build

```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

### Signing Configuration

The release build is signed using `speakwise.keystore` located in the project root.

### ProGuard

ProGuard is configured but disabled (`minifyEnabled false`). Enable for production:

```groovy
buildTypes {
    release {
        minifyEnabled true
        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
    }
}
```
