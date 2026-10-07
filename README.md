# FreshSave

Android app for a surplus-food rescue marketplace in Dhaka. Cafés, bakeries and shops post leftover food at a discount; shoppers reserve it and pick it up before closing. The UI follows the 23 annotated screens in [`docs/App.pdf`](docs/App.pdf).

## Run it
1. Open the folder in Android Studio (or run `./gradlew installDebug` with an emulator running).
2. The app starts with no backend: all data is in-memory mock data (Tasty Treat, Star Kabab, Agora Fresh…), reset every time the app process restarts. Login state and role are saved with DataStore.
3. Any phone/email plus a 6+ character password logs you in. Choose **Shopper** or **Business**; you can switch later in Profile.

Handy codes for the business pickup queue: `2C77`, `4F29`, `9B14`, plus whatever code a new reservation shows.

## Stack
Kotlin · Jetpack Compose · Material 3 · Navigation Compose (type-safe routes) · MVVM with `StateFlow` · DataStore · Coil 3 · ZXing · manual DI (`AppContainer`). All versions live in `gradle/libs.versions.toml`.

## Code map
```
app/src/main/java/com/freshsave/app/
├── data/
│   ├── model/        plain data classes (Deal, Order, Review…)
│   ├── repository/   interfaces the UI depends on
│   ├── fake/         in-memory implementations + MockData
│   ├── local/        DataStore session (login + role)
│   └── AppClock.kt   one shared ticking clock for live countdowns
├── di/AppContainer.kt   builds every repository once (swap fakes for Firebase/REST here)
├── navigation/          routes, NavHost, bottom-bar logic
├── feature/             one package per screen: Screen + ViewModel
│   └── business/        dashboard, listing, listings, queue, analytics, profile, ratings
├── ui/components/       DealCard, DiscountBadge, TimerBadge, FilterChip, PrimaryButton,
│                        StatTile, SettingsRow, ToggleRow, PickupCodeCard, bottom bars, BarChart…
├── ui/theme/            colours, Plus Jakarta Sans type, light/dark themes
└── util/Format.kt       ৳ formatting, times, countdowns
```

## Still TODO
- Real backend (Firebase or REST) behind the repository interfaces; persist orders/listings
- Google Maps Compose in `MapPlaceholder` (home mini-map + full map) — needs an API key
- Real auth (phone OTP, Google, Apple) — buttons are stubs
- Real payments (bKash / Nagad / card SDKs) — checkout is mocked
- Push notifications for alerts; live chat in Help
- Payment methods, saved addresses, business hours, payout and staff screens ("coming soon")
- Unit/UI tests
