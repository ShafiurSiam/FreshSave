# FreshSave — Build Instructions for Claude Code

## What we're building
FreshSave is an Android app for a surplus-food rescue marketplace in Dhaka, Bangladesh. Cafés, bakeries and shops post leftover food at a discount, and shoppers reserve it and pick it up before closing. The full UI spec (23 annotated screens) is in `docs/App.pdf`. **Read the whole PDF before writing any code**, and match its layouts, text, colors and navigation arrows as closely as you can.

This project was created from Android Studio's "Empty Activity" (Compose) template. Build on top of it; don't recreate the project.

## Tech stack (stick to this)
- Kotlin + Jetpack Compose + Material 3
- Navigation Compose (single Activity)
- MVVM: a ViewModel per screen, exposing `StateFlow` UI state
- Repository interfaces with **fake in-memory implementations** using realistic mock data from the PDF (Tasty Treat, Star Kabab, Agora Fresh, Banani, Gulshan, Dhanmondi, ৳ prices). No backend yet. Design the interfaces so Firebase or a REST API can replace the fakes later.
- Simple manual dependency injection (an `AppContainer` class). No Hilt for now.
- Coil for images (use placeholder drawables until real photos exist)
- ZXing (`com.google.zxing:core`) to generate the pickup QR code
- Use the Gradle version catalog (`gradle/libs.versions.toml`) for all dependencies

## Design system
Create a `ui/theme` package. Approximate values taken from the PDF:
- Primary green `#1B7A4A`, dark green `#0F5132`, light green tint `#E3EFE7`
- Accent orange `#F07F2D` (discount badges, "Post surplus" button, timers)
- Background cream `#F8F5EF`, card white `#FFFFFF`, dark card `#1A2620`
- Text dark `#1A1F1C`, text muted `#6B6F6C`
- Font: Plus Jakarta Sans from Google Fonts (bold for headings), via downloadable fonts or bundled in `res/font`
- Rounded cards (16dp corners), pill-shaped chips, large rounded primary buttons
- Currency is Bangladeshi Taka: format as `৳75`, with the original price struck through
- Reusable components: `DealCard`, `DiscountBadge`, `TimerBadge`, `FilterChip`, `PrimaryButton`, `StatTile`, `SettingsRow`, `ToggleRow`, `PickupCodeCard`, `ShopperBottomBar`, `BusinessBottomBar`

## Screens (from the PDF)
**Onboarding & auth:** 1 Splash (auto-advances) · 2 Onboarding carousel (3 slides, Skip, Next) · 3 Log in / Sign up (phone or email, show password, Google/Apple buttons as stubs) · 4 Role selection (Shopper / Business)

**Shopper** (bottom nav: Home, Search, Orders, Impact, Profile): 5 Home feed (mini-map placeholder + "Ending soon" list) · 6 Deal detail (quantity stepper, Reserve now) · 7 Search & filters (distance slider, price/food-type/pickup-time chips) · 8 Checkout (bKash / Nagad / Card, mocked) · 9 Order confirmation (QR + 4-character pickup code) · 10 Orders (Active / Past tabs, live countdown) · 11 Favorites & alert toggles · 12 Impact dashboard (money saved, stat cards, monthly bar chart drawn with Canvas, level badge) · 13 Rate & review · 14 Profile & settings (Switch to business, Log out)

**Business** (bottom nav: Dashboard, Listings, Orders, Analytics, Profile): 15 Dashboard · 16 Create listing (discount slider that recalculates the sale price) · 17 Manage listings (Edit, Expire now, Extend window, Relist) · 18 Pickup queue (Mark collected) · 19 Analytics (Pro) · 20 Business profile (branches) · 21 Ratings received (with Reply)

**Shared:** 22 Notifications inbox · 23 Help & support (expandable FAQ, Chat button stub)

Follow the navigation arrows in the PDF exactly (e.g. Reserve now → Checkout → Order confirmation → View order → Orders).

## Behavior that should actually work (with fake data)
- Reserving a deal reduces "X left", creates an order, and shows it in Orders and in the business Pickup queue
- Countdown timers tick live
- Filters really filter the deal list
- Creating a listing adds it to Home and Manage listings
- "Mark collected" moves an order to Past / Collected
- Role choice and logged-in state persist across app restarts (use DataStore)
- The map is a styled placeholder for now; leave a `TODO` where Google Maps Compose will go

## How to work
Build in phases. **After each phase, run `./gradlew assembleDebug`, fix every error, then stop and summarize what was done** so I can run it on the emulator before you continue.

1. Theme, reusable components, navigation skeleton with placeholder screens, fake repositories and mock data
2. Screens 1–4 (onboarding, auth, role selection)
3. Screens 5–10 (core shopper flow: browse → reserve → pay → pickup code)
4. Screens 11–14 and 22–23
5. Screens 15–21 (business side)
6. Polish: animations, empty states, loading states, dark-mode check

Keep code organized by feature (`feature/home`, `feature/deal`, `feature/checkout`, `feature/business/...`), keep composables small, and add `@Preview`s for the main components so I can view them in Android Studio. Explain anything non-obvious in short comments, because I'm learning Kotlin as I go.

Start with Phase 1.
