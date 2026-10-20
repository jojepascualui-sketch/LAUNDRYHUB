# Deploying Laundry HUB

This guide puts the system online:
- the **backend** on Render (Docker), with a hosted **MySQL** database on Railway or Aiven;
- the **Android app** as a signed APK, shared through GitHub Releases or Firebase App Distribution;
- the **admin console** as a Windows installer.

Every command runs in the **VS Code terminal (PowerShell)**. Nothing secret goes into Git: values live in the hosting
dashboards, in environment variables, or in git-ignored files (CLAUDE.md §8).

```
Android app (APK) ─┐                       ┌─ Google Geocoding API   (GOOGLE_SERVER_KEY)
                   ├─ HTTPS ─▶ Render: Spring Boot API ─┤
Admin console    ──┘   /api/...          │  └─ Maps JavaScript API  (MAPS_JS_KEY, admin map page)
                                         ▼
                              Railway / Aiven: MySQL 8  (Flyway migrates on every start)
Phones: Maps SDK for Android (MAPS_API_KEY in the APK, restricted to the app's signature)
```

| Step | What | Result |
|---|---|---|
| 1 | Google Cloud: API keys and OAuth client | 3 restricted keys + client id |
| 2 | Hosted MySQL | `DB_URL`, `DB_USER`, `DB_PASSWORD` |
| 3 | Backend on Render | `https://<name>.onrender.com/api/` |
| 4 | First sign-in | admin password changed |
| 5 | Android release | signed `app-release.apk` |
| 6 | Share the APK | GitHub Release or Firebase testers |
| 7 | Admin installer | `Laundry HUB Admin-<version>.exe` |

**Before you start**
- The repository is pushed to GitHub (`git push`).
- You have accounts on GitHub, Render, Google Cloud (with billing turned on), and Railway or Aiven.
- These tools are installed and work in the VS Code terminal:
  - **GitHub CLI:** `winget install GitHub.cli`, then `gh auth login`.
  - **JDK 17:** already installed (CLAUDE.md §6).
  - **Node.js:** only for Firebase (`winget install OpenJS.NodeJS.LTS`).
  - **WiX Toolset 3:** only for the admin installer (`winget install WiXToolset.WiXToolset`).
- After a `winget install`, close and reopen the terminal so the new command is found.

---

## 1. Google Cloud: API keys and OAuth client

Open <https://console.cloud.google.com> and create (or pick) a project, e.g. `laundryhub`. Maps Platform needs a
**billing account** on the project. Google's monthly free credit covers a school-sized shop.

### 1.1 Enable the APIs
**APIs & Services → Library** → enable each:
- **Maps SDK for Android** (map on 04a, 07, R2/R3)
- **Maps JavaScript API** (admin A2 "Pinned location")
- **Geocoding API** (address from the pin, 04a)

### 1.2 Create three API keys, each restricted
**APIs & Services → Credentials → + Create credentials → API key**. Rename each key, then press **Edit**:

| Key name | Application restriction | API restriction | Goes into |
|---|---|---|---|
| `laundryhub-android` | **Android apps**: package `com.laundryhub.app` + the **SHA-1** of the debug *and* release signing keys (below) | Maps SDK for Android | `MAPS_API_KEY` in `android-app/local.properties` |
| `laundryhub-server` | **IP addresses**: Render's outbound IPs (Render → your service → **Connect → Outbound**). If you cannot list them, choose *None*, but keep the API restriction | Geocoding API | `GOOGLE_SERVER_KEY` (Render) |
| `laundryhub-admin-map` | **Websites**: `https://<name>.onrender.com/*` (the backend serves the map page at `/api/maps/order-pin`, so the page's address is the API's) | Maps JavaScript API | `MAPS_JS_KEY` (Render) |

**SHA-1 fingerprints for the Android key.** You can add these later, after creating the release key in step 5.1:
```powershell
# Debug key (emulator / development builds)
keytool -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android | Select-String "SHA1:"
# Release key (step 5.1)
keytool -list -v -keystore "$env:USERPROFILE\laundryhub-keys\laundryhub-release.jks" -alias laundryhub | Select-String "SHA1:"
```
Without the matching SHA-1 the map stays grey and Logcat shows "Authorization failure".

