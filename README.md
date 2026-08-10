# المعلم الذكي — Smart Teacher 📚

A complete Android app (Kotlin) for teachers and students with **real-time cloud sync** via Supabase and **push notifications** via Firebase Cloud Messaging (FCM). Arabic RTL, modern green theme, clean Material Design.

> **Footer on every screen:** `تصميم الأستاذ محمد جيلو`

---

## ✨ Features

### Teacher
- **Login** with username + password (persistent session).
- **First-time setup**: select Grade + Section (remembered).
- **Dashboard** with 7 modules:
  1. Student Management (add / delete students, auto 4-digit code)
  2. Subject Management (each subject has 1 or 2 terms)
  3. Assignments & Homework (broadcast to the whole class/section)
  4. Exams (title, subject, term, date, notes)
  5. Gradebook (Term 1 / Term 2, per-student score entry)
  6. Notes (broadcast to the whole class/section)
  7. Weekly Schedule (Sun–Thu × 6 lessons, colored day cards)

### Student
- **Login** with a 4-digit study code (persistent session).
- **Dashboard** with 5 sections:
  Assignments · Weekly Schedule · Exams · Grades · Notes
- No attendance features.

### Cross-cutting
- **Real-time sync**: all data lives in Supabase Postgres; changes appear on every device instantly via Supabase Realtime websockets.
- **Push notifications**: new assignments / exams / notes trigger FCM pushes that arrive even when the app is closed.
- **Multi-device**: any number of teacher / student phones share the same cloud database.

---

## 🏗️ Project structure

```
smart_teacher/
├── app/
│   ├── build.gradle
│   ├── google-services.json          ← replace with your Firebase config
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/smartteacher/app/
│       │   ├── SmartTeacherApp.kt
│       │   ├── backend/
│       │   │   ├── SupabaseConfig.kt ← put your Supabase URL + anon key here
│       │   │   ├── SessionManager.kt
│       │   │   ├── Repository.kt
│       │   │   └── model/Models.kt
│       │   ├── notification/
│       │   │   ├── SmartMessagingService.kt
│       │   │   ├── NotificationTrigger.kt
│       │   │   └── NotificationPermissionHelper.kt
│       │   └── ui/
│       │       ├── Constants.kt
│       │       ├── auth/   (Role, TeacherLogin, TeacherSetup, StudentLogin)
│       │       ├── teacher/(Dashboard, Students, Subjects, Assignments,
│       │       │           Exams, Gradebook, Notes, WeeklySchedule)
│       │       └── student/(Dashboard, Assignments, Exams, Grades,
│       │                   Notes, Schedule)
│       └── res/  (layouts, drawables, values, themes, strings — all Arabic RTL)
├── supabase/
│   ├── schema.sql                   ← run in Supabase SQL editor
│   └── edge/send_push.ts            ← Edge Function for FCM
├── build.gradle  (project)
├── settings.gradle
└── gradle.properties
```

---

## 🚀 Setup guide

### 1. Create a Supabase project
1. Go to <https://supabase.com> → **New project**.
2. Note your **Project URL** and **anon public key** (Settings → API).
3. Open **SQL Editor** → paste the contents of [`supabase/schema.sql`](supabase/schema.sql) → **Run**.
   This creates all tables, indexes, RLS policies, and enables realtime.

### 2. Configure the app with Supabase
Edit `app/src/main/java/com/smartteacher/app/backend/SupabaseConfig.kt`:

```kotlin
private const val SUPABASE_URL = "https://YOUR-PROJECT-REF.supabase.co"
private const val SUPABASE_ANON_KEY = "YOUR-ANON-PUBLIC-KEY"
```

Optionally, if you deploy the Edge Function, also set the edge function base URL.

### 3. Create a Firebase project for push notifications
1. Go to <https://console.firebase.google.com> → **Add project**.
2. Add an **Android app** with package name `com.smartteacher.app`.
3. Download `google-services.json` and replace `app/google-services.json`.
4. In **Project settings → Cloud Messaging**, copy the **Server key** (legacy).

### 4. Deploy the Edge Function
```bash
# install supabase CLI: https://supabase.com/docs/guides/cli
supabase login
supabase link --project-ref YOUR-PROJECT-REF

# set the FCM server key as a secret
supabase secrets set FCM_SERVER_KEY=your_fcm_server_key

# deploy
supabase functions deploy send_push --no-verify-jwt
```

The function URL will be:
`https://YOUR-PROJECT-REF.supabase.co/functions/v1/send_push`

Put that URL in `NotificationTrigger.kt` (`EDGE_FUNCTION_PATH`).

### 5. Build the APK
```bash
cd smart_teacher
./gradlew assembleRelease      # or assembleDebug
# APK: app/build/outputs/apk/release/app-release.apk
```

> The Gradle wrapper jar is not included in the repo (binary). Run `gradle wrapper` once with a local Gradle 8.2 install to generate it, or open the project in Android Studio which will do it automatically.

---

## 🧑‍🏫 First teacher account

Insert the first teacher directly in Supabase SQL editor so you can log in:

```sql
insert into public.teachers (username, password, name)
values ('admin', 'admin123', 'الأستاذ محمد جيلو');
```

Then log in with username `admin` / password `admin123`, pick your Grade + Section, and start adding students & subjects.

Students log in with the 4-digit code you assign when adding them.

---

## 🔄 How real-time sync works

- Every list screen uses `SwipeRefreshLayout` to pull fresh data.
- `Repository.kt` opens Supabase **Realtime** channels on the relevant tables; when any row changes, a `MutableSharedFlow` emits an event and the active screen reloads.
- Because all data is keyed by `grade + section`, every teacher phone and every student phone in the same class sees the same data instantly.

## 🔔 How push notifications work

1. Teacher creates an assignment / exam / note.
2. `NotificationTrigger.kt` POSTs `{grade, section, type, title, body}` to the `send_push` Edge Function.
3. The Edge Function calls `get_class_tokens(grade, section)` to get every student's FCM token, then sends a high-priority FCM push.
4. `SmartMessagingService.kt` receives the push (even when the app is closed) and shows a notification with the appropriate icon.
5. Tapping the notification opens the student dashboard.

---

## 🎨 Design notes
- Green primary (`#2E7D32`) with white cards and soft shadows.
- Arabic RTL throughout (`supportsRtl="true"`, `layoutDirection="rtl"`).
- Fixed footer `تصميم الأستاذ محمد جيلو` included on every screen via `view_footer.xml`.
- Weekly schedule days each have a distinct soft pastel color.

---

## 📋 Tech stack
- Kotlin, AndroidX, Material Design Components
- Supabase (Postgrest + GoTrue + Realtime) via `io.github.jan-tennert.supabase`
- Firebase Cloud Messaging
- EncryptedSharedPreferences for secure persistent sessions
- Coroutines + lifecycleScope
- ViewBinding

---

تصميم الأستاذ محمد جيلو
