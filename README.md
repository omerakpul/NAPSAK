# NAPSAK – Real-Time Group Decision-Making App

NAPSAK helps a group decide together: one person opens a room, everyone joins with a 6-digit code (or QR), swipes through the options, and the most-liked option wins – with a raffle when there is a tie.

The project has a native **Android client** (Jetpack Compose, Clean Architecture, Hilt) and a lightweight **web client** (`docs/`, hosted on GitHub Pages). Both talk to the same **Supabase** backend (Postgres, Realtime, anonymous Auth, Storage) and stay in sync instantly. Everything runs on Supabase's free tier.

---

## Key Features

* **Real-time rooms:** lobby, ready states, vote progress and results update live on every device through Supabase Realtime.
* **Cross-platform:** players without the app can join from any mobile or desktop browser.
* **Server-enforced rules:** all writes go through `SECURITY DEFINER` Postgres functions. One vote per player, secret ballots, only the host can start/end voting, and the host can only pick a winner among the top-voted options. Clients cannot write tables directly (Row Level Security has no write policies).
* **Anonymous sign-in:** no accounts; each device gets an anonymous Supabase user.
* **Easy joining:** the invite link opens the app (Android deep link) or the web page and pre-fills the room code; the QR code is generated on device (no third-party service); tapping the room code copies it. If you leave the app mid-lobby, Home offers to take you back to your unfinished room.
* **Voting:** swipe or tap, undo the previous card, haptic feedback, countdown, live progress; the host can close voting if someone is idle.
* **Image pipeline:** images are validated (10 MB source limit), downscaled to ≤1024 px and JPEG-compressed on device, then uploaded to a public Supabase Storage bucket (`choice-images`, 2 MB cap, owner-only write).
* **Image retention:** uploaded images are intentionally not auto-deleted, because users' saved lists (stored on their device) keep referencing the URLs. Each file is ≤2 MB after compression, and an uploader can delete their own objects.
* **Leaving & cleanup:** leaving the lobby removes you from the room (the host leaving closes it); rooms live while they are used (3 days of inactivity) and abandoned ones are deleted hourly by `pg_cron`.
* **Local lists:** preset and user-created option lists are stored on device with Preferences DataStore.

---

## Architecture

Android follows **Clean Architecture + MVVM**:

```
app/src/main/java/com/napsak/app/
├── data/
│   ├── datasource/        # DataStore (username, saved lists)
│   ├── model/             # Serializable Supabase DTOs -> domain mappers
│   ├── remote/            # SupabaseSession (anonymous auth)
│   └── repository/        # RoomRepositoryImpl, ImageRepositoryImpl
├── domain/                # Pure Kotlin: models, repository interfaces, use cases
├── di/                    # Hilt modules (AppModule, SupabaseModule)
└── ui/                    # Compose screens, ViewModels, navigation, theme
```

```
supabase/migrations/       # Schema, RLS, RPCs, storage bucket, cron (apply in order)
docs/                      # Web client (supabase-js, vanilla JS/CSS)
```

Realtime events are only a *trigger*: on any change the client re-reads the whole room (rooms + participants + choices), with a slow safety refresh in case an event is missed.

---

## Tech Stack

**Android:** Kotlin 2.4, Jetpack Compose (Material 3), Navigation Compose, Hilt, Coroutines/Flow, DataStore, Coil, kotlinx.serialization, **supabase-kt** (Postgrest, Auth, Realtime, Storage) on Ktor/OkHttp, R8.

**Web:** HTML5, CSS3, vanilla JS, `@supabase/supabase-js`, GitHub Pages.

**Backend:** Supabase – Postgres 17 + RLS, RPC functions, Realtime (Postgres Changes), anonymous Auth, Storage, `pg_cron`.

---

## Testing

```bash
./gradlew testDebugUnitTest                      # JVM unit tests (use cases, DTO mapping)
./gradlew connectedDebugAndroidTest              # needs an emulator/device and network
```

`SupabaseRoomFlowTest` is an end-to-end test against the live Supabase project: two anonymous users create/join a room, go through voting and results observing each other via Realtime, leave and delete the room, and upload an image.

---

## Setup

### 1. Prerequisites
* Android Studio (current stable) with Android SDK 37, JDK 21
* A Supabase project (free plan is enough)

### 2. Create the backend
1. In the Supabase dashboard enable **Authentication → Sign In / Providers → Allow anonymous sign-ins**.
2. Apply the SQL in `supabase/migrations/` in filename order (SQL editor, or the Supabase CLI: `supabase db push`).

### 3. Configure the Android client
Add your project URL and publishable (anon) key to `local.properties` (git-ignored):

```properties
supabase.url=https://YOUR-PROJECT-REF.supabase.co
supabase.anon.key=YOUR_PUBLISHABLE_KEY
```

They are exposed as `BuildConfig.SUPABASE_URL` / `SUPABASE_ANON_KEY`. In CI, set the repository secrets `SUPABASE_URL` and `SUPABASE_ANON_KEY` instead. The publishable key is safe to ship; access is enforced by RLS and the RPC functions.

### 4. Configure the web client
Set `SUPABASE_URL` and `SUPABASE_KEY` at the top of `docs/app.js`.

### 5. Build and run
```bash
./gradlew installDebug
```

---
*Created by Ömer Emre Akpul*
