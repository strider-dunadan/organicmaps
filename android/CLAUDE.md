# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this
repository.

## Project Overview

Organic Maps is a privacy-first offline maps & GPS app. This is the **Android module** of the
cross-platform project. The app is built on a C++ core with Java UI layer.

## Build Commands

```bash
# Build, install, and run (from android/ directory)
./gradlew runWebDebug          # Web Debug variant
./gradlew runFdroidDebug       # F-Droid Debug variant
./gradlew runGoogleDebug       # Google Play Debug variant
./gradlew runHuaweiDebug       # Huawei AppGallery Debug variant

# Build APKs
./gradlew assembleFdroidBeta   # F-Droid Beta APK
./gradlew assembleGoogleRelease # Google Play Release APK
./gradlew assembleHuaweiRelease # Huawei AppGallery Release APK
./gradlew assembleBeta         # Beta APKs for all flavors
./gradlew assembleRelease      # Release APKs for all flavors

# Build AABs (Android App Bundles)
./gradlew bundleGoogleRelease  # AAB for Play Store
./gradlew bundleHuaweiRelease  # AAB for AppGallery

# Clean build
./gradlew clean

# List all tasks
./gradlew tasks
```

### Build Variants

- **Flavors**: `google`, `web`, `fdroid`, `huawei`
- **Build types**: `debug`, `beta`, `release`

### Build Flags

- `-Parm64`, `-Parm32`, `-Px64`, `-Px86` — target architecture (default builds all)
- `-Ppch` — enable precompiled headers (~15% faster)
- `-PsplitApk` — separate APKs per ABI
- `-Pnjobs=N` — parallel compilation jobs
- `-Pfirebase=true` — enable Firebase
- `-PenableTrace=ON` — enable tracing
- `-PenableVulkanDiagnostics=ON` — Vulkan diagnostics

### Lint & Verification

```bash
./gradlew lint                 # Run lint on app module
./gradlew lintAllModules       # Run lint on all modules with merged report
./gradlew lintFix              # Auto-fix lint issues
./gradlew check                # Run all checks
```

## Testing

```bash
# Unit tests
./gradlew test

# Android instrumented tests
./gradlew connectedAndroidTest
```

Test files are in:

- `app/src/test/java/` — Unit tests
- `sdk/src/test/java/` — SDK unit tests
- `sdk/src/androidTest/java/` — Instrumented tests

## Project Structure

```
android/
├── app/                          # Main application module
│   └── src/main/java/app/organicmaps/
│       ├── MwmApplication.java   # Application entry point
│       ├── MwmActivity.java      # Main map activity
│       ├── SplashActivity.java   # Startup activity
│       ├── adapter/              # RecyclerView adapters
│       ├── background/           # Background services
│       ├── base/                 # Base classes
│       ├── bookmarks/            # Bookmarks UI
│       ├── car/                  # Android Auto UI
│       ├── dialog/               # Dialog components
│       ├── downloader/           # Map download UI
│       ├── editor/               # OSM editor UI
│       ├── help/                 # Help screens
│       ├── intent/               # Intent handling
│       ├── location/             # Location UI
│       ├── maplayer/             # Map layer UI
│       ├── routing/              # Navigation UI
│       ├── search/               # Search UI
│       ├── settings/             # Settings UI
│       ├── util/                 # Utilities
│       └── widget/               # Custom widgets
├── sdk/                          # Core SDK module (JNI bridge to C++)
│   ├── car/                      # Android Auto SDK
│   ├── maps/world/               # World map data
│   ├── widgets/lanes/            # Lane guidance widget
│   ├── widgets/speedlimit/       # Speed limit widget
│   └── src/main/java/app/organicmaps/sdk/
│       ├── Framework.java        # JNI bridge to C++ Framework
│       ├── Map.java              # Map control
│       ├── OrganicMaps.java      # SDK initialization
│       ├── api/                  # Public API
│       ├── bookmarks/            # Bookmarks logic
│       ├── content/              # Content providers
│       ├── display/              # Display utilities
│       ├── downloader/           # Download logic
│       ├── editor/               # OSM editor logic
│       ├── location/             # Location services
│       ├── maplayer/             # Map layer logic
│       ├── products/             # Product features
│       ├── routing/              # Routing logic
│       ├── search/               # Search logic
│       ├── settings/             # Settings logic
│       ├── sound/                # Sound/TTS
│       ├── util/                 # SDK utilities
│       └── widget/               # SDK widgets
├── libs/                         # Reusable library modules
│   ├── api/                      # Public API library
│   ├── branding/                 # Branding customization
│   ├── car/                      # Android Auto library
│   ├── downloader/               # Map download library
│   ├── googleassistant/          # Google Assistant integration
│   ├── routing/                  # Routing library
│   └── utils/                    # Shared utilities
└── groovy/                       # Shared Gradle configs
    ├── common-config.gradle      # Common module configuration
    └── permission-checker.gradle # APK permission validation
```

### Architecture Notes

- **JNI Bridge**: `Framework.java` wraps `android::Framework.cpp` via native methods
- **Native Library**: `liborganicmaps.so` — built from C++ sources in project root
- **CMake**: Native build configured in `sdk/build.gradle`, references `../../CMakeLists.txt`

## SDK Versions

| Property    | Value            |
|-------------|------------------|
| Min SDK     | 21 (Android 5.0) |
| Target SDK  | 36 (Android 15)  |
| Compile SDK | 36               |
| NDK         | 29.0.14206865    |
| Java        | 17               |

## Code Style

- Use `clang-format` 21.0+ for Java code formatting
- Format on save: `tools/unix/clang-format.sh` or `git clang-format`
- Java 17 compatibility required
- Sign commits with DCO: `git commit -s -m "message"`

## Key Files

- `build.gradle` (root) — Version from `../tools/unix/version.sh`
- `settings.gradle` — Module definitions
- `app/build.gradle` — Flavors, Firebase config, signing
- `sdk/build.gradle` — Native build config, NDK settings
- `gradle.properties` — SDK versions, supported locales
- `groovy/common-config.gradle` — Shared module configuration
- `groovy/permission-checker.gradle` — APK permission validation
- `lint.xml` — Lint suppressions
