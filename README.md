# Chato Android SDK

Chato is a lightweight Android SDK that lets you add **real-time in-app
support chat** to your application in minutes.

The SDK provides: - A floating chat bubble inside your app - A
native-feeling chat popup - Real-time messaging with a web dashboard -
Remote customization (colors, icon, copy) - No backend setup required

------------------------------------------------------------------------

## Requirements

-   Android **minSdk 21+**
-   Kotlin (Java supported)
-   Internet permission enabled
-   A Chato account and **API key**

------------------------------------------------------------------------

## Installation

### 1. Add JitPack repository

Add JitPack to your project-level configuration.

#### settings.gradle

``` gradle
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    maven { url "https://jitpack.io" }
  }
}
```

------------------------------------------------------------------------

### 2. Add the SDK dependency

In your app module build.gradle:

``` gradle
dependencies {
  implementation("com.github.YOUR_USERNAME:chato-sdk:VERSION")
}
```

Replace: - YOUR_USERNAME with your GitHub/JitPack username - VERSION
with the published SDK version

------------------------------------------------------------------------

### 3. Internet permission

Ensure your app has internet access:

``` xml
<uses-permission android:name="android.permission.INTERNET" />
```

------------------------------------------------------------------------

## Getting Started

### 1. Create an app & API key

1.  Sign in to the Chato dashboard\
2.  Create a new application\
3.  Copy the generated API key

Each API key represents one Android app.

------------------------------------------------------------------------

### 2. Initialize the SDK

Initialize Chato once, usually in onCreate.

``` kotlin
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

------------------------------------------------------------------------

## Activity Lifecycle (Important)

Attach and detach Chato based on the Activity lifecycle.

``` kotlin
override fun onResume() {
  super.onResume()
  Chato.attach(activity = this)
}

override fun onPause() {
  super.onPause()
  Chato.detach()
}
```

Why this is required: - attach() shows the chat bubble and popup -
detach() removes overlays when the Activity is not visible - Prevents
memory leaks and UI duplication

------------------------------------------------------------------------

## Chat Bubble

The floating bubble: - Appears above your UI - Opens the chat popup when
tapped - Persists while the Activity is active

Customization: - Bubble color - Bubble icon (SVG) - Position
(left/right)

All customization is handled remotely from the dashboard.

------------------------------------------------------------------------

## Chat Popup

The popup includes: - Header title (default: Support) - Message list -
Input field - Send button

The UI is designed to feel native and match your app's branding.

------------------------------------------------------------------------

## Pre-Chat Flow

Before a chat starts, you can configure a pre-chat flow: 1. Ask for user
name 2. Ask for email (optional) 3. Final message (FAQ / waiting
message)

Configured entirely from the dashboard.

------------------------------------------------------------------------

## Customization

All customization is handled from the Chato dashboard: - Primary color
(message bubbles + send button) - Bubble background color - Chat title -
Bubble SVG icon - Pre-chat questions and copy

Changes apply instantly without an app update.

------------------------------------------------------------------------

## Real-Time Messaging

Messages sync instantly between: - Android app users - Web dashboard
agents

No polling or socket setup is required.

------------------------------------------------------------------------

## Notes for Evaluators

-   The SDK is distributed as a hosted Android library
-   Integrated using standard Gradle dependency management
-   No manual source code copying is required
-   Lifecycle handling follows Android best practices

------------------------------------------------------------------------

## License

This project is provided as part of an academic assignment and demo SDK.
