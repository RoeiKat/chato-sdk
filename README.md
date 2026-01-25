# Chato Android SDK

[![](https://jitpack.io/v/RoeiKat/chato-sdk.svg)](https://jitpack.io/#RoeiKat/chato-sdk)

Chato is a lightweight Android SDK that adds **real-time in-app support chat** to your application.

It includes:
- A floating chat bubble inside your app
- A native chat popup screen
- Real-time messaging with the Chato web dashboard
- Remote customization (colors, icon, title, pre-chat copy)

---

## Requirements

- **Android minSdk 21+**
- **JDK 17 required** (the SDK is built with Java 17 / Kotlin `jvmTarget = 17`)
- Internet permission enabled
- A Chato account and an **API key**

> If your project uses Gradle toolchains, set `JavaLanguageVersion.of(17)`.

---

## Installation

### 1) Add JitPack repository

Add JitPack to your root `settings.gradle` / `settings.gradle.kts`:

```gradle
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    maven { url "https://jitpack.io" }
  }
}
```

### 2) Add the SDK dependency

In your app module `build.gradle` / `build.gradle.kts`:

```gradle
dependencies {
  implementation("com.github.RoeiKat:chato-sdk:v0.1.1")
}
```

### 3) Internet permission

Ensure your app has internet access:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

---

## Getting Started

### 1) Create an app & API key

1. Sign in to the Chato dashboard
2. Create a new application
3. Copy the generated API key

Each API key represents one Android app.

### 2) Initialize the SDK

Initialize Chato once (typically in `onCreate`):

```kotlin
class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    Chato.init(
      context = applicationContext,
      apiKey = "YOUR_API_KEY"
    )
  }
}
```

---

## Activity Lifecycle (Important)

Attach and detach Chato based on the Activity lifecycle:

```kotlin
override fun onResume() {
  super.onResume()
  Chato.attach(activity = this)
}

override fun onPause() {
  super.onPause()
  Chato.detach()
}
```

Why:
- `attach()` shows the bubble + enables the popup
- `detach()` removes overlays when the Activity is not visible
- prevents leaks / duplicated overlays

---

## Customization

Customization is managed remotely from the Chato dashboard:

- Primary color (bubbles + send button)
- Bubble background
- Chat title
- Bubble SVG icon
- Pre-chat questions and copy

Changes can apply without publishing a new app version (depending on what you change).

---

## Real-Time Messaging

Messages sync instantly between:
- Android app users
- Web dashboard agents

---

## License

Provided as a demo / project SDK.
