# CLAUDE.md — Laundry HUB

Every session working in this repo **must** follow this file. Laundry HUB is a school
OOP Java project: a laundry-ordering system with a customer + rider Android app, a
Spring Boot REST backend, and a JavaFX admin console.

```
/backend         Spring Boot 3.5 · Java 17 · Maven (mvnw.cmd)      REST API, all business rules
/android-app     Android · Java 17 · minSdk 24 · Gradle (gradlew)  Customer app + Rider app (role-based)
/admin-desktop   JavaFX 21 · Java 17 · Maven (mvnw.cmd)            Admin / staff console
/docs/design     UI design package — SOURCE OF TRUTH, READ-ONLY
```

---

## 1. DESIGN RULE — most important

`docs/design/` is the single source of truth for **all** UI. **Never move or edit anything
in it.** Never invent a different style, layout, wording or color.

**Before building or changing any screen:**

1. **Open its PNG** in `docs/design/screens/` (use the Read tool to view it).
2. **Open its HTML** in `docs/design/prototype/` and read the inline styles for exact
   colors, sizes, radii and spacing.
   The prototype `.html` files are thin shells that call `DCRender('<Component>')`, so the
   real markup and inline styles live in **`docs/design/source/<Component>.dc.html`**
   (the same text is bundled in `prototype/assets/sources.js`). The component name for each
   screen is in the table in §3. **Customer screens 01–07 are all one component,
   `source/Main.dc.html`**: one wizard whose `renderVals()` holds the state logic (selection,
   disabled states, live total). The other customer files only set a start screen.
3. **Read its section** in `docs/design/DESIGN_SPEC.md` §3.

Use the **same text, order and behavior** as the design: same labels, same English wording
(plus "Magandang araw!" on Home), same element order, and every interactive element in the
prototype must work (selection, disabled states, live total).

**After building:** compare the result with the PNG side by side (take an emulator
screenshot with `adb exec-out screencap -p > shot.png`, or a JavaFX window screenshot) and
**write down every difference**, then fix them. Report the remaining differences to the user.
Without an emulator (this machine is short on disk space), render Android screens at the design size
(390×844 @2×) with `.\gradlew.bat testDebugUnitTest -Pscreenshots` (Robolectric, e.g.
`AuthScreenshotTest`); PNGs land in `%USERPROFILE%\.laundryhub-build\android-app\app\screenshots`.

Android UI uses the design system in `res/values/styles.xml` (PrimaryPillButton, OutlinePillButton,
DarkPillButton, TextLink, Card, SelectableCard, TextInput / `ui.widget.FormField`, StatusBadge,
`ui.widget.SegmentedControl`, CheckBox, ErrorBanner). Never add one-off styles, and never set
`android:textColor` / `android:fontFamily` in the theme: theme-level values override every
`textAppearance`.

Loading, empty and error states are not in the mockups. Add them in the same style, using
only existing tokens. If something has **no design at all**, see §7 and ask the user
rather than inventing a style.

> ⚠️ `docs/design/` also contains files from an **unrelated project ("CareFolio")**:
> `README.md`, `brand/`, and all top-level `*.html` files (`AAccounts.html`, `Main.html`,
> `MDoctorView.html`, …). **Ignore them.** The Laundry HUB files are only
> `DESIGN_SPEC.md`, `README (2).md`, `screens/`, `prototype/`, `source/` and `screens.json`.
> The Laundry HUB logo is the washing-machine icon drawn inline in the source files, not `brand/`.

---

## 2. Design tokens (copied from DESIGN_SPEC.md §1)

