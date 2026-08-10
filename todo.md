# Smart Teacher (المعلم الذكي) - Android App Build Plan

## Phase 1: Project Setup
- [ ] Create Gradle project structure (AndroidManifest, build.gradle, settings.gradle)
- [ ] Configure dependencies (Supabase, Firebase FCM, Material Components, ViewModel, Coroutines)
- [ ] Set up Android resources (strings.xml, colors.xml, themes.xml RTL Arabic)
- [ ] Configure Java/Kotlin source directories

## Phase 2: Cloud Backend (Supabase)
- [ ] Design database schema (teachers, students, classes, subjects, assignments, exams, grades, notes, schedules)
- [ ] Create SQL migration files
- [ ] Configure Supabase client wrapper (real-time subscriptions)
- [ ] Set up auth + persistent session

## Phase 3: Authentication
- [ ] Role selection screen (Teacher / Student)
- [ ] Teacher login (username + password) with persistent session
- [ ] First-time teacher setup (Grade/Section selection, remembered)
- [ ] Student login (4-digit study code) with persistent session

## Phase 4: Teacher Dashboard
- [ ] Dashboard home with 7 modules
- [ ] Student Management (by grade & section)
- [ ] Subject Management (Term 1 / Term 2 toggle)
- [ ] Assignments & Homework (broadcast to class/section)
- [ ] Exams (title, subject, term, date, notes)
- [ ] Gradebook (Term 1 / Term 2)
- [ ] Notes (broadcast to class/section)
- [ ] Weekly Schedule (Sun-Thu x 6 lessons, colored cards)

## Phase 5: Student Dashboard
- [ ] Welcome message with student name
- [ ] Assignments & Homework section
- [ ] Weekly Schedule section
- [ ] Exams section
- [ ] Grades section
- [ ] Notes section
- [ ] No attendance features

## Phase 6: Real-Time Sync
- [ ] Supabase realtime subscriptions for all tables
- [ ] Teacher changes broadcast immediately to students
- [ ] Cross-device shared database

## Phase 7: Push Notifications (FCM)
- [ ] FirebaseMessagingService for background notifications
- [ ] Trigger notifications on new assignment / exam / note
- [ ] Notifications work when app closed

## Phase 8: Design & Polish
- [ ] Green theme, white cards, subtle shadows
- [ ] Arabic RTL throughout
- [ ] Fixed footer "تصميم الأستاذ محمد جيلو" on all pages
- [ ] Responsive layouts

## Phase 9: Build & Deploy
- [ ] Verify Gradle build succeeds
- [ ] Generate APK-ready project
- [ ] Push to GitHub repository
- [ ] Document setup (Supabase + Firebase config)
