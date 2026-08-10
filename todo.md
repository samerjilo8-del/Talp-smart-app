# Smart Teacher (المعلم الذكي) - Android App Build Plan

## Phase 1: Project Setup
- [x] Create Gradle project structure (AndroidManifest, build.gradle, settings.gradle)
- [x] Configure dependencies (Supabase, Firebase FCM, Material Components, ViewModel, Coroutines)
- [x] Set up Android resources (strings.xml, colors.xml, themes.xml RTL Arabic)
- [x] Configure Java/Kotlin source directories

## Phase 2: Cloud Backend (Supabase)
- [x] Design database schema (teachers, students, classes, subjects, assignments, exams, grades, notes, schedules)
- [x] Create SQL migration files (supabase/schema.sql)
- [x] Configure Supabase client wrapper (real-time subscriptions)
- [x] Set up auth + persistent session

## Phase 3: Authentication
- [x] Role selection screen (Teacher / Student)
- [x] Teacher login (username + password) with persistent session
- [x] First-time teacher setup (Grade/Section selection, remembered)
- [x] Student login (4-digit study code) with persistent session

## Phase 4: Teacher Dashboard
- [x] Dashboard home with 7 modules
- [x] Student Management (by grade & section)
- [x] Subject Management (Term 1 / Term 2 toggle)
- [x] Assignments & Homework (broadcast to class/section)
- [x] Exams (title, subject, term, date, notes)
- [x] Gradebook (Term 1 / Term 2)
- [x] Notes (broadcast to class/section)
- [x] Weekly Schedule (Sun-Thu x 6 lessons, colored cards)

## Phase 5: Student Dashboard
- [x] Welcome message with student name
- [x] Assignments & Homework section
- [x] Weekly Schedule section
- [x] Exams section
- [x] Grades section
- [x] Notes section
- [x] No attendance features

## Phase 6: Real-Time Sync
- [x] Supabase realtime subscriptions for all tables
- [x] Teacher changes broadcast immediately to students
- [x] Cross-device shared database

## Phase 7: Push Notifications (FCM)
- [x] FirebaseMessagingService for background notifications
- [x] Trigger notifications on new assignment / exam / note
- [x] Notifications work when app closed

## Phase 8: Design & Polish
- [x] Green theme, white cards, subtle shadows
- [x] Arabic RTL throughout
- [x] Fixed footer "تصميم الأستاذ محمد جيلو" on all pages
- [x] Responsive layouts

## Phase 9: Build & Deploy
- [x] Verify Gradle project structure (98 files, all activities match manifest)
- [x] Generate APK-ready project (build.gradle, wrapper scripts, proguard)
- [x] Push to GitHub repository (samerjilo8-del/Talp-smart-app, main branch)
- [x] Document setup (Supabase + Firebase config in README.md)