### Colors
| Token | Hex | Use | Android `@color/` |
|---|---|---|---|
| `bg` | `#F3F5F2` | App background | `bg` |
| `surface` | `#FFFFFF` | Cards, inputs, bottom bars | `surface` |
| `ink` | `#14212B` | Main text; dark headers (rider, admin sidebar); secondary dark buttons | `ink` |
| `muted` | `#4A5863` | Secondary text, labels | `muted` |
| `subtle` | `#6B7780` | Disabled / upcoming text | `subtle` |
| `accent` | `#1E63A8` | Primary buttons, selected states, progress, links | `accent` |
| `accentTint` | `#E4EEF8` | Icon tiles (selected-card background is `#F2F7FC`) | `accent_tint`, `selected_card_bg` |
| `border` | `#DCE2DD` | Unselected card borders; inputs use `#C8D0CA` | `border`, `input_border` |
| `divider` | `#E1E6E2` | Lines between rows | `divider` |
| `warnBg` / `warnText` | `#FFF1E8` / `#8A3A12` | 8 kg minimum notice, "Collect cash", Return delivery badge | `warn_bg`, `warn_text` |
| `pin` | `#E0662A` | Map pin | `pin` |
| `successBg` / `successText` | `#E6F2EA` / `#1D5E36` | Paid, Completed, Active, Cash icon | `success_bg`, `success_text` |
| `onDuty` | `#1D7A4A` | Rider "On duty" switch | `on_duty` |
| `disabled` | `#9AA5AD` | Disabled primary button | `disabled` |

Status badge colors (background / text). Badges always show the status **text**, never color alone:

| Status | Background | Text |
|---|---|---|
| Pending | `#EEF1EE` | `#3A4651` |
| Washing | `#E4EEF8` | `#174B80` |
| Ready | `#DFF3F0` | `#0B5B54` |
| Out for delivery | `#FFF1E8` | `#8A3A12` |
| Completed | `#E6F2EA` | `#1D5E36` |

Admin-only extras from the spec: sidebar `#14212B` (~230 px wide), active nav item background `#24333F` with an accent dot.

All of these already exist in `android-app/app/src/main/res/values/colors.xml`. In JavaFX,
put them in **one** CSS file as looked-up colors (`-lh-accent: #1E63A8;` …). Never hard-code a hex value in a layout or a class.

### Typography
- Headings & big numbers: **Bricolage Grotesque** (700–800). Fallback: any bold sans.
- Body: **DM Sans** (400/500/700). Source files: `docs/design/prototype/assets/fonts/files/` (OFL licensed).
- Sizes (phone): screen title 21, hero 30–32, section title 16–17, body 14–15, caption 12–13, totals 22–28, kilo number 48.
- Admin page title 30 px.
- Fonts are bundled, converted from woff2 to TTF with the `latin` and `latin-ext` subsets merged:
  - Android: `res/font/dm_sans.xml` (`dm_sans_regular|medium|bold.ttf`) and
    `res/font/bricolage_grotesque.xml` (`bricolage_grotesque_medium|bold|extrabold.ttf`).
    The theme defaults to `@font/dm_sans`.
  - JavaFX: `admin-desktop/src/main/resources/fonts/*.ttf`, registered by `AdminApp.loadFonts()`
    (`Font.loadFont`). CSS families are `"DM Sans"` and `"Bricolage Grotesque"`.
  - DM Sans has **no ₱ glyph** (upstream limitation), so the platform falls back to a system font for ₱
    in body text, exactly as the browser does in the prototype. Bricolage Grotesque does have ₱.

### Shape & spacing
- Phone screen: 390×844 design size (1 design px = 1 dp), side padding **22 px**.
- Card radius 18–20, big hero card 24, inputs 14, chips 14, pill buttons fully rounded.
- Touch targets **≥ 44 px** (primary buttons 52–54 px tall).
- Gap between stacked cards 12–14 px; between sections 18–22 px.
- Icons: simple outline icons (stroke 2), **no emoji**. Draw them as Android vector drawables / JavaFX SVGPath copied from the source SVGs.

