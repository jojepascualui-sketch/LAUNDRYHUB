# Screen → API map

Every screen in [`docs/design/screens.json`](design/screens.json), the data it shows and the
endpoint behind each button. Base URL: `http://localhost:8080` (emulator: `http://10.0.2.2:8080`).
Interactive docs: **`/swagger-ui`** (OpenAPI JSON at `/v3/api-docs`).

**Conventions**

- **Auth:** send `Authorization: Bearer <token>` (from sign-in). 🔓 = public, otherwise the role shown.
- **Money:** JSON numbers with two decimals (`410.00`). The client formats them as `₱410.00`.
  The **server calculates every price**; the app's live total is only an estimate.
- **Errors:** [problem details](https://www.rfc-editor.org/rfc/rfc9457), for example
  `{"status":400,"detail":"Minimum of 8 kg per order","errors":{"field":"message"}}`.
  The codes used are 400 (validation or a business rule), 401 (not signed in, or the account
  is deactivated), 403 (wrong role), 404, 409 (state conflict, e.g. a booking taken by
  another rider) and 503 (server feature not configured).
- **Roles:** `CUSTOMER` → `/api/orders/**`, `/api/geocode/**` · `RIDER` → `/api/rider/**` ·
  `ADMIN`, `STAFF` → `/api/admin/**`. Pricing, soaps, users and reports are **ADMIN only**.

---

## Sign in & sign up

### 00a-sign-in: Customer / Rider / Admin sign in
| UI element | Endpoint | Notes |
|---|---|---|
| **Sign in** | `POST /api/auth/login` 🔓 | `{email, password, role}`. On the Rider tab, `email` may also be the rider's mobile number 09XXXXXXXXX ("Rider email or mobile"). `role` is the selected tab: `CUSTOMER`, `RIDER` or `ADMIN` (`ADMIN` also accepts STAFF). A wrong tab returns 403, wrong credentials return 401. The response is `{token, expiresAt, user:{role,…}}`, and `user.role` picks the next screen. |
| **Continue with Google** (Customer tab only) | `POST /api/auth/google` 🔓 | `{idToken}` from Google Sign-In. Creates the customer on first use. Rider, admin and staff e-mails get 403. |
| Remember me | none | Client-side: keep the token in secure storage. |
| Forgot password? | none | **No design** (CLAUDE.md §7). No endpoint was built. |
| "Sign up" link | none | Navigates to 00b. |

### 00b-sign-up: Create account
| UI element | Endpoint | Notes |
|---|---|---|
| **Create account** | `POST /api/auth/register` 🔓 | `{fullName, mobileNumber (09XXXXXXXXX), email, password (≥ 8), confirmPassword, acceptedTerms: true}` returns 201 and a token, the same as sign-in. A duplicate e-mail returns 409. Field errors are in `errors.*`. |

### 00c-admin-sign-in: Admin sign in
| UI element | Endpoint | Notes |
|---|---|---|
| **Sign in** (phone) | `POST /api/auth/login` with `role: "ADMIN"` | The phone only routes the user to the desktop console: `user.usesAdminConsole = true`. |
| **Sign in** (JavaFX console) | same | Then `GET /api/auth/me` for the sidebar profile ("Admin · Owner": `fullName`, `position`, `initials`). |

---

## Customer app (role CUSTOMER)

### 01-home: Home
| Data / element | Endpoint |
|---|---|
| "Wash · Dry · Fold ₱30 /kg", "Minimum load 8 kg" | `GET /api/pricing` 🔓 → `ratePerKg`, `minKg` |
| **Start laundry order** | Navigation only. The wizard data comes from `GET /api/pricing`. |
| Bottom nav **Orders** tab | `GET /api/orders/mine` (no design for the tab body, see §7) |
| Profile tab, bell | **No design.** `GET /api/auth/me` has the profile data. |

### 02-choose-service: Choose service
| Data / element | Endpoint |
|---|---|
| Walk-in / Pickup & Delivery cards ("No extra fee", "+ ₱60 delivery fee") | `GET /api/pricing` → `services[] {code, name, fee, requiresPin}` |
| Sticky **Estimated total** | `POST /api/orders/quote` `{serviceType, weightKg, soapId}` → `total`. A client estimate from `/api/pricing` is also fine. |
| **Continue** | Navigation only. `requiresPin` chooses 04a or 04b. |

### 03-weight-and-soap: Weight & fabric soap
| Data / element | Endpoint |
|---|---|
| "₱30 per kg", **−** disabled at 8 | `GET /api/pricing` → `ratePerKg`, `minKg` |
| 2×2 soap grid (name, description, "+ ₱X /kg" / "Included", tint) | `GET /api/pricing` → `soaps[]` (available only): `addOnPerKg`, `included`, `tintHex` |
| Live total | `POST /api/orders/quote`. Below `minKg` returns **400** "Minimum of 8 kg per order". |

### 04a-pin-location-delivery: Pin location (delivery)
| Data / element | Endpoint |
|---|---|
| Map centre / shop marker | `GET /api/shop` 🔓 → `latitude`, `longitude` |
| Drop / drag pin, **Use my current location** → "Location pinned" | `GET /api/geocode/reverse?lat=&lng=` → `address` (Google reverse geocoding), `distanceKm`, `deliveryRadiusKm`, `insideDeliveryArea` |
| Address details input | Sent later as `addressDetails` in `POST /api/orders` |
| "Pickup time" 2×2 chips | `GET /api/orders/slots?service=PICKUP_DELIVERY` → `[{date, label, text:"Today, 2–4 PM"}]` |
| **Continue** (needs pin + time) | Client-side check. The server re-checks both on `POST /api/orders`. |

### 04b-drop-off-walk-in: Drop-off schedule (walk-in)
| Data / element | Endpoint |
|---|---|
| "Drop off at Laundry HUB", `[SHOP ADDRESS]`, "Open [SHOP HOURS]" | `GET /api/shop` → `name`, `address`, `hoursLabel` |
| "Drop-off time" chips | `GET /api/orders/slots?service=WALK_IN` → "Today, morning" … |

### 05-review-and-pay: Review & pay
| Data / element | Endpoint |
|---|---|
| Order summary: Service; Laundry (kg × ₱rate); soap (kg × add-on); Pickup & delivery fee; **Total** | `POST /api/orders/quote` → `serviceName, billableKg, ratePerKg, laundryCost, soapName, soapAddOnPerKg, soapCost, deliveryFee, total` |
| Payment methods Cash / GCash | `GET /api/pricing` → `cashEnabled`, `gcashEnabled`. The descriptions ("Pay the rider upon delivery" / "Pay at the counter" / "Pay instantly from your e-wallet") also come back on the order as `payment.instructions`. |
| **Place order** | `POST /api/orders` `{serviceType, weightKg, soapId, scheduleDate, slotLabel, pin:{latitude,longitude}, addressDetails, paymentMethod: CASH\|GCASH, gcashNumber}` → **201** + order. The server rejects these with 400: kg < minimum; missing pin (delivery); pin outside the radius (Haversine from `SHOP_LAT`/`SHOP_LNG`); an unknown time chip; GCash without a number; a payment method that is switched off. Any price the client sends is ignored. |

### 06-order-placed: Order placed
| Data / element | Endpoint |
|---|---|
| "Order #LH-0001 · service", Weight, Fabric soap, Schedule, Payment, Total | The response of `POST /api/orders`, or `GET /api/orders/{id}` |
| **Back to home** | Navigation only |

### 07-track-order: Track order (live GPS)
| Data / element | Endpoint |
|---|---|
| Everything on the screen | `GET /api/orders/{id}/tracking`. Poll it every few seconds while the screen is open. |
| Timeline Order placed → Picked up → Washing & drying → Out for delivery → Delivered | `timeline[] {key, done, current}`, `currentStep` 0–4. Statuses map onto the five design steps: Pending → 0 (1 once the rider confirms pickup), Washing/**Ready** → 2 (Ready has no step, §7), Out for delivery → 3, Completed → 4. |
| Rider card: initials, `[RIDER NAME]`, `[PLATE NO.]`, call | `rider {name, initials, plateNumber, mobileNumber}` |
| Rider marker + "Rider is on the way · live GPS" | `liveTracking`, `riderLocation {latitude, longitude, recordedAt}`. Only shared while that rider's job is active. |
| Shop marker, customer pin | `shop`, `pin` |
| Status times | `history[] {status, changedAt, changedBy, note}` |

---

## Rider app (role RIDER)

### R1-rider-bookings: Available bookings
| Data / element | Endpoint |
|---|---|
| "Hi, [RIDER NAME]", duty switch, counters **Available / Accepted / Done today** | `GET /api/rider/summary` → `riderName, onDuty, availableCount, acceptedCount, doneTodayCount` |
| **On duty / Off duty** switch | `PATCH /api/rider/duty` `{onDuty}` → the updated summary |
| GPS updates (needed for the distances) | `POST /api/rider/location` `{lat, lng, accuracyMeters}` → 204. Send while the app is open. |
| **Available (n)** tab: cards with badge, "LH-0003 · 1.2 km", customer, address, "slot · kg", payment strip, total | `GET /api/rider/jobs/available`. Empty when off duty. Sorted by distance from the rider's last GPS fix to the first stop (customer pin for a pickup, shop for a return delivery). Fields: `typeLabel, orderNo, distanceKm, customerName, customerPin.addressDetails/geocodedAddress, scheduleText, billableKg, total, payment {method, paid, collectCash}`. |
| **My jobs (n)** tab ("Start job" / "Continue job") | `GET /api/rider/jobs/mine` (accepted + in progress). There is no design for this tab body (§7); the endpoint has the data. |
| **Accept booking** | `POST /api/rider/jobs/{id}/accept`. Only one rider can win: the loser gets **409** "This booking was already taken by another rider." (optimistic locking, `@Version`). |
| **Decline** | `POST /api/rider/jobs/{id}/decline` → 204. Hidden for this rider only. |
| Empty state text | Client-side: empty list from `/jobs/available` or `/jobs/mine`. |

### R2-rider-pickup-job / R3-rider-delivery-cash: Active job
| Data / element | Endpoint |
|---|---|
| Badge, order/distance, customer card + call, checklist, payment box | `GET /api/rider/jobs/{id}` → `steps[] {title, actionLabel, done, current}`, `customerMobile` (assigned rider only), `customerPin`, `shop` |
| **Navigate in Google Maps** | `navigationUrl` (directions to the next stop) |
| Big button (label = `currentActionLabel`) | `POST /api/rider/jobs/{id}/advance`. Optional body `{weighedKg}` on "Confirm pickup" re-prices the order (below 8 kg is billed as 8). |
| Pickup steps | Start trip → I’ve arrived → Confirm pickup → **Drop off at shop**, which sets the order to **WASHING** |
| Return delivery steps | **Picked up at shop** sets the order to **OUT_FOR_DELIVERY** → Start trip → I’ve arrived → *Cash received* → **Mark as delivered**, which sets the order to **COMPLETED** |
| **Cash received** (cash orders only, `awaitingCash: true`) | `POST /api/rider/jobs/{id}/cash-collected` marks the payment PAID, collected by this rider. `/advance` on this step returns 409. GCash orders have no cash step. |

Every automatic order status change is written to `order_status_history`, with the rider as `changedBy`.

### R4-rider-job-done: Job completed
| Data / element | Endpoint |
|---|---|
| "LH-0003 · Pickup for Ana Reyes", summary row | The response of the last `/advance` (`status: COMPLETED`, `completedAt`, `payment`) |
| **Back to bookings** | Navigation; refresh `GET /api/rider/summary` ("Done today" +1) |

### R5-rider-off-duty: Off duty
| Data / element | Endpoint |
|---|---|
| Switch off → "You’re off duty" card | `PATCH /api/rider/duty {onDuty:false}`. `/jobs/available` then returns `[]` and accept returns 409. |

---

## Admin console (roles ADMIN, STAFF)

| Shared element | Endpoint |
|---|---|
| Sidebar profile "Admin · Owner" | `GET /api/auth/me` |
| **Search orders** (header) | `GET /api/admin/orders?q=` |
| Bell | **No design** (§7) |
| Log out | Client-side: discard the token |

### A1-admin-dashboard
| Data / element | Endpoint |
|---|---|
| Orders today · Waiting to process · Kilos in today · Sales today | `GET /api/admin/dashboard` → `ordersToday, waitingToProcess, kilosToday, salesToday` |
| Order pipeline tiles | `pipeline[] {status, label, count}` (includes `CANCELLED`, which has no design; hide it or ask) |
| Recent orders table + **Open** | `recentOrders[]`. **Open** → `GET /api/admin/orders/{id}`. **View all** → A2. |

### A2-admin-orders: Orders & status update
| Data / element | Endpoint |
|---|---|
| Status filter chips with counts | `GET /api/admin/orders?status=` → `totalCount` ("All"), `counts[]` |
| Order list cards | `orders[] {orderNo, status, customerName, serviceName, weightKg, total}` |
| Detail panel: id, customer · phone, badge, Service / Weight / Fabric soap / Schedule tiles | `GET /api/admin/orders/{id}` |
| **Pinned location** map, address, coordinates, **Open in Google Maps** | `pickupLocation {latitude, longitude, addressDetails, geocodedAddress, mapsUrl}`. `null` for walk-in, which shows the walk-in note instead. |
| **Update status** buttons (current one filled) | `statusOptions[]` (walk-in has no Out for delivery). Pressing one sends `PATCH /api/admin/orders/{id}` `{status}`. **READY** offers the return delivery to riders. `{status:"CANCELLED", cancelReason}` cancels both rider jobs (no design for Cancel, §7). |
| **Assign rider** dropdown | Options: `GET /api/admin/riders`. Assign or reassign: `PATCH /api/admin/orders/{id}` `{riderId, jobType?}`. The default job is the next unfinished one. A return delivery assigned before READY goes straight to that rider when it is offered. |
| Price breakdown, "Total · GCash (paid)" | `price`, `payment {method, status}` |
| Mark payment received | `POST /api/admin/orders/{id}/payment` `{referenceNo}` (GCash needs a reference). There is no button in the design; this is the endpoint for the "(unpaid)" → "(paid)" change. |

### A3-admin-pricing: Pricing & soaps (ADMIN only)
| Data / element | Endpoint |
|---|---|
| Rates card, payment methods, Shop GCash number | `GET /api/admin/pricing` |
| **Save changes** | `PUT /api/admin/pricing` `{ratePerKg, minKg, deliveryFee, deliveryRadiusKm, cashEnabled, gcashEnabled, shopGcashNumber}`. GCash on requires the number. New prices apply only to new orders. |
| Fabric soaps table | `GET /api/admin/soaps` (includes unavailable soaps) |
| Available checkbox / edit row | `PUT /api/admin/soaps/{id}` `{name, description, addOnPerKg, available, tintHex?, sortOrder?}` |
| **+ Add soap** | `POST /api/admin/soaps` → 201 |
| (delete) | `DELETE /api/admin/soaps/{id}`. Returns 409 if any order used the soap; untick Available instead. |

### A4-admin-users: Customers, riders & staff (ADMIN only)
| Data / element | Endpoint |
|---|---|
| Tabs Customers / Riders / Staff | `GET /api/admin/users?role=CUSTOMER` · `?role=RIDER` · `?role=STAFF&role=ADMIN` |
| Columns Name, Contact, Orders / Vehicle / Role, Status | `fullName, mobileNumber, orderCount \| plateNumber \| position, status` |
| **Add customer / Add rider / Add staff** | `POST /api/admin/users` `{role, fullName, email, mobileNumber?, password?, plateNumber (rider), position (admin/staff)}` → 201 |
| **Edit** | `GET /api/admin/users/{id}`, `PUT /api/admin/users/{id}` `{fullName, mobileNumber, status, plateNumber?, position?, password?}` |
| (delete) | `DELETE /api/admin/users/{id}` deactivates the account (soft delete; orders keep their history; existing tokens stop working). You cannot deactivate yourself. |

### A5-admin-reports: Sales reports (ADMIN only)
| Data / element | Endpoint |
|---|---|
| Cash sales · GCash sales · Delivery vs walk-in | `GET /api/admin/reports?from=YYYY-MM-DD&to=YYYY-MM-DD` (default: this month) → `cashSales, gcashSales, deliveryCount, walkInCount` (+ `totalSales, deliverySales, walkInSales`) |
| Sales by fabric soap bars | `soapSales[] {soapName, orders, kilos, sales}` (highest first) |
| **Export CSV** | `GET /api/admin/reports/export.csv?from=&to=` → `text/csv` download, one row per order |

Sales are the totals of orders placed in the range that are **not cancelled**, paid or unpaid.

---

## Endpoint index

| Method | Path | Role | Screens |
|---|---|---|---|
| POST | `/api/auth/register` | 🔓 | 00b |
| POST | `/api/auth/login` | 🔓 | 00a, 00c |
| POST | `/api/auth/google` | 🔓 | 00a |
| GET | `/api/auth/me` | any | R1, A-sidebar |
| GET | `/api/pricing` | 🔓 | 01–05 |
| GET | `/api/shop` | 🔓 | 04a, 04b, 07 |
| GET | `/api/orders/slots?service=` | CUSTOMER | 04a, 04b |
| POST | `/api/orders/quote` | CUSTOMER | 02–05 |
| POST | `/api/orders` | CUSTOMER | 05 |
| GET | `/api/orders/mine` | CUSTOMER | Orders tab |
| GET | `/api/orders/{id}` | CUSTOMER | 06 |
| GET | `/api/orders/{id}/tracking` | CUSTOMER | 07 |
| GET | `/api/geocode/reverse?lat&lng` | CUSTOMER | 04a |
| GET | `/api/rider/summary` | RIDER | R1 |
| PATCH | `/api/rider/duty` | RIDER | R1, R5 |
| POST | `/api/rider/location` | RIDER | R1, 07 |
| GET | `/api/rider/jobs/available` | RIDER | R1 |
| GET | `/api/rider/jobs/mine` | RIDER | R1 |
| GET | `/api/rider/jobs/{id}` | RIDER | R2, R3, R4 |
| POST | `/api/rider/jobs/{id}/accept` | RIDER | R1 |
| POST | `/api/rider/jobs/{id}/decline` | RIDER | R1 |
| POST | `/api/rider/jobs/{id}/advance` | RIDER | R2, R3 |
| POST | `/api/rider/jobs/{id}/cash-collected` | RIDER | R3 |
| GET | `/api/admin/dashboard` | ADMIN, STAFF | A1 |
| GET | `/api/admin/orders?status&q` | ADMIN, STAFF | A2 |
| GET | `/api/admin/orders/{id}` | ADMIN, STAFF | A1 Open, A2 |
| PATCH | `/api/admin/orders/{id}` | ADMIN, STAFF | A2 |
| POST | `/api/admin/orders/{id}/payment` | ADMIN, STAFF | A2 |
| GET | `/api/admin/riders` | ADMIN, STAFF | A2 |
| GET, PUT | `/api/admin/pricing` | ADMIN | A3 |
| GET, POST | `/api/admin/soaps` | ADMIN | A3 |
| GET, PUT, DELETE | `/api/admin/soaps/{id}` | ADMIN | A3 |
| GET, POST | `/api/admin/users?role` | ADMIN | A4 |
| GET, PUT, DELETE | `/api/admin/users/{id}` | ADMIN | A4 |
| GET | `/api/admin/reports?from&to` | ADMIN | A5 |
| GET | `/api/admin/reports/export.csv?from&to` | ADMIN | A5 |

## Server configuration (environment variables)

| Variable | Used for |
|---|---|
| `LAUNDRYHUB_JWT_SECRET` | HS256 signing key, **at least 32 characters**. If blank, a random key is used and tokens stop working on restart. |
| `GOOGLE_OAUTH_CLIENT_ID` | Audience checked on Google ID tokens. If blank, `/api/auth/google` returns 503. |
| `GOOGLE_SERVER_KEY` | Google Geocoding API (reverse geocoding of pins). If blank, no address is filled in; orders still work. |
| `SHOP_LAT`, `SHOP_LNG` | Shop position for the delivery radius. If blank, the position in `shop_settings` is used; if neither is set, delivery orders return 503. |
| `SHOP_TIME_ZONE` | Time zone used for "today" (default `Asia/Manila`) |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Supabase Postgres |