**Cost guard.** Go to **APIs & Services → each API → Quotas** and cap the requests per day (e.g. 1,000). Then go to
**Billing → Budgets & alerts** and add a budget with an email alert.

### 1.3 OAuth client for "Continue with Google"
1. **APIs & Services → OAuth consent screen**: External, app name "Laundry HUB", support email, and your testers'
   addresses while in *Testing*.
2. **Credentials → + Create credentials → OAuth client ID → Web application** ("laundryhub-web"). Copy its **client
   ID**. This one value is `GOOGLE_WEB_CLIENT_ID` for the backend (Render) and for the app (`local.properties`).
3. **+ Create credentials → OAuth client ID → Android**, package `com.laundryhub.app`, with the **release SHA-1**
   (add a second Android client for the debug SHA-1 if you test Google sign-in on debug builds). The app never uses
   this id directly, but Google refuses the sign-in without it.

---

## 2. Hosted MySQL (Railway or Aiven)

Render only offers PostgreSQL, and the backend uses MySQL 8 (Flyway scripts in
`backend/src/main/resources/db/migration`). Pick **one** of these hosts.

### Option A — Railway
1. <https://railway.com> → **New Project → Deploy MySQL**.
2. Open the MySQL service → **Settings → Networking → Public Networking**: enable the **TCP Proxy**. Render
   connects from outside Railway, so the private `*.railway.internal` address does not work.
3. **Variables** tab: note `MYSQLUSER`, `MYSQLPASSWORD` and `MYSQLDATABASE` (usually `railway`), plus the public host
   and port from the TCP proxy (e.g. `xxxx.proxy.rlwy.net:12345`).
4. Your three values:
   ```
   DB_URL      = jdbc:mysql://xxxx.proxy.rlwy.net:12345/railway?sslMode=REQUIRED&serverTimezone=Asia/Manila
   DB_USER     = <MYSQLUSER>
   DB_PASSWORD = <MYSQLPASSWORD>
   ```

### Option B — Aiven
1. <https://console.aiven.io> → **Create service → MySQL** → *Free* plan, a region near the Philippines
   (e.g. Singapore).
2. When it is running, the **Overview** shows host, port, user `avnadmin`, password and database `defaultdb`.
   Aiven only accepts TLS connections.
3. Optional, for a clean database name: **Databases → Create database** `laundryhub`.
4. Your three values:
   ```
   DB_URL      = jdbc:mysql://<host>:<port>/laundryhub?sslMode=REQUIRED&serverTimezone=Asia/Manila
   DB_USER     = avnadmin
   DB_PASSWORD = <password>
   ```
   (Use `/defaultdb` instead of `/laundryhub` if you skipped step 3.)

### Running the Flyway migrations
Flyway runs **automatically every time the backend starts**:
- **First start:** it creates every table (V1, V2, V4, V5) and records them in `flyway_schema_history`.
- **Later starts:** it applies only the new scripts.

On Render, `FLYWAY_LOCATIONS=classpath:db/migration` keeps out the design's sample orders
(`db/sample-data/V3__sample_orders.sql`).

To migrate (and check the connection) from your PC **before** deploying:
```powershell
cd backend
$env:DB_URL = "jdbc:mysql://<host>:<port>/<db>?sslMode=REQUIRED&serverTimezone=Asia/Manila"
$env:DB_USER = "<user>"
$env:DB_PASSWORD = Read-Host "DB password"          # typed, not saved in history
$env:FLYWAY_LOCATIONS = "classpath:db/migration"
$env:ADMIN_DEFAULT_PASSWORD = Read-Host "First admin password (8+ characters)"
.\mvnw.cmd spring-boot:run
# Wait for "Started LaundryHubApplication", check http://localhost:8080/actuator/health, then Ctrl+C.
Remove-Item Env:DB_PASSWORD, Env:ADMIN_DEFAULT_PASSWORD
```
The log lists each `Migrating schema ... to version ...` line. A migrated version is never re-run. **Never edit a
script that has already run.** Add a new `V5__...sql` instead.

**Backups.** Railway: **Backups** tab (paid plans), or `mysqldump` over the TCP proxy. Aiven takes daily backups,
including on the free plan.

---

## 3. Backend on Render