### Money & rules (shown in UI, enforced by the server)
- Price per kg default **₱30**, delivery fee **₱60** (delivery only), **minimum 8 kg**.
- Soap add-ons per kg: Standard ₱0 (shown "Included"), Hypoallergenic ₱5, Premium Scented ₱8, Baby-Safe ₱6.
- Money format: `₱1,234.00` (two decimals, comma thousands).

---

## 3. Screen list (from `docs/design/screens.json`)

PNG = `docs/design/screens/<file>.png` · HTML = `docs/design/prototype/<file>.html` ·
Source = `docs/design/source/<Component>.dc.html`

| File | Title | Group | Size | Component (source) | Builds in |
|---|---|---|---|---|---|
| `00a-sign-in` | Customer / Rider / Admin sign in | Sign in & sign up | phone | `Auth` | android-app |
| `00b-sign-up` | Create account | Sign in & sign up | phone | `Signup` | android-app |
| `00c-admin-sign-in` | Admin sign in | Sign in & sign up | phone | `AdminLogin` | android-app (routes admins to desktop) + admin-desktop login |
| `01-home` | Home (start of interactive order flow) | Customer app | phone | `Main` | android-app |
| `02-choose-service` | Choose service | Customer app | phone | `Service` → `Main` | android-app |
| `03-weight-and-soap` | Weight & fabric soap | Customer app | phone | `Load` → `Main` | android-app |
| `04a-pin-location-delivery` | Pin location (delivery) | Customer app | phone | `LocationDelivery` → `Main` | android-app |
| `04b-drop-off-walk-in` | Drop-off schedule (walk-in) | Customer app | phone | `LocationWalkin` → `Main` | android-app |
| `05-review-and-pay` | Review & pay | Customer app | phone | `Payment` → `Main` | android-app |
| `06-order-placed` | Order placed | Customer app | phone | `Confirmation` → `Main` | android-app |
| `07-track-order` | Track order (live GPS) | Customer app | phone | `Tracking` → `Main` | android-app |
| `R1-rider-bookings` | Available bookings — accept / decline | Rider app | phone | `Rider` | android-app |
| `R2-rider-pickup-job` | Pickup job in progress | Rider app | phone | `RiderPickup` | android-app |
| `R3-rider-delivery-cash` | Return delivery — collect cash | Rider app | phone | `RiderDelivery` | android-app |
| `R4-rider-job-done` | Job completed | Rider app | phone | `RiderDone` | android-app |
| `R5-rider-off-duty` | Off duty | Rider app | phone | `RiderOffDuty` | android-app |
| `A1-admin-dashboard` | Admin dashboard | Admin console | desktop | `AdminDashboard` | admin-desktop |
| `A2-admin-orders` | Orders & status update | Admin console | desktop | `AdminOrders` | admin-desktop |
| `A3-admin-pricing` | Pricing & soaps | Admin console | desktop | `AdminPricing` | admin-desktop |
| `A4-admin-users` | Customers, riders & staff | Admin console | desktop | `AdminUsers` | admin-desktop |
| `A5-admin-reports` | Sales reports | Admin console | desktop | `AdminReports` | admin-desktop |

Phone PNGs are 780×1688 (2× of 390×844). Admin PNGs are 1440 px wide.

Navigation (DESIGN_SPEC §2):
```
Sign in ─┬─ Customer ─▶ Home ─▶ Service ─▶ Weight & soap ─▶ Location/Schedule ─▶ Review & pay ─▶ Order placed ─▶ Home
         │                 └─ Orders tab ─▶ Track order
         ├─ Rider ────▶ Available bookings ─▶ (Accept) Active job ─▶ Job completed ─▶ Bookings
         └─ Admin ────▶ Admin console (Dashboard · Orders · Pricing & soaps · Customers & riders · Sales reports)
Sign in ◀─▶ Sign up
```

---

## 4. Product rules

