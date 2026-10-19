# Design review

Every entry of [`docs/design/screens.json`](design/screens.json) compared with the built screen.

**How it was checked**
- **Phone screens:** rendered at the design size (390 × 844 dp at 2×, the PNG size) with Robolectric.
  - Command: `.\gradlew.bat testDebugUnitTest -Pscreenshots`
  - Test classes: `AuthScreenshotTest`, `OrderScreenshotTest`, `TrackScreenshotTest`, `RiderScreenshotTest`.
- **Admin pages:** captured by `ScreenshotTour` at 1440 px wide and each PNG's height.
  - Set `LAUNDRYHUB_TOUR_DIR` and run `.\mvnw.cmd javafx:run`.
  - Data: the backend's sample data on an in-memory database.
- **Comparison:** each PNG side by side with its render.
  - Text: same wording and order.
  - Colours: the design tokens.
  - States: selection, disabled, current step.
  - Positions: edges measured along pixel columns and rows.

**"Matches?"**
- **Yes:** same layout, text, colours and states, with every edge within 4 px.
- **Mostly:** same, except the listed items, which cannot be reproduced in the test harness or have no design.

| Screen | Matches? | Differences | Fixed? |
|---|---|---|---|
| **00a-sign-in** (Customer / Rider / Admin sign in) | Yes | **Fixed:** heading, subheading, label and checkbox gaps were 2–5 dp off after the DM Sans/Bricolage switch, tuned against the PNG; the Material 3 type scale forced its own line heights (text styles now extend AppCompat); the theme's default text colour and font overrode text styles (removed from the theme). **Open:** "Forgot password?" shows a message (no reset flow is designed, CLAUDE.md §7). | Fixed; "Forgot password?" open (§7) |
| **00b-sign-up** (Create account) | Yes | **Fixed:** five fields drifted 5 dp in total because the 13 sp label line rounds down (label gap 7 dp, measured). | Fixed |
| **00c-admin-sign-in** (Admin sign in) | Yes (phone) | On the phone, Admin accounts get a dialog sending them to the desktop console, as CLAUDE.md §3 requires. The desktop console has its own sign-in in the same style (blue header with rings, logo tile, Staff email, Password, Sign in). **Fixed:** the rings were hidden behind the header fill. **Left out on desktop:** "Remember me" and "Forgot password?" (no flow). | Fixed; desktop extras left out on purpose |
| **01-home** | Mostly | **Different:** the "New order" card is 18 dp taller than in the PNG, because the PNG squeezes it to fit the fixed 844 px frame; the app keeps the card's CSS padding. The bell and Profile have no design (bell: message; Profile: name + Sign out dialog). **Fixed:** the Orders tab, which used to say "not designed yet", now opens 07 for the newest order. | Orders tab fixed; card height kept on purpose |
| **02-choose-service** | Yes | All edges within 2 px. The selected-card border and fill and the disabled Continue button match. | — |
| **03-weight-and-soap** | Yes | Within 4 px. − is disabled at 8 kg; the live total comes from the server quote. | — |
| **04a-pin-location-delivery** | Mostly | **Not visible in the render:** the Google map is blank in Robolectric because there are no Play services; on a phone the real map shows with the draggable orange pin (needs `MAPS_API_KEY`). **Added on purpose:** a "Use my current location" button (CLAUDE.md §4 requires it; not in the PNG). **Different:** Address details fills in from the server's reverse geocoding (the prototype has none); pins outside the delivery radius show a warn-coloured error. **Fixed:** chip margin (12 + 2 dp border). | Map: device only; additions on purpose |
| **04b-drop-off-walk-in** | Yes | Within 2 px. The shop card and drop-off chips match. | — |
| **05-review-and-pay** | Mostly | **Different:** "Place order" stays disabled until a valid GCash number is entered (the PNG shows GCash selected with an empty field); the server's price lines replace the prototype's local total. | On purpose (validation) |
| **06-order-placed** | Yes | Same text, summary rows and "Back to home". | — |
| **07-track-order** | Mostly | **New in this review:** the screen did not exist, so it was built (header, map with live chip, "Your rider" card with Call, five-step "Order status"). **Fixed after comparing:** the map frame was 4 dp short (210 px map plus a 2 px border each side = 214 dp) and the back button 2 dp small (44 px + 1 px border = 46 dp). **Not visible in the render:** the map is blank in Robolectric; on a phone it shows the shop, the live rider marker, the pin and the dashed route. **No design:** cancelled orders show a warn note "This order was cancelled." (§7). | Fixed; cancelled state needs a design |
| **R1-rider-bookings** | Yes | **Fixed:** the header was 2 dp too tall (CSS line boxes are 1 dp shorter for the 24 sp greeting and 22 sp counters; gaps measured). **Remaining:** each booking card is about 1 dp shorter (DM Sans 13 sp line rounding). **My jobs:** content follows the prototype (accepted jobs with Start / Continue job). | Fixed; 1 dp per card remains |
| **R2-rider-pickup-job** | Mostly | **Fixed:** the bottom bar was 20 dp short (LinearLayout ignores negative margins when sharing space, so the sheet now overlaps the map inside a frame); checklist rows were 30 dp apart instead of 40 dp. **Not visible in the render:** the map is blank in Robolectric; on a phone it shows the truck, the pin and the route. | Fixed; map: device only |
| **R3-rider-delivery-cash** | Mostly | Same fixes as R2. The cash step and "Cash received" button appear only for cash orders. The pay box is warn-coloured for cash return deliveries. | Fixed; map: device only |
| **R4-rider-job-done** | Yes | Pixel-aligned (check circle, title, summary row, "Back to bookings"). | — |
| **R5-rider-off-duty** | Yes | Pixel-aligned after the R1 header fix. | Fixed |
| **A1-admin-dashboard** | Yes | **Fixed:** the design fonts were not applied (JavaFX registers the files as "DM Sans 9pt" / "Bricolage Grotesque 96pt ExtraBold ExtraBold"); text rounding at 150 % display scale drifted 2–5 px per card (every text style now has the browser line height and pixel snapping is off); recent orders were newest first (now LH-0001 first, as designed); the avatar showed "A" (now "AD"). **Remaining:** titles look a little wider (the bundled Bricolage file is the 96 pt optical size; the browser chooses one per size); the 0.6 px letter spacing of table headers is not available in JavaFX; the design's "Sample data shown for design purposes." note is left out. | Fixed; small font differences remain |
| **A2-admin-orders** | Mostly | **Fixed:** the status badge in the detail header sat 8 px low (now top-aligned like the design); list cards were 4–5 px taller (line heights). **Not shown without a key:** the "Pinned location" map uses the Maps JavaScript API in a WebView; the map page is served by the backend (`/api/maps/order-pin`) with `MAPS_JS_KEY`; without the key it is a plain frame with a note. **Different:** coordinates are real (the PNG shows "[LAT], [LNG]"); the rider dropdown arrow is JavaFX's. | Fixed; map needs the key |
| **A3-admin-pricing** | Yes | **Fixed:** the Rates card stretched to the full row height; soap rows were 41 px instead of 45 (the browser's line box around the 20 px checkbox). All edges within 3 px. | Fixed |
| **A4-admin-users** | Yes | **Fixed:** customers were sorted by name (now oldest account first, as in the design). **Different:** the sample database has 6 customers (the PNG shows 4) and real numbers instead of "XXX". **No design:** Add / Edit open dialogs in the A3 field style; "Off duty" and "Inactive" badges use the neutral Pending colours. | Fixed; dialogs and badge colours need a design |
| **A5-admin-reports** | Yes | **Fixed:** the cards were 6 px taller (a CSS spacing rule overrode the FXML); the bars ended 3 px early; the soaps were ordered by sales instead of the soap order. Now pixel-aligned. | Fixed |

## Added features without a design (feedback chat and ratings)

Built at the user's request with existing tokens only (CLAUDE.md §1 / §7); replace them when a design exists.
Renders: `FeedbackScreenshotTest` (X1 chat, X2 empty chat, X3 rating card) and the admin tour (X4 Feedback page).

| Where | What | Entry point |
|---|---|---|
| Customer + rider app | **Feedback chat**: 07-style header, own messages as accent bubbles on the right, shop replies as white bubbles on the left with the sender, input bar like the wizard footer, accent send button. Empty state in the R5 card style; loading spinner; warn banner on errors; refreshes every 5 s. | Customer: Home → Profile → "Feedback chat". Rider: round accent button at the bottom right of R1 (kept out of the header so R1/R5 still match their PNGs). |
| 07 Track order | **"Rate your laundry"** card once the order is completed (5 stars, optional comment, "Send rating" / "Update rating") and a "Message the shop" link under the status card. | — |
| Admin console | **Feedback** page (6th sidebar item): ratings card (average, count, latest ratings), conversation list like A2's order cards (Customer / Rider badge, unread count), detail with bubbles and a reply box; refreshes every 5 s while shown. | Sidebar "Feedback". |

## States without a design (CLAUDE.md §1: same style, existing tokens only)

| Where | Loading | Empty | Error |
|---|---|---|---|
| Sign in / sign up | Button disabled while sending | — | Warn banner with the server message |
| 01 Home | Price tiles fill in when loaded | Orders tab: "You have no orders yet. Start one from Home." | Ink snackbar with "Try again" |
| 02–06 wizard | Total shows the last server quote | — | Load / pin / place-order errors as warn banners |
| 07 Track order | Accent spinner until the first load | No rider yet: rider card hidden; live chip only while GPS is shared | Warn banner; the 5 s poll retries |
| R1 / R5 bookings | Pull-to-refresh spinner, 15 s auto refresh | Prototype texts for both tabs and off duty | Warn banner (load); ink snackbar (accept / decline, including "Another rider accepted this booking first.") |
| R2–R4 active job | Accent spinner until the job loads | — | Warn banner in the sheet |
| A1–A5 console | Accent spinner next to the page title while data loads | Muted row: "No orders yet." / "No customers yet." / "No soaps yet." / "No sales yet."; A2 "No orders with this status." (design text) | Ink toast at the bottom right; expired sign-in returns to sign in |

## Open design questions (CLAUDE.md §7)

- **Cancelled orders:** no badge colour, filter chip, timeline state or Cancel button. They are shown with the Pending colours and a warn note on 07.
- **Missing screens:** the customer Orders list and Profile, the bell, and "Forgot password?".
- **Admin details:** forms for Add / Edit account and "+ Add soap"; badge colours for "Off duty" and "Inactive".
- **Rider sign-out:** riders have no sign-out control; they are signed out only when the session expires.
