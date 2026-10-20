# Laundry HUB

Laundry ordering system, built as a school OOP Java project. Customers book **Walk-in** or
**Pickup & Delivery** laundry (minimum 8 kg, choice of fabric soap, Google Maps pin,
time slot, Cash or GCash). Riders accept pickup and return-delivery jobs. Admin and staff
manage orders, pricing and reports.

| Folder | What | Stack | Build tool |
|---|---|---|---|
| [`backend/`](backend/) | REST API, all pricing & business rules | Spring Boot 3.5, Java 17 | Maven wrapper `mvnw.cmd` |
| [`android-app/`](android-app/) | Customer + Rider app | Android (Java, XML layouts), minSdk 24 | Gradle wrapper `gradlew.bat` |
| [`admin-desktop/`](admin-desktop/) | Admin / staff console | JavaFX 21, Java 17 | Maven wrapper `mvnw.cmd` |
| [`docs/design/`](docs/design/) | UI design: spec, screenshots, clickable prototype | — | open `docs/design/prototype/index.html` |

**Rules for contributors (and Claude Code) are in [`CLAUDE.md`](CLAUDE.md).** The most
important rule: every screen must match `docs/design/` exactly.

## Requirements

- Windows + **VS Code** (no Android Studio). Accept the extension recommendations in `.vscode/extensions.json`.
- **JDK 17**. The wrappers download Maven and Gradle themselves; no global installs needed.
- Android SDK at `%LOCALAPPDATA%\Android\Sdk`, plus an emulator (AVD `Pixel_4`) or a phone with USB debugging.

VS Code's integrated terminal already has `JAVA_HOME`, `adb` and `emulator` on PATH
(`.vscode/settings.json`). In any other PowerShell window, run this first:

```powershell
$env:JAVA_HOME   = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:PATH = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\emulator;$env:PATH"
```

## Secrets

Secrets are **never committed**.

| Where | How |
|---|---|
| Backend | Environment variables or `backend/.env` (git-ignored, see `backend/.env.example`): `LAUNDRYHUB_JWT_SECRET`, `GOOGLE_OAUTH_CLIENT_ID`, `GOOGLE_SERVER_KEY`, `SHOP_LAT`/`SHOP_LNG`, `DB_*` (read in `application.yml`) |
| Android | `android-app/local.properties`: `sdk.dir=...`, `MAPS_API_KEY=...` and `GOOGLE_WEB_CLIENT_ID=...` (git-ignored) |

## Backend

```powershell
cd backend
.\mvnw.cmd test               # unit + MockMvc integration tests (in-memory H2, no database needed)
.\mvnw.cmd spring-boot:run    # http://localhost:8080/api/ping · API docs: http://localhost:8080/swagger-ui
.\mvnw.cmd package            # %USERPROFILE%\.laundryhub-build\laundryhub-backend\laundryhub-backend-0.1.0.jar
```

Debug from VS Code: **Run and Debug → "Backend: Spring Boot (launch + debug)"**.

Which endpoint serves which screen and button: [`docs/SCREEN_API_MAP.md`](docs/SCREEN_API_MAP.md).

## Admin console

```powershell
cd admin-desktop
.\mvnw.cmd test
.\mvnw.cmd javafx:run          # opens the window
.\mvnw.cmd javafx:run@debug    # waits for a debugger on port 5005
```

Debug from VS Code: **"Admin: JavaFX (mvnw javafx:run + attach)"**. **"Backend + Admin"** runs both.

## Android app

```powershell
cd android-app
emulator -avd Pixel_4                        # in a separate terminal, or plug in a phone
adb devices                                  # the device must show as "device"
.\gradlew.bat assembleDebug                  # %USERPROFILE%\.laundryhub-build\android-app\app\outputs\apk\debug\app-debug.apk
.\gradlew.bat installDebug                   # install on the running emulator/phone
adb shell am start -n com.laundryhub.app/.MainActivity
.\gradlew.bat testDebugUnitTest              # JVM unit tests
.\gradlew.bat connectedDebugAndroidTest      # instrumented tests (needs a device)
adb logcat --pid=$(adb shell pidof com.laundryhub.app)
```

The emulator reaches the backend at `http://10.0.2.2:8080/api/` (`BuildConfig.API_BASE_URL`).
On a physical phone, run `adb reverse tcp:8080 tcp:8080` and use `http://localhost:8080/api/`,
or change the URL to your PC's LAN IP.

All of these commands are also VS Code tasks (**Terminal → Run Task**).

## Non-ASCII project path

The repo lives under `OneDrive\문서`. On Windows, `java.exe` reads its arguments and
`@argfiles` in the ANSI code page, so any JVM started with a path inside the repo sees `??`
and fails. These workarounds are in place:

- **Build output goes outside the repo** to `%USERPROFILE%\.laundryhub-build\` (Maven profile
  `ascii-build-dir` on Windows; for Gradle, `android-app/build.gradle.kts` whenever the path is non-ASCII).
  This also keeps build files out of OneDrive sync.
- `gradlew.bat` passes the wrapper jar by its 8.3 short path.
- `.mvn/jvm.config` runs Maven in UTF-8, and the admin tests run on the classpath (`useModulePath=false`).
- `android.overridePathCheck=true` lets the Android Gradle Plugin accept the path.

The simplest permanent fix is to move the repo to an ASCII path such as `C:\dev\LaundryHUB`.

## Fonts

DM Sans and Bricolage Grotesque (SIL Open Font License) come from
`docs/design/prototype/assets/fonts/files`. They were converted from woff2 to TTF, with the
latin and latin-ext subsets merged so that ₱ is included, into
`android-app/app/src/main/res/font/` and `admin-desktop/src/main/resources/fonts/`.
