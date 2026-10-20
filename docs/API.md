# Laundry HUB REST API

Reference for the Spring Boot backend (`backend/`). Everything here is taken from the code:
controllers in `backend/src/main/java/com/laundryhub/backend/web/**`, request/response records in
`web/dto/**`, access rules in `config/SecurityConfig.java` and error handling in
`web/GlobalExceptionHandler.java`.

For the screen-by-screen view (which button calls which endpoint), see
[`SCREEN_API_MAP.md`](SCREEN_API_MAP.md). The database behind these endpoints is described in
[`ERD.md`](ERD.md).

Contents:

1. [Basics](#1-basics): base URL, JSON conventions, authentication, roles, errors
2. [Auth](#2-auth--apiauth): `AuthController`
3. [Catalog](#3-catalog--apipricing-apishop): `CatalogController`
4. [Customer orders](#4-customer-orders--apiorders): `CustomerOrderController`
5. [Geocoding](#5-geocoding--apigeocode): `GeocodeController`
6. [Rider](#6-rider--apirider): `RiderController`
7. [Admin orders and dashboard](#7-admin-orders-and-dashboard--apiadmin): `AdminOrderController`
8. [Admin pricing and soaps](#8-admin-pricing-and-soaps--apiadmin): `AdminPricingController`
9. [Admin users](#9-admin-users--apiadminusers): `AdminUserController`
10. [Admin reports](#10-admin-reports--apiadminreports): `AdminReportController`
11. [Other endpoints](#11-other-endpoints): ping, health, Swagger
12. [Configuration](#12-configuration-environment-variables): environment variables

---

## 1. Basics

### Base URL

| Client | Base URL |
|---|---|
| Admin console, browser, curl | `http://localhost:8080/api/` |
| Android emulator (`BuildConfig.API_BASE_URL`) | `http://10.0.2.2:8080/api/` |

The port comes from `PORT` (default `8080`). Interactive documentation (springdoc) is at
`/swagger-ui`, and the OpenAPI JSON is at `/v3/api-docs`.

### JSON conventions

| Java type | JSON | Example |
|---|---|---|
| `BigDecimal` (money, kilos, coordinates) | number | `410.00`, `10.00`, `14.599512` |
| `LocalDate` | string `yyyy-MM-dd` | `"2026-10-19"` |
| `LocalTime` | string `HH:mm:ss` | `"14:00:00"` |
| `LocalDateTime` (shop local time) | ISO string, no zone | `"2026-10-19T09:15:30.123"` |
| `Instant` (`expiresAt` only) | ISO string, UTC | `"2026-10-19T21:15:30Z"` |
| enums | name as a string | `"PICKUP_DELIVERY"`, `"OUT_FOR_DELIVERY"` |

Enum values used in requests and responses:

| Enum | Values |
|---|---|
| `Role` | `CUSTOMER`, `RIDER`, `ADMIN`, `STAFF` |
| `AccountStatus` | `ACTIVE`, `INACTIVE` |
| `ServiceCode` | `WALK_IN`, `PICKUP_DELIVERY` |
| `OrderStatus` | `PENDING`, `WASHING`, `READY`, `OUT_FOR_DELIVERY`, `COMPLETED`, `CANCELLED` |
| `PaymentChoice` | `CASH`, `GCASH` |
| `PaymentStatus` | `UNPAID`, `PAID`, `REFUNDED` |
| `JobType` | `PICKUP`, `RETURN_DELIVERY` |
| `JobStatus` | `WAITING`, `AVAILABLE`, `ACCEPTED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |

Money is never sent by the client. The server calculates every price with `BigDecimal`
(2 decimals, `HALF_UP`); `POST /api/orders/quote` and `POST /api/orders` return the server's numbers.

### Authentication

The API is stateless (no session, CSRF disabled). Sign in with one of these public endpoints:

- `POST /api/auth/login`
- `POST /api/auth/register`
- `POST /api/auth/google`

Each one returns an `AuthResponse` that contains a JWT. Send the JWT on every other call:

```
Authorization: Bearer <token>
```

| Token property | Value |
|---|---|
| Algorithm | HS256, key from `JWT_SECRET` (or `LAUNDRYHUB_JWT_SECRET`) (at least 32 characters; if blank, the server uses a random key per start, so tokens stop working after a restart) |
| `sub` | user id |
| `role` claim | `CUSTOMER`, `RIDER`, `ADMIN` or `STAFF`. It becomes the Spring authority `ROLE_<role>`. |
| `name` claim | full name |
| `iss` | fixed issuer set by `JwtTokenService` |
| Lifetime | `LAUNDRYHUB_TOKEN_HOURS` hours (default **12**). `expiresAt` in the response says when it ends. |

A deactivated account (`status = INACTIVE`) gets **401** on its next call even if its token has not
expired yet. Every protected service looks the account up through `AccountLookup`.

### Roles and path prefixes (`SecurityConfig`)

| Path | Who may call it |
|---|---|
| `POST /api/auth/register`, `/api/auth/login`, `/api/auth/google` | public |
| `GET /api/ping`, `/api/pricing`, `/api/shop` | public |
| `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/health`, `/error` | public |
| `/api/orders/**`, `/api/geocode/**` | `CUSTOMER` |
| `/api/rider/**` | `RIDER` |
| `/api/admin/**` | `ADMIN`, `STAFF` |
| `/api/admin/pricing`, `/api/admin/soaps/**`, `/api/admin/users/**`, `/api/admin/reports/**` | `ADMIN` only (`@PreAuthorize("hasRole('ADMIN')")` on the controller) |
| anything else, e.g. `GET /api/auth/me` | any signed-in role |

So STAFF can use A1 Dashboard and A2 Orders, but not A3 Pricing, A4 Users or A5 Reports.

### Errors

Every error body is a problem detail (RFC 7807 / RFC 9457) with content type
`application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Some fields are not valid.",
  "instance": "/api/auth/register",
  "errors": {
    "mobileNumber": "must look like 09XXXXXXXXX",
    "passwordConfirmed": "Passwords do not match"
  }
}
```

- `detail` is a sentence the app can show to the user as it is.
- `errors` (field → message) appears only for validation errors: a request body that fails Bean
  Validation, or query parameters that fail a constraint (for example `lat` out of range).

| Status | When | Source |
|---|---|---|
| **400** Bad Request | Bean Validation failed (`errors` present). A domain rule failed (`IllegalArgumentException`), e.g. "Minimum of 8 kg per order", a pin outside the delivery radius, a missing GCash number, a payment method that is switched off, an invalid time chip, a report range with `from` after `to` or longer than 366 days. A query/path value has the wrong type, e.g. `?status=WET`. | `GlobalExceptionHandler` |
| **401** Unauthorized | No token, or an invalid or expired token ("Please sign in."). Wrong e-mail or password. The account is deactivated. The Google e-mail is not verified. | `SecurityConfig`, `UnauthorizedException` |
| **403** Forbidden | Wrong role for the path ("You do not have access to this."). Sign-in on the wrong tab (e.g. a rider on the Customer tab). Google sign-in for a non-customer account. | `SecurityConfig`, `ForbiddenException` |
| **404** Not Found | The order, soap, account, rider or job does not exist, or belongs to someone else (a customer asking for another customer's order; a rider asking for a job that is neither theirs nor available). | `NotFoundException` |
| **409** Conflict | The action is not allowed in the current state (`IllegalStateException` or `ConflictException`). Examples: duplicate e-mail or soap name, deleting a soap that orders used, deactivating yourself, a booking already taken by another rider, accepting while off duty, `/advance` while the cash step is due, an invalid status change. Also returned for a lost optimistic/pessimistic lock ("Someone else changed this at the same time…") and any database constraint violation ("This conflicts with existing data."). | `GlobalExceptionHandler`, `ConflictException` |
| **503** Service Unavailable | A server feature is not configured: Google sign-in without `GOOGLE_WEB_CLIENT_ID`, or a delivery pin check when the shop position is unknown (neither `SHOP_LAT`/`SHOP_LNG` nor `shop_settings`). | `ServiceUnavailableException` |
| **500** | Unexpected error. It is logged with a stack trace; the client only sees "Something went wrong. Please try again." | `GlobalExceptionHandler` |

Successful status codes: `200` by default, `201 Created` (with a `Location` header) for register,
place order, add soap and add user, and `204 No Content` for rider location, decline, delete soap
and deactivate user.

---

## 2. Auth (`/api/auth`)

`AuthController`. Screens 00a Sign in, 00b Create account, 00c Admin sign in, plus the profile
used in the R1 header and the admin sidebar.

| Method | Path | Role | Request body | Response | Notes |
|---|---|---|---|---|---|
| POST | `/api/auth/register` | public | `RegisterRequest` | **201** `AuthResponse` | Always creates a `CUSTOMER`. 409 on a duplicate e-mail. |
| POST | `/api/auth/login` | public | `LoginRequest` | `AuthResponse` | 401 for wrong credentials or a deactivated account; 403 for the wrong tab. |
| POST | `/api/auth/google` | public | `GoogleLoginRequest` | `AuthResponse` | Customers only (403 for other roles). Creates the customer on first use. 503 if `GOOGLE_WEB_CLIENT_ID` is blank. |
| GET | `/api/auth/me` | any signed-in | none | `UserProfileResponse` | |

**`RegisterRequest`**

| Field | Type | Rules |
|---|---|---|
| `fullName` | string | required, max 120 |
| `mobileNumber` | string | required, `09XXXXXXXXX` |
| `email` | string | required, e-mail, max 190 |
| `password` | string | required, 8–72 characters |
| `confirmPassword` | string | required, must equal `password` (error key `passwordConfirmed`) |
| `acceptedTerms` | boolean | must be `true` |

**`LoginRequest`**

| Field | Type | Rules |
|---|---|---|
| `email` | string | required. On the Rider tab it may also be the rider's mobile number. |
| `password` | string | required |
| `role` | `Role` or null | The selected tab: `CUSTOMER`, `RIDER` or `ADMIN` (`ADMIN` also lets `STAFF` in). `null` accepts any role. |

**`GoogleLoginRequest`**: `idToken` (string, required): the Google ID token from the Android app.

**`AuthResponse`**

| Field | Type | Meaning |
|---|---|---|
| `token` | string | JWT for the `Authorization` header |
| `tokenType` | string | always `"Bearer"` |
| `expiresAt` | Instant | when the token stops working |
| `user` | `UserProfileResponse` | the account; `user.role` decides the next screen |

**`UserProfileResponse`**

| Field | Type | Meaning |
|---|---|---|
| `id` | long | user id |
| `role` | `Role` | |
| `fullName`, `email` | string | |
| `mobileNumber` | string or null | |
| `initials` | string | avatar initials |
| `position` | string or null | admin/staff only, e.g. "Owner" |
| `plateNumber` | string or null | riders only |
| `onDuty` | boolean or null | riders only |
| `usesAdminConsole` | boolean | `true` for ADMIN and STAFF; 00c on the phone routes these users to the desktop console |

Example:

```http
POST /api/auth/login
Content-Type: application/json

{"email": "rider1@example.com", "password": "secret123", "role": "RIDER"}
```

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresAt": "2026-10-19T21:15:30Z",
  "user": {"id": 7, "role": "RIDER", "fullName": "Rider 1", "email": "rider1@example.com",
           "mobileNumber": "09170001111", "initials": "R1", "position": null,
           "plateNumber": "ABC 1234", "onDuty": false, "usesAdminConsole": false}
}
```

---

## 3. Catalog (`/api/pricing`, `/api/shop`)

`CatalogController`. These endpoints are public. Screens 01–05 (prices and choices), the 04b shop
card and the 07 shop marker.

| Method | Path | Role | Request body | Response | Notes |
|---|---|---|---|---|---|
| GET | `/api/pricing` | public | none | `PricingResponse` | Only soaps marked available. Totals the app calculates from this are estimates. |
| GET | `/api/shop` | public | none | `ShopResponse` | |

**`PricingResponse`**

| Field | Type | Meaning |
|---|---|---|
| `ratePerKg` | decimal | "₱30 per kg" |
| `minKg` | decimal | minimum load; the − button is disabled here |
| `deliveryFee` | decimal | Pickup & Delivery fee |
| `deliveryRadiusKm` | decimal | pins farther away are rejected |
| `cashEnabled`, `gcashEnabled` | boolean | payment methods offered on 05 |
| `services` | list of `ServiceOptionResponse` | the two cards on 02 |
| `soaps` | list of `SoapResponse` | available soaps (2×2 grid on 03) |

**`ServiceOptionResponse`**: `code` (`ServiceCode`), `name` (string), `fee` (decimal, 0 for walk-in),
`requiresPin` (boolean; `true` means 04a comes next, `false` means 04b).

**`SoapResponse`**: `id` (long), `name`, `description` (string), `addOnPerKg` (decimal),
`included` (boolean, add-on is 0), `available` (boolean), `tintHex` (string `#RRGGBB`), `sortOrder` (int).

**`ShopResponse`**: `name`, `address`, `hoursLabel` (string), `latitude`, `longitude` (decimal or null
if not set), `contactNumber` (string or null).

Example `GET /api/pricing`:

```json
{
  "ratePerKg": 30.00, "minKg": 8.00, "deliveryFee": 60.00, "deliveryRadiusKm": 5.00,
  "cashEnabled": true, "gcashEnabled": true,
  "services": [
    {"code": "WALK_IN", "name": "Walk-in", "fee": 0, "requiresPin": false},
    {"code": "PICKUP_DELIVERY", "name": "Pickup & Delivery", "fee": 60.00, "requiresPin": true}
  ],
  "soaps": [
    {"id": 1, "name": "Standard", "description": "Everyday clean for regular clothes",
     "addOnPerKg": 0.00, "included": true, "available": true, "tintHex": "#E4EEF8", "sortOrder": 1},
    {"id": 2, "name": "Hypoallergenic", "description": "Gentle on sensitive skin",
     "addOnPerKg": 5.00, "included": false, "available": true, "tintHex": "#E6F2EA", "sortOrder": 2}
  ]
}
```

---

## 4. Customer orders (`/api/orders`)

`CustomerOrderController`, role **CUSTOMER**. Screens 02–07 and the Orders tab. A customer only
sees their own orders; another customer's order id returns 404.

| Method | Path | Role | Request body | Response | Notes |
|---|---|---|---|---|---|
| GET | `/api/orders/slots?service=` | CUSTOMER | none | list of `ScheduleResponse` | The four time chips on 04a/04b. `service` is required (`WALK_IN` or `PICKUP_DELIVERY`). |
| POST | `/api/orders/quote` | CUSTOMER | `QuoteRequest` | `PriceBreakdownResponse` | Server price for the live total and the 05 summary. 400 below the minimum load. |
| POST | `/api/orders` | CUSTOMER | `CreateOrderRequest` | **201** `OrderResponse` | 05 Place order. The server recalculates every price. |
| GET | `/api/orders/mine` | CUSTOMER | none | list of `OrderSummaryResponse` | Newest first. |
| GET | `/api/orders/{id}` | CUSTOMER | none | `OrderResponse` | 06 Order placed / order detail. |
| GET | `/api/orders/{id}/tracking` | CUSTOMER | none | `TrackingResponse` | 07 Track order. Poll every few seconds for live GPS. |

Slot labels: Pickup & Delivery offers `2–4 PM` and `4–6 PM` today and `9–11 AM` and `1–3 PM`
tomorrow. Walk-in offers `morning` and `afternoon`, today and tomorrow.

**`QuoteRequest`**: `serviceType` (`ServiceCode`, required), `weightKg` (decimal > 0, required),
`soapId` (long, required).

**`CreateOrderRequest`**

| Field | Type | Rules |
|---|---|---|
| `serviceType` | `ServiceCode` | required |
| `weightKg` | decimal | required, > 0, at least the minimum load (else 400) |
| `soapId` | long | required; an unknown soap returns 404 |
| `scheduleDate` | date | required; between today and 7 days ahead |
| `slotLabel` | string | required; one of the chip labels from `/slots` |
| `pin` | `{latitude, longitude}` | required for `PICKUP_DELIVERY` (latitude −90..90, longitude −180..180), ignored for walk-in; must be inside the delivery radius |
| `addressDetails` | string | optional, max 255 |
| `paymentMethod` | `PaymentChoice` | required; must be enabled on A3 |
| `gcashNumber` | string | required for `GCASH` (`09XXXXXXXXX`) |

**`PriceBreakdownResponse`**: `serviceType` (`ServiceCode`), `serviceName` (string), `billableKg`,
`ratePerKg`, `laundryCost` (= billableKg × ratePerKg), `soapName` (string), `soapAddOnPerKg`,
`soapCost` (= billableKg × addOn), `deliveryFee` (0 for walk-in), `total` (all decimals except the
two strings).

**`OrderResponse`**

| Field | Type | Meaning |
|---|---|---|
| `id` | long | |
| `orderNo` | string | `LH-0001` |
| `status`, `statusLabel` | `OrderStatus`, string | badge text, e.g. "Out for delivery" |
| `serviceType`, `serviceName` | `ServiceCode`, string | |
| `weightKg` | decimal | requested kilos; the weighed kilos after pickup |
| `billableKg` | decimal | kilos charged, never below the minimum |
| `soapName` | string | |
| `schedule` | `ScheduleResponse` | |
| `pickupLocation` | `LocationResponse` or null | delivery only |
| `payment` | `PaymentResponse` | |
| `price` | `PriceBreakdownResponse` | |
| `cancelledReason` | string or null | |
| `createdAt` | datetime | |

**`ScheduleResponse`**: `date` (date), `label` (string, the `slotLabel`), `start`, `end` (time or
null; walk-in chips have no times), `text` (string, e.g. "Today, 2–4 PM").

**`LocationResponse`**: `latitude`, `longitude` (decimal), `addressDetails` (what the customer typed),
`geocodedAddress` (what Google found), `mapsUrl` (Google Maps search link). The two addresses may be null.

**`PaymentResponse`**: `method` (`PaymentChoice`), `displayName` ("Cash"/"GCash"), `instructions`
(e.g. "Pay instantly from your e-wallet"), `status` (`PaymentStatus`), `amount`, `amountDue` (decimal),
`gcashNumber`, `referenceNo` (string or null), `paidAt` (datetime or null).

**`OrderSummaryResponse`**: `id`, `orderNo`, `status`, `statusLabel`, `serviceType`, `serviceName`,
`weightKg`, `total`, `scheduleText` (e.g. "Today, 2–4 PM"), `createdAt`.

**`TrackingResponse`**

| Field | Type | Meaning |
|---|---|---|
| `orderId`, `orderNo` | long, string | |
| `status`, `statusLabel` | `OrderStatus`, string | |
| `cancelled` | boolean | no step is current when cancelled |
| `currentStep` | int 0–4 or null | null when cancelled |
| `timeline` | list of `{key, done, current}` | `key` is `ORDER_PLACED`, `PICKED_UP`, `WASHING`, `OUT_FOR_DELIVERY` or `DELIVERED` (the five design steps; `READY` shows as `WASHING`) |
| `history` | list of `StatusHistoryResponse` | every status change |
| `rider` | `{name, initials, plateNumber, mobileNumber}` or null | rider handling the order now |
| `liveTracking` | boolean | "Rider is on the way · live GPS" applies |
| `riderLocation` | `{latitude, longitude, recordedAt}` or null | only while that rider's job is active |
| `shop`, `pin` | `{latitude, longitude}` or null | map markers; `pin` is null for walk-in |

**`StatusHistoryResponse`**: `status`, `statusLabel`, `changedAt` (datetime), `changedBy` (name, or
null for the system), `note` (string or null).

Example: 10 kg, Hypoallergenic, Pickup & Delivery, Cash.

```http
POST /api/orders
Authorization: Bearer <customer token>
Content-Type: application/json

{"serviceType": "PICKUP_DELIVERY", "weightKg": 10, "soapId": 2,
 "scheduleDate": "2026-10-19", "slotLabel": "2–4 PM",
 "pin": {"latitude": 14.599512, "longitude": 120.984222},
 "addressDetails": "Blk 4 Lot 2, Mabini St.", "paymentMethod": "CASH", "gcashNumber": null}
```

```json
HTTP/1.1 201 Created
Location: /api/orders/12

{
  "id": 12, "orderNo": "LH-0012", "status": "PENDING", "statusLabel": "Pending",
  "serviceType": "PICKUP_DELIVERY", "serviceName": "Pickup & Delivery",
  "weightKg": 10.00, "billableKg": 10.00, "soapName": "Hypoallergenic",
  "schedule": {"date": "2026-10-19", "label": "2–4 PM", "start": "14:00:00", "end": "16:00:00",
               "text": "Today, 2–4 PM"},
  "pickupLocation": {"latitude": 14.599512, "longitude": 120.984222,
                     "addressDetails": "Blk 4 Lot 2, Mabini St.", "geocodedAddress": null,
                     "mapsUrl": "https://www.google.com/maps/search/?api=1&query=14.599512,120.984222"},
  "payment": {"method": "CASH", "displayName": "Cash", "instructions": "Pay the rider upon delivery",
              "status": "UNPAID", "amount": 410.00, "amountDue": 410.00, "gcashNumber": null,
              "referenceNo": null, "paidAt": null},
  "price": {"serviceType": "PICKUP_DELIVERY", "serviceName": "Pickup & Delivery", "billableKg": 10.00,
            "ratePerKg": 30.00, "laundryCost": 300.00, "soapName": "Hypoallergenic",
            "soapAddOnPerKg": 5.00, "soapCost": 50.00, "deliveryFee": 60.00, "total": 410.00},
  "cancelledReason": null, "createdAt": "2026-10-19T09:15:30.123"
}
```

Placing a Pickup & Delivery order also creates its two rider jobs (`PICKUP` available now,
`RETURN_DELIVERY` waiting until the order is `READY`).

---

## 5. Geocoding (`/api/geocode`)

`GeocodeController`, role **CUSTOMER**. Screen 04a, after the pin is dropped or dragged.

| Method | Path | Role | Request body | Response | Notes |
|---|---|---|---|---|---|
| GET | `/api/geocode/reverse?lat=&lng=` | CUSTOMER | none | `GeocodeResponse` | `lat` −90..90, `lng` −180..180 (400 with `errors` otherwise). 503 if the shop position is unknown. |

**`GeocodeResponse`**: `latitude`, `longitude` (decimal), `address` (string or null; null when
`GOOGLE_SERVER_KEY` is blank or Google finds nothing), `distanceKm` (decimal), `deliveryRadiusKm`
(decimal), `insideDeliveryArea` (boolean).

```json
{"latitude": 14.599512, "longitude": 120.984222, "address": "Mabini St, Manila, Metro Manila",
 "distanceKm": 1.2, "deliveryRadiusKm": 5.00, "insideDeliveryArea": true}
```

---

## 6. Rider (`/api/rider`)

`RiderController`, role **RIDER**. Screens R1–R5.

| Method | Path | Role | Request body | Response | Notes |
|---|---|---|---|---|---|
| GET | `/api/rider/summary` | RIDER | none | `RiderSummaryResponse` | R1 header and counters |
| PATCH | `/api/rider/duty` | RIDER | `DutyRequest` | `RiderSummaryResponse` | R1/R5 switch |
| POST | `/api/rider/location` | RIDER | `RiderLocationRequest` | **204** | GPS fix for distances (R1) and the live map (07) |
| GET | `/api/rider/jobs/available` | RIDER | none | list of `RiderJobResponse` | **Empty list when off duty.** Excludes jobs this rider declined. Nearest first. |
| GET | `/api/rider/jobs/mine` | RIDER | none | list of `RiderJobResponse` | Accepted and in-progress jobs, newest accepted first |
| GET | `/api/rider/jobs/{id}` | RIDER | none | `RiderJobResponse` | 404 unless the job is this rider's or still `AVAILABLE` |
| POST | `/api/rider/jobs/{id}/accept` | RIDER | none | `RiderJobResponse` | **409** "This booking was already taken by another rider." when someone was first; 409 when off duty |
| POST | `/api/rider/jobs/{id}/decline` | RIDER | none | **204** | Hides the job for this rider only. 409 if it is no longer available. |
| POST | `/api/rider/jobs/{id}/advance` | RIDER | `AdvanceJobRequest` (optional) | `RiderJobResponse` | Completes the current step. **409** when the next step is "Cash collected". |
| POST | `/api/rider/jobs/{id}/cash-collected` | RIDER | none | `RiderJobResponse` | R3 "Cash received". 409 if the job is not waiting for cash. |

Rider rules enforced by `RiderJobService` and the `RiderJob` domain classes:

- **Off duty:** `/jobs/available` returns `[]` and `/accept` returns 409 "Go on duty to accept bookings."
- **Only one rider wins a booking.** `rider_jobs.version` is an optimistic lock (`@Version`). When two
  riders accept at the same time, the second commit fails and gets **409** with the "already taken"
  message. A job that is no longer `AVAILABLE` gives the same 409.
- **Decline** writes a row to `rider_job_declines`. Other riders still see the job.
- **Steps** (fixed by the design): Pickup is Head to customer → Arrived at pickup point → Laundry
  picked up & weighed → Dropped off at shop (the order becomes `WASHING`). Return delivery is Collect
  laundry at shop (order becomes `OUT_FOR_DELIVERY`) → On the way to customer → Arrived at drop-off →
  Cash collected (cash orders only) → Delivered (order becomes `COMPLETED`). Each automatic status
  change is written to `order_status_history` with the rider as `changedBy`.
- **Cash step:** while `awaitingCash` is `true`, `/advance` returns **409** "Confirm the cash first
  with "Cash received"." and the app must call `/cash-collected`, which marks the payment `PAID`
  and records the rider as collector.
- **Weighing:** `weighedKg` is accepted only on the "Laundry picked up & weighed" step (400 otherwise).
  It re-prices the order, and a load below the minimum is billed as the minimum.

**`DutyRequest`**: `onDuty` (boolean, required).

**`RiderLocationRequest`**: `lat` (decimal −90..90, required), `lng` (decimal −180..180, required),
`accuracyMeters` (decimal ≥ 0, optional).

**`AdvanceJobRequest`** (body optional): `weighedKg` (decimal > 0).

**`RiderSummaryResponse`**: `riderName`, `initials`, `plateNumber` (string), `onDuty` (boolean),
`availableCount`, `acceptedCount`, `doneTodayCount` (long).

**`RiderJobResponse`**

| Field | Type | Meaning |
|---|---|---|
| `id`, `orderId` | long | |
| `orderNo` | string | |
| `type`, `typeLabel` | `JobType`, string | "Pickup" / "Return delivery" |
| `status` | `JobStatus` | |
| `distanceKm` | decimal or null | from the rider's last GPS fix to the first stop (customer for pickup, shop for return delivery) |
| `customerName`, `customerInitials` | string | |
| `customerMobile` | string or null | only for the assigned rider |
| `customerPin` | `LocationResponse` | |
| `shop` | `{latitude, longitude}` or null | |
| `navigationUrl` | string or null | Google Maps directions to the next stop; null when completed |
| `scheduleText` | string | "Today, 4–6 PM" |
| `billableKg`, `total` | decimal | |
| `payment` | `{method, paid, collectCash, amountDue}` | payment strip |
| `steps` | list of `{title, actionLabel, done, current}` | checklist |
| `currentStepIndex` | int | index of the next step (= steps done) |
| `currentActionLabel` | string or null | big button text; null when completed |
| `awaitingCash` | boolean | next step is "Cash collected" |
| `acceptedAt`, `completedAt` | datetime or null | |

Example: a second rider accepts the same booking.

```http
POST /api/rider/jobs/31/accept
Authorization: Bearer <rider token>
```

```json
HTTP/1.1 409 Conflict
Content-Type: application/problem+json

{"type": "about:blank", "title": "Conflict", "status": 409,
 "detail": "This booking was already taken by another rider.", "instance": "/api/rider/jobs/31/accept"}
```

---

## 7. Admin orders and dashboard (`/api/admin`)

`AdminOrderController`, roles **ADMIN and STAFF**. Screens A1 Dashboard and A2 Orders.

| Method | Path | Role | Request body | Response | Notes |
|---|---|---|---|---|---|
| GET | `/api/admin/dashboard` | ADMIN, STAFF | none | `DashboardResponse` | A1 |
| GET | `/api/admin/orders?status=&q=` | ADMIN, STAFF | none | `AdminOrderListResponse` | A2 list. Both parameters optional. |
| GET | `/api/admin/orders/{id}` | ADMIN, STAFF | none | `AdminOrderDetailResponse` | A2 detail panel |
| PATCH | `/api/admin/orders/{id}` | ADMIN, STAFF | `AdminOrderUpdateRequest` | `AdminOrderDetailResponse` | Update status and/or assign a rider |
| POST | `/api/admin/orders/{id}/payment` | ADMIN, STAFF | `ConfirmPaymentRequest` (optional) | `AdminOrderDetailResponse` | Mark payment received |
| GET | `/api/admin/riders` | ADMIN, STAFF | none | list of `RiderOptionResponse` | "Assign rider" dropdown, sorted by name |

Query parameters of `GET /api/admin/orders`:

| Param | Type | Meaning |
|---|---|---|
| `status` | `OrderStatus` | filter chip; omit for "All". An unknown value returns 400. |
| `q` | string | "Search orders": case-insensitive match on order number or customer name |

Notes on `PATCH /api/admin/orders/{id}`:

- Setting `READY` offers the return-delivery job to riders.
- Setting `CANCELLED` (with `cancelReason`) cancels both rider jobs.
- An invalid status change returns 409, e.g. "Order LH-0003 is already Completed".
- `riderId` assigns or reassigns a rider to `jobType`, or to the next unfinished job when `jobType`
  is null. The response is 404 for an unknown rider, and 409 for a walk-in order ("Walk-in orders
  have no rider jobs.") or when no job is open.

**`AdminOrderUpdateRequest`**: `status` (`OrderStatus` or null), `cancelReason` (string, max 255),
`riderId` (long or null), `jobType` (`JobType` or null).

**`ConfirmPaymentRequest`** (body optional): `referenceNo` (string, max 40). The reference is required for GCash.

**`DashboardResponse`**: `ordersToday`, `waitingToProcess` (long), `kilosToday`, `salesToday`
(decimal), `pipeline` (list of `StatusCount`, including `CANCELLED`), `recentOrders` (list of `AdminOrderRow`).

**`StatusCount`**: `status` (`OrderStatus`), `label` (string), `count` (long).

**`AdminOrderListResponse`**: `totalCount` (long, "All (n)"), `counts` (list of `StatusCount`),
`orders` (list of `AdminOrderRow`).

**`AdminOrderRow`**: `id`, `orderNo`, `customerName`, `customerMobile`, `serviceType`, `serviceName`,
`weightKg`, `paymentMethod` (`PaymentChoice`), `paymentName`, `paid` (boolean), `total`, `status`,
`statusLabel`, `createdAt`.

**`AdminOrderDetailResponse`**

| Field | Type |
|---|---|
| `order` | `AdminOrderRow` |
| `soapName` | string |
| `schedule` | `ScheduleResponse` |
| `pickupLocation` | `LocationResponse` or null (walk-in) |
| `price` | `PriceBreakdownResponse` |
| `payment` | `PaymentResponse` |
| `statusOptions` | list of `OrderStatus` valid for this service, in order (walk-in has no `OUT_FOR_DELIVERY`; `CANCELLED` is not listed) |
| `jobs` | list of `AdminJobResponse` |
| `history` | list of `StatusHistoryResponse` |

**`AdminJobResponse`**: `id` (long), `type` (`JobType`), `typeLabel` (string), `status` (`JobStatus`),
`riderId` (long or null), `riderName` (string or null), `currentStepIndex`, `stepCount` (int),
`currentStep` (string or null).

**`RiderOptionResponse`**: `id` (long), `fullName`, `plateNumber` (string), `onDuty` (boolean), `activeJobs` (long).

Example:

```http
PATCH /api/admin/orders/12
Authorization: Bearer <admin or staff token>
Content-Type: application/json

{"status": "READY"}
```

The response is the updated `AdminOrderDetailResponse`, with `order.status = "READY"` and the
`RETURN_DELIVERY` job now `AVAILABLE`.

---

## 8. Admin pricing and soaps (`/api/admin`)

`AdminPricingController`, role **ADMIN only**. Screen A3 Pricing & soaps. New prices apply only to
new orders, because every order stores its own price snapshot.

| Method | Path | Role | Request body | Response | Notes |
|---|---|---|---|---|---|
| GET | `/api/admin/pricing` | ADMIN | none | `PricingSettingsResponse` | Form plus all soaps |
| PUT | `/api/admin/pricing` | ADMIN | `PricingUpdateRequest` | `PricingSettingsResponse` | "Save changes" |
| GET | `/api/admin/soaps` | ADMIN | none | list of `SoapResponse` | Includes unavailable soaps |
| GET | `/api/admin/soaps/{id}` | ADMIN | none | `SoapResponse` | 404 if unknown |
| POST | `/api/admin/soaps` | ADMIN | `SoapRequest` | **201** `SoapResponse` | 409 on a duplicate name |
| PUT | `/api/admin/soaps/{id}` | ADMIN | `SoapRequest` | `SoapResponse` | Edit row / Available checkbox |
| DELETE | `/api/admin/soaps/{id}` | ADMIN | none | **204** | 409 if any order used the soap (untick Available instead) |

**`PricingUpdateRequest`**: `ratePerKg` (> 0), `minKg` (> 0), `deliveryFee` (≥ 0), `deliveryRadiusKm`
(> 0) (all decimals, required), `cashEnabled`, `gcashEnabled` (boolean, required), `shopGcashNumber`
(string `09XXXXXXXXX`; required when GCash is on).

**`PricingSettingsResponse`**: `ratePerKg`, `minKg`, `deliveryFee`, `deliveryRadiusKm`, `cashEnabled`,
`gcashEnabled`, `shopGcashNumber`, `updatedAt` (datetime), `soaps` (list of `SoapResponse`).

**`SoapRequest`**: `name` (required, max 60), `description` (required, max 120), `addOnPerKg`
(decimal ≥ 0, required), `available` (boolean, required), `tintHex` (optional `#RRGGBB`, default
`#E4EEF8`), `sortOrder` (optional int ≥ 0, default after the last soap).

```http
PUT /api/admin/pricing
{"ratePerKg": 30, "minKg": 8, "deliveryFee": 60, "deliveryRadiusKm": 5,
 "cashEnabled": true, "gcashEnabled": true, "shopGcashNumber": "09171234567"}
```

---

## 9. Admin users (`/api/admin/users`)

`AdminUserController`, role **ADMIN only**. Screen A4 Customers, riders & staff.

| Method | Path | Role | Request body | Response | Notes |
|---|---|---|---|---|---|
| GET | `/api/admin/users?role=` | ADMIN | none | list of `UserResponse` | Sorted by name. `role` may repeat. |
| GET | `/api/admin/users/{id}` | ADMIN | none | `UserResponse` | 404 if unknown |
| POST | `/api/admin/users` | ADMIN | `UserCreateRequest` | **201** `UserResponse` | 409 on a duplicate e-mail |
| PUT | `/api/admin/users/{id}` | ADMIN | `UserUpdateRequest` | `UserResponse` | The e-mail cannot change |
| DELETE | `/api/admin/users/{id}` | ADMIN | none | **204** | Soft delete: sets `INACTIVE`, orders keep their history, existing tokens stop working. 409 when deactivating yourself. |

Query parameter `role` (one or more `Role` values; omit for all accounts):

| Tab | Query |
|---|---|
| Customers | `?role=CUSTOMER` |
| Riders | `?role=RIDER` |
| Staff | `?role=STAFF&role=ADMIN` |

**`UserCreateRequest`**: `role` (`Role`, required), `fullName` (required, max 120), `email` (required,
e-mail, max 190), `mobileNumber` (optional `09XXXXXXXXX`), `password` (optional, 8–72; without one
the account cannot sign in yet), `plateNumber` (max 20, required for riders), `position` (max 60,
required for admin/staff).

**`UserUpdateRequest`**: `fullName` (required), `mobileNumber` (null clears it), `status`
(`AccountStatus`, required), `plateNumber` (riders), `position` (admin/staff), `password` (null keeps it).

**`UserResponse`**: `id`, `role`, `fullName`, `email`, `mobileNumber`, `initials`, `status`
(`AccountStatus`), `orderCount` (customers, else null), `plateNumber`, `onDuty` (riders, else null),
`position` (admin/staff, else null), `createdAt`.

```http
POST /api/admin/users
{"role": "RIDER", "fullName": "Rider 3", "email": "rider3@example.com",
 "mobileNumber": "09170003333", "password": "changeme1", "plateNumber": "XYZ 987", "position": null}
```

---

## 10. Admin reports (`/api/admin/reports`)

`AdminReportController`, role **ADMIN only**. Screen A5 Sales reports. Sales are the totals of
orders placed in the range that are **not cancelled**, paid or unpaid.

| Method | Path | Role | Request body | Response | Notes |
|---|---|---|---|---|---|
| GET | `/api/admin/reports?from=&to=` | ADMIN | none | `ReportResponse` | |
| GET | `/api/admin/reports/export.csv?from=&to=` | ADMIN | none | `text/csv` file | "Export CSV" |

Query parameters (both optional, ISO dates `yyyy-MM-dd`, inclusive):

| Param | Default | Rule |
|---|---|---|
| `from` | first day of the current month | must be on or before `to` (else 400) |
| `to` | today | range at most 366 days (else 400) |

**`ReportResponse`**: `from`, `to` (date), `orderCount` (long), `totalSales`, `cashSales`, `gcashSales`
(decimal), `deliveryCount`, `walkInCount` (long), `deliverySales`, `walkInSales` (decimal),
`soapSales` (list of `{soapName, orders, kilos, sales}`, highest sales first).

**CSV export**

- `Content-Type: text/csv;charset=UTF-8`. The body starts with a UTF-8 byte-order mark so Excel opens
  it correctly.
- `Content-Disposition: attachment; filename="laundryhub-sales-<from>-to-<to>.csv"`.
- Header row, then one row per order: `Order, Placed, Customer, Service, Status, Weight kg,
  Billable kg, Fabric soap, Payment, Payment status, Laundry, Soap add-on, Delivery fee, Total`
  (`Placed` is formatted `yyyy-MM-dd HH:mm`).

```http
GET /api/admin/reports?from=2026-10-01&to=2026-10-19
```

```json
{"from": "2026-10-01", "to": "2026-10-19", "orderCount": 42, "totalSales": 15230.00,
 "cashSales": 9180.00, "gcashSales": 6050.00, "deliveryCount": 25, "walkInCount": 17,
 "deliverySales": 10120.00, "walkInSales": 5110.00,
 "soapSales": [{"soapName": "Standard", "orders": 20, "kilos": 200.00, "sales": 6600.00}]}
```

---

## 11. Other endpoints

| Method | Path | Role | Response |
|---|---|---|---|
| GET | `/api/ping` | public | `{"status": "ok", "service": "laundryhub-backend"}` (`PingController`) |
| GET | `/actuator/health` | public | Spring Boot health (`health` and `info` are exposed) |
| GET | `/swagger-ui`, `/v3/api-docs` | public | OpenAPI UI and JSON |

---

## 12. Configuration (environment variables)

Read by `backend/src/main/resources/application.yml` (via `LaundryHubProperties`), by
`LaundryHubApplication` and by `AdminAccountSeeder`. For local development they can be placed in
`backend/.env` (git-ignored; template `backend/.env.example`). Real environment variables win.
**Never commit real values.**

| Variable | Meaning | Default |
|---|---|---|
| `DB_URL` | JDBC URL of the Supabase Postgres database (Session pooler) | none (required) |
| `DB_USER` | database user | none (required) |
| `DB_PASSWORD` | database password | none (required) |
| `FLYWAY_LOCATIONS` | migration folders; set to `classpath:db/migration` to skip the sample data | `classpath:db/migration,classpath:db/sample-data` |
| `PORT` | HTTP port | `8080` |
| `JWT_SECRET` (alias `LAUNDRYHUB_JWT_SECRET`) | HS256 signing key, at least 32 characters (startup fails if shorter) | blank: random key per start |
| `LAUNDRYHUB_TOKEN_HOURS` | token lifetime in hours | `12` |
| `GOOGLE_WEB_CLIENT_ID` (alias `GOOGLE_OAUTH_CLIENT_ID`) | audience checked on Google ID tokens ("Continue with Google") | blank: `/api/auth/google` returns 503 |
| `GOOGLE_SERVER_KEY` | server key for the Google Geocoding API (address from the pin) | blank: no address lookup; orders still work |
| `SHOP_LAT`, `SHOP_LNG` | shop position for the delivery radius | blank: use `shop_settings`; if neither is set, pin checks return 503 |
| `SHOP_TIME_ZONE` | JVM default time zone, used for "today" | `Asia/Manila` |
| `ADMIN_DEFAULT_EMAIL` | e-mail of the first admin, created at startup when no admin exists | `admin@laundryhub.ph` |
| `ADMIN_DEFAULT_PASSWORD` | password of that first admin (BCrypt-hashed, at least 8 characters) | blank: no admin is created (a warning is logged) |

## Feedback chat and ratings (no design yet)

Customers and riders each have **one conversation** with the shop; admin and staff answer from the console's
Feedback page. Clients poll every 5 s with `after=<last message id>`. Customers can rate a **completed** order 1–5 stars.

| Method | Path | Role | Request | Response | Notes |
|---|---|---|---|---|---|
| GET | `/api/feedback/messages?after=` | CUSTOMER, RIDER | — | `ChatResponse {threadId, messages[]}` | Without `after`: the latest 200 messages; with `after`: only newer ones. Opening marks the shop's replies read. `threadId` is null before the first message. |
| POST | `/api/feedback/messages` | CUSTOMER, RIDER | `{body}` (1–1000 chars) | 201 `ChatMessageResponse` | Creates the conversation on the first message. |
| GET | `/api/feedback/unread` | CUSTOMER, RIDER | — | `{"unread": n}` | Shop replies not opened yet. |
| GET | `/api/orders/{id}/rating` | CUSTOMER | — | `RatingResponse` or **204** | 204 = not rated yet; 404 for someone else's order. |
| PUT | `/api/orders/{id}/rating` | CUSTOMER | `{stars 1–5, comment ≤ 500}` | `RatingResponse` | Creates or changes the rating. **409** while the order is not completed. |
| GET | `/api/admin/feedback/threads` | ADMIN, STAFF | — | `ChatThreadResponse[]` | Conversations with at least one message, newest activity first: `id, userId, userName, initials, role, lastMessage` (80 chars), `lastMessageAt, unread`. |
| GET | `/api/admin/feedback/threads/{id}/messages?after=` | ADMIN, STAFF | — | `ChatResponse` | Marks the user's messages read by the shop. 404 for an unknown conversation. |
| POST | `/api/admin/feedback/threads/{id}/messages` | ADMIN, STAFF | `{body}` | 201 `ChatMessageResponse` | Reply as the shop. |
| GET | `/api/admin/feedback/ratings` | ADMIN, STAFF | — | `RatingSummaryResponse {average, count, latest[]}` | `average` rounded to 1 decimal (HALF_UP), null without ratings; latest 10. |

`ChatMessageResponse`: `id, side` (`USER` / `SHOP`), `senderName, mine` (written by the viewer's side), `body, sentAt`.
`RatingResponse`: `orderId, orderNo, customerName, stars, comment, ratedAt`. Admin and staff calling
`/api/feedback/**` get 403 ("Admin and staff answer feedback from the console.").

## Map page and health (deployment)

| Method | Path | Role | Response | Notes |
|---|---|---|---|---|
| GET | `/api/maps/order-pin?lat=&lng=` | public | `text/html` | A2 "Pinned location" page for the admin console's WebView: Google map with the design's orange pin. Uses `MAPS_JS_KEY`, so the key stays on the server and can be restricted to this site's address. Without a key the page shows the plain map frame. Out-of-range coordinates → 400. `Cache-Control: no-store`. |
| GET | `/actuator/health` | public | `{"status":"UP"}` | Includes the database; the host's health check (Render `healthCheckPath`, Docker `HEALTHCHECK`). 503 with `DOWN` when the database is unreachable. |
| GET | `/actuator/health/liveness`, `/actuator/health/readiness` | public | `{"status":"UP"}` | Probes for hosts that use them. |
| GET | `/api/ping` | public | `{"status":"ok","service":"laundryhub-backend"}` | Quick reachability check (no database). |

Deployment variables added for hosting (see [DEPLOYMENT.md](DEPLOYMENT.md)): `MAPS_JS_KEY` (above), and the reserved
`PAYMONGO_SECRET_KEY` / `PAYMONGO_WEBHOOK_SECRET` (online GCash checkout is not implemented yet; the backend does not
read them).