The repository already contains:
- **`backend/Dockerfile`** — a multi-stage build. Maven builds the jar, which then runs on a small JRE image as a
  non-root user. The image has a `HEALTHCHECK` on `/actuator/health`, honours `PORT`, and leaves out sample data.
- **`render.yaml`** — the Render Blueprint: a Docker web service, health check path, and every variable. Secrets are
  marked `sync: false`.
- **Health endpoints** (no sign-in):
  - `/actuator/health`: UP only when the database answers.
  - `/actuator/health/liveness` and `/actuator/health/readiness`.
  - `/api/ping`.

### 3.1 Create the service
1. <https://dashboard.render.com> → **New → Blueprint** → connect GitHub → pick the repository → **Apply**.
2. Render reads `render.yaml` and asks for every `sync: false` value:

| Variable | Value |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | From step 2 |
| `JWT_SECRET` | Generated by Render (`generateValue`). Leave it: changing it signs everyone out. |
| `GOOGLE_WEB_CLIENT_ID` | OAuth **web** client id (1.3) |
| `GOOGLE_SERVER_KEY` | `laundryhub-server` key (1.2) |
| `MAPS_JS_KEY` | `laundryhub-admin-map` key (1.2) |
| `SHOP_LAT`, `SHOP_LNG` | The shop's pin, e.g. `14.600000` / `121.000000` (Google Maps → right-click the shop → copy the numbers) |
| `ADMIN_DEFAULT_PASSWORD` | First admin password, 8+ characters (only used when no admin exists yet) |
| `PAYMONGO_SECRET_KEY`, `PAYMONGO_WEBHOOK_SECRET` | **Reserved.** Online GCash checkout is not built yet; GCash payments are confirmed manually in A2. Leave empty or put your PayMongo test keys here for later. |
| `FLYWAY_LOCATIONS`, `SHOP_TIME_ZONE` | Already set by the Blueprint |

3. The first deploy builds the Docker image (about 5–8 minutes). It is live when the health check turns green.

### 3.2 Check it
```powershell
$api = "https://<name>.onrender.com"
Invoke-RestMethod "$api/actuator/health"          # status : UP
Invoke-RestMethod "$api/api/ping"                 # status : ok
Invoke-RestMethod "$api/api/pricing"              # ₱30 /kg, 8 kg minimum, soaps
Start-Process "$api/swagger-ui"                   # interactive API docs
```

**Notes**
- **Free plan cold starts:** the free plan sleeps after 15 minutes without traffic, and the first request then takes
  about a minute. For a demo, open `/actuator/health` a minute before, or use a paid instance.
- **Deploys:** every `git push` to `main` redeploys (`autoDeploy: true`). Logs are under **Logs**; never paste them
  publicly, because they contain request paths.
- **Running elsewhere:** the same image runs on Railway (**New → GitHub repo**, root directory `backend`, add the
  same variables, health check path `/actuator/health`) or on any Docker host:
  ```powershell
  docker build -t laundryhub-backend backend
  docker run -p 8080:8080 --env-file backend/.env laundryhub-backend
  ```

---

## 4. First sign-in

1. On first start the backend creates `admin@laundryhub.ph` (or `ADMIN_DEFAULT_EMAIL`) with
   `ADMIN_DEFAULT_PASSWORD`.
2. Sign in from the admin console (step 7). Then:
   - **A4 → Staff → Edit Admin**: set a new password.
   - **A3**: check the rates, soaps and the shop GCash number, then **Save changes**.
   - **A4 → Riders → + Add rider**: create the riders' accounts.
3. On Render you can now clear `ADMIN_DEFAULT_PASSWORD`. It is only read when no admin exists.

---

## 5. Android release build

### 5.1 Create the release (upload) key — once
```powershell
New-Item -ItemType Directory -Force "$env:USERPROFILE\laundryhub-keys" | Out-Null
keytool -genkeypair -v -keystore "$env:USERPROFILE\laundryhub-keys\laundryhub-release.jks" `
  -alias laundryhub -keyalg RSA -keysize 2048 -validity 10000