### Ordering (customer)
1. Choose service: **Walk-in** or **Pickup & Delivery**.
2. Set weight. **Minimum 8 kg.** The UI's − button is disabled at 8. The server must never
   charge less than 8 kg: a weight below 8 is billed as 8 (`billableKg = max(kg, minKg)`),
   and an order *request* below the minimum is rejected with a 400.
3. Choose fabric soap: Standard +₱0/kg, Hypoallergenic +₱5/kg, Premium Scented +₱8/kg, Baby-Safe +₱6/kg.
4. Delivery only: **pin the location on Google Maps** (draggable marker + "Use my current
   location"), plus address details. Pins outside the admin's delivery radius (default 5 km) are rejected.
5. Choose a time slot (pickup time for delivery, drop-off time for walk-in).
6. Pay by **Cash** or **GCash** (GCash requires a mobile number).

### Price formula
```
total = billableKg × ratePerKg            (default ₱30)
      + billableKg × soap.addOnPerKg
      + deliveryFee                       (default ₱60, Pickup & Delivery only)
```
Example: 10 kg, Hypoallergenic, Pickup & Delivery → 300 + 50 + 60 = **₱410.00**.

- All prices (rate per kg, minimum kg, delivery fee, delivery radius, soap add-ons and
  availability, payment methods) are **editable by ADMIN** (screen A3) and stored server-side.
- **The SERVER is the source of truth.** Clients may show a live *estimate*, but the
  backend recalculates on every order and its number is what is saved and charged.
  Never trust a price sent by a client.
- Use `BigDecimal` for money (never `double`), and round to 2 decimals with `HALF_UP`.

### Roles
`CUSTOMER`, `RIDER`, `ADMIN`, `STAFF`. Customers and riders use the Android app;
ADMIN and STAFF use the JavaFX console (staff are managed on A4's "Staff" tab).

### Order statuses
`PENDING` → `WASHING` → `READY` → `OUT_FOR_DELIVERY` → `COMPLETED`, plus `CANCELLED`.
Walk-in orders skip `OUT_FOR_DELIVERY` (READY → COMPLETED on pickup at the counter).

### Rider jobs
Every **Pickup & Delivery** order creates two jobs:
- a `PICKUP` job (customer → shop), available when the order is placed, and
- a `RETURN_DELIVERY` job (shop → customer), available when the order is `READY`.

Only **on-duty** riders see available jobs. They can **accept** or **decline** a job.
Accepting assigns it to that rider, and only one rider can win it (guard against
double-accept on the server). Declining hides the job for that rider only. Admin can
also assign a rider (A2 "Assign rider").
Job step sequences and button labels are fixed by the design (DESIGN_SPEC §3, R2/R3):
- Pickup: Head to customer → Arrived at pickup point → Laundry picked up & weighed → Dropped off at shop.
- Return delivery: Collect laundry at shop → On the way to customer → Arrived at drop-off →
  Cash collected (cash orders only) → Delivered.

---

## 5. OOP rules (graded — apply everywhere)

- Demonstrate all four pillars clearly, and mark each place with a comment on the line
  above, using exactly this form:
  - `// OOP: Encapsulation`: private fields, validated constructors and setters, no public mutable state.
  - `// OOP: Inheritance`: e.g. `abstract class User` → `Customer`, `Rider`, `Admin`, `Staff`;
    `abstract class RiderJob` → `PickupJob`, `ReturnDeliveryJob`.
  - `// OOP: Polymorphism`: overridden behavior called through the base type (e.g.
    `ServiceType.deliveryFee()`, `RiderJob.steps()`, `PaymentMethod.describe()`).
  - `// OOP: Abstraction`: abstract classes and interfaces that hide details (e.g.
    `PriceCalculator`, `PaymentMethod`, repository interfaces).
- **All fields private.** Expose behavior, not setters for everything.
- **Constructors validate** their arguments and throw `IllegalArgumentException` with a clear message
  (e.g. negative kg, null soap, blank name).
- **No magic numbers or strings.** Use named constants or enums (`MIN_KG`, `DEFAULT_RATE_PER_KG`) or values
  loaded from settings. In Android, use `dimens.xml`, `colors.xml` and `strings.xml`.
- **Javadoc on every public class, constructor and method.**
- Keep classes small and single-purpose. Prefer enums for fixed sets (roles, statuses, job types).

---

## 6. Development environment — VS Code only, Windows

- **No Android Studio**: no layout editor, wizards or Run button. Write Android layouts as
  **XML by hand**. The VS Code Java extension imports only the Maven projects (`.vscode/settings.json`).
- **JDK 17** (`C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot`). JavaFX stays on 21 LTS
  (JavaFX 25+ needs JDK 23+). No global `mvn`/`gradle`, so always use the wrappers.
- Android SDK: `%LOCALAPPDATA%\Android\Sdk`. `adb` and `emulator` are on PATH only in VS Code's
  integrated terminal. AVDs: `Pixel_4`, `Medium_Phone_API_37.0`.
- Emulator → backend: `http://10.0.2.2:8080/api/` (`BuildConfig.API_BASE_URL`).
- The repo path contains Korean (`OneDrive\문서`), which `java.exe` mangles on this machine, so
  **build output lives in `%USERPROFILE%\.laundryhub-build\`**, not `target/` or `build/` (see README,
  "Non-ASCII project path"). Keep those workarounds (Maven `ascii-build-dir` profile, the
  `android-app/build.gradle.kts` redirect, the `gradlew.bat` short path, `.mvn/jvm.config`,
  `useModulePath=false`) unless the repo moves to an ASCII path.
- `.vscode/launch.json`: "Backend: Spring Boot (launch + debug)", "Admin: JavaFX (mvnw javafx:run + attach)", "Backend + Admin".

**End every phase** with the exact commands to build, run and test what changed, for example:

```powershell
# Backend (http://localhost:8080/api/ping)
cd backend
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run

# Admin console
cd admin-desktop
.\mvnw.cmd test
.\mvnw.cmd javafx:run

# Android
cd android-app
emulator -avd Pixel_4                        # separate terminal
adb devices
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
adb shell am start -n com.laundryhub.app/.MainActivity
.\gradlew.bat testDebugUnitTest
adb logcat --pid=$(adb shell pidof com.laundryhub.app)
```

---

## 7. Known design gaps (ask the user, do not invent)

- **`CANCELLED`** has no badge color, filter chip or timeline state in the design.
- Rider **"My jobs"** tab content, the customer **Orders** and **Profile** tabs, the **bell/notifications**,
  and the **Forgot password** flow have no screens.
- The customer tracking timeline (07) has no "Ready" step. Map statuses onto the five
  design steps instead of adding one.

---

## 8. Secrets

Secrets come from **environment variables** (backend, admin) or **`android-app/local.properties`**
(Android). They are **never committed**, and `.gitignore` covers both.
- Backend: `application.yml` reads `${LAUNDRYHUB_JWT_SECRET:}`, `${GOOGLE_OAUTH_CLIENT_ID:}`,
  `${GOOGLE_SERVER_KEY:}` (Geocoding API), `${SHOP_LAT:}`/`${SHOP_LNG:}` and the `DB_*` credentials the
  same way (template: `backend/.env.example`). Never put a real value in a properties file.
- Android: `MAPS_API_KEY=...` in `android-app/local.properties` → `manifestPlaceholders["mapsApiKey"]`
  → `com.google.android.geo.API_KEY` meta-data in the manifest. `GOOGLE_WEB_CLIENT_ID=...` (the OAuth
  web client id, same value as the backend's `GOOGLE_OAUTH_CLIENT_ID`) → `BuildConfig.GOOGLE_WEB_CLIENT_ID`
  for "Continue with Google".
- Never print or log secrets, and never paste them into source, tests or docs.