```
- **Keep the file safe:** it lives **outside the repository**. Back it up and its passwords in a password manager.
  If it is lost, you cannot publish updates that install over the old app.
- **Register the SHA-1:** add the key's SHA-1 to the `laundryhub-android` Maps key and the Android OAuth client
  (steps 1.2 and 1.3).

### 5.2 Set the build values
1. `android-app/local.properties` (git-ignored) needs the app's own keys:
   ```properties
   MAPS_API_KEY=<laundryhub-android key>
   GOOGLE_WEB_CLIENT_ID=<OAuth web client id>
   ```
2. In the terminal, set the API address and the signing key. Passwords are typed at a prompt, never saved:
   ```powershell
   $env:LAUNDRYHUB_API_URL = "https://<name>.onrender.com/api/"
   $env:LAUNDRYHUB_KEYSTORE_FILE = "$env:USERPROFILE\laundryhub-keys\laundryhub-release.jks"
   $env:LAUNDRYHUB_KEY_ALIAS = "laundryhub"
   $env:LAUNDRYHUB_KEYSTORE_PASSWORD = Read-Host "Keystore password"
   $env:LAUNDRYHUB_KEY_PASSWORD = Read-Host "Key password"
   ```
   Instead of the environment variable, you can put `RELEASE_API_BASE_URL=https://...` in `local.properties`.

### 5.3 Build and verify
```powershell
cd android-app
.\gradlew.bat assembleRelease -PversionCode=2 -PversionName=1.0.1
$apk = "$env:USERPROFILE\.laundryhub-build\android-app\app\outputs\apk\release\app-release.apk"
$bt = (Get-ChildItem "$env:LOCALAPPDATA\Android\Sdk\build-tools" | Sort-Object Name -Descending)[0].FullName
& "$bt\apksigner.bat" verify --print-certs $apk                  # must show your release certificate
Remove-Item Env:LAUNDRYHUB_KEYSTORE_PASSWORD, Env:LAUNDRYHUB_KEY_PASSWORD
```

What the release build does differently from debug:
- **Backend address:** `BuildConfig.API_BASE_URL` is the HTTPS address (debug keeps `http://10.0.2.2:8080/api/`).
- **HTTPS only:** `usesCleartextTraffic` is `false`.
- **Signing:** the APK is signed with your release key.
- **Version:** increase `versionCode` for every build you share. Android only installs an update with a higher number.
- **Fail-fast check:** the `checkReleaseConfig` task stops the build if the HTTPS address or any signing variable is
  missing.

---

## 6. Share the APK

### Option A — GitHub Releases
```powershell
cd "<repo root>"
git tag v1.0.1; git push origin v1.0.1
gh release create v1.0.1 "$apk#LaundryHub-1.0.1.apk" `
  --title "Laundry HUB 1.0.1" --notes "Customer + rider app. Uninstall older test builds signed with a different key first."
```
- **Phones:** testers open the release page on the phone, download the APK, and allow
  **Install unknown apps** for the browser when asked.
- **Repository access:** if the repository is private, invite testers as collaborators, or attach the APK to a
  public "releases-only" repository.

### Option B — Firebase App Distribution
Use this for invite-only testing, with email invitations and update notifications.
1. One-time setup:
   - <https://console.firebase.google.com> → **Add project**. You can link the same Google Cloud project.
   - **Add app → Android**, package `com.laundryhub.app`. You do not need to add `google-services.json` for
     distribution.
   - Copy the **App ID** (`1:…:android:…`).
   - **App Distribution → Testers & Groups**: create the group `testers` and add their emails.
2. From the terminal:
   ```powershell
   npm install -g firebase-tools
   firebase login
   firebase appdistribution:distribute $apk --app "<Firebase App ID>" --groups "testers" --release-notes "1.0.1: track order, rider app"
   ```
3. Testers get an email, install the **App Tester** app once, and then receive each new build there.

---

## 7. Admin console: Windows installer

`admin-desktop/package-windows.ps1` does three things:
1. Builds the console and copies its dependencies (JavaFX for Windows, Jackson).
2. Runs `jpackage` (part of JDK 17), which links a small Java runtime. The PC does not need Java installed.
3. Wraps everything in an installer with a Start-menu entry, a desktop shortcut and an install-folder chooser.

The backend address is baked in. The console holds **no secrets**: the A2 map page and its key come from the backend.

```powershell
winget install WiXToolset.WiXToolset          # once; reopen the terminal afterwards
cd admin-desktop
.\package-windows.ps1 -ApiUrl https://<name>.onrender.com/api/ -Version 1.0.1
# → %USERPROFILE%\.laundryhub-build\laundryhub-admin\installer\Laundry HUB Admin-1.0.1.exe
```

**Variants**
- **No WiX:** `-Type app-image` makes a portable folder (`Laundry HUB Admin\Laundry HUB Admin.exe`, about 90 MB).
  Zip it and copy it to the office PC.
- **MSI for school or company PCs:** use `-Type msi`.
- **Upgrades:** run a newer installer with a higher `-Version`. It replaces the old install (fixed upgrade UUID in
  the script).
- **Pointing one PC at another backend:** set `LAUNDRYHUB_API_URL` on that PC. It overrides the baked-in address.

**Shipping it**
- **Attach to the release:**
  `gh release upload v1.0.1 "$env:USERPROFILE\.laundryhub-build\laundryhub-admin\installer\Laundry HUB Admin-1.0.1.exe"`.
- **SmartScreen warning:** the installer is not code-signed, so Windows SmartScreen shows "Windows protected your PC".
  Choose **More info → Run anyway**. Removing the warning needs a code-signing certificate (`signtool`).

---

## 8. Updating

| Change | Do |
|---|---|
| Backend code | `git push` → Render redeploys; Flyway applies new `V…` scripts on start. Rollback: Render → **Deploys** → previous deploy → **Rollback** (a migration is not undone — write a new script instead). |
| Prices / soaps / accounts | Admin console A3 / A4 (no deploy). |
| Android app | Steps 5.2–6 with a higher `-PversionCode`. |
| Admin console | Step 7 with a higher `-Version`. |
| A secret | Change it in Render → **Environment** → **Save** (redeploys). In Google Cloud you can regenerate a key and update the variable. |

---

## 9. Troubleshooting

| Symptom | Cause / fix |
|---|---|
| Render health check fails, log says `Communications link failure` | Wrong `DB_URL` host/port or the database refuses outside connections: Railway needs the **public TCP proxy** address; Aiven needs `sslMode=REQUIRED`. |
| `Access denied for user` | `DB_USER` / `DB_PASSWORD` mismatch (copy them again; no quotes in the Render field). |
| Log: `Validate failed: Migrations have failed validation` | A migration file changed after it ran. Restore it and add a new `V…` script. |
| Everyone is signed out after a deploy | `JWT_SECRET` changed or is empty (empty = random key per start). |
| "Continue with Google" returns 503 | `GOOGLE_WEB_CLIENT_ID` is not set on Render. 10 / DEVELOPER_ERROR on the phone: the Android OAuth client is missing the release SHA-1. |
| Grey map in the app | `MAPS_API_KEY` missing in `local.properties` at build time, or the key's SHA-1 / package restriction does not match the APK's certificate. |
| A2 map says "Set MAPS_JS_KEY…" | Set `MAPS_JS_KEY` on Render. "This page can't load Google Maps correctly": the key's website restriction must be `https://<name>.onrender.com/*`. |
| Address details stay empty after pinning | `GOOGLE_SERVER_KEY` missing, Geocoding API not enabled, or its IP restriction does not include Render's outbound IPs. |
| App shows "Cannot reach the server" | Release built without the right `LAUNDRYHUB_API_URL`, or the free Render service is waking up (wait a minute). |
| `assembleRelease` stops at `checkReleaseConfig` | Set the HTTPS address and all four signing variables (step 5.2). |
| `package-windows.ps1`: "WiX Toolset 3 is needed" | `winget install WiXToolset.WiXToolset`, reopen the terminal — or use `-Type app-image`. |

---

## 10. Secrets checklist

- `git status` shows no `.env`, `local.properties`, `*.jks`, `*.keystore` or `google-services.json` (all
  git-ignored).
- Every secret lives in **one** place:
  - Render variables (backend);
  - `local.properties` (Android Maps key and client id);
  - environment variables in the current terminal (signing passwords).
- Every Google key has an **API restriction** and an **application restriction** (step 1.2), plus a quota cap and
  a budget alert.
- `ADMIN_DEFAULT_PASSWORD` is cleared on Render after the first sign-in, and the admin password has been changed.
- The release keystore and its passwords are backed up outside the repository.
