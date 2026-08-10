-- ============================================================================
-- Smart Teacher (المعلم الذكي) - Supabase Database Schema
-- ============================================================================
-- Run this in the Supabase SQL Editor (Dashboard → SQL → New Query)
-- This creates all tables, indexes, Row Level Security policies, and
-- realtime publication for live sync across devices.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. Extension for UUID generation
-- ---------------------------------------------------------------------------
create extension if not exists "pgcrypto";

-- ---------------------------------------------------------------------------
-- 2. Tables
-- ---------------------------------------------------------------------------

-- Teachers: one row per teacher account
create table if not exists public.teachers (
    id          uuid primary key default gen_random_uuid(),
    username    text not null unique,
    password    text not null,           -- stored as-is; for production hash with pgcrypto
    name        text not null,
    grade       text,                    -- e.g. "الصف السادس"
    section     text,                    -- e.g. "أ"
    created_at  timestamptz not null default now()
);

-- Students: keyed by a 4-digit study code per class
create table if not exists public.students (
    id          uuid primary key default gen_random_uuid(),
    code        text not null,           -- 4-digit study code
    name        text not null,
    grade       text not null,
    section     text not null,
    teacher_id  uuid references public.teachers(id) on delete set null,
    fcm_token   text,                    -- Firebase Cloud Messaging token for push
    created_at  timestamptz not null default now(),
    unique (code, grade, section)
);

-- Subjects: each subject may have 1 or 2 terms
create table if not exists public.subjects (
    id          uuid primary key default gen_random_uuid(),
    teacher_id  uuid not null references public.teachers(id) on delete cascade,
    name        text not null,
    term_count  int not null default 1 check (term_count in (1, 2)),
    created_at  timestamptz not null default now()
);

-- Assignments / Homework: broadcast to an entire class/section
create table if not exists public.assignments (
    id          uuid primary key default gen_random_uuid(),
    teacher_id  uuid not null references public.teachers(id) on delete cascade,
    grade       text not null,
    section     text not null,
    subject     text not null,
    title       text not null,
    content     text,
    due_date    date,
    created_at  timestamptz not null default now()
);

-- Exams: title, subject, term, date, notes
create table if not exists public.exams (
    id          uuid primary key default gen_random_uuid(),
    teacher_id  uuid not null references public.teachers(id) on delete cascade,
    grade       text not null,
    section     text not null,
    subject     text not null,
    title       text not null,
    term        text not null default '1',
    exam_date   date,
    notes       text,
    created_at  timestamptz not null default now()
);

-- Grades: per student per subject per term
create table if not exists public.grades (
    id          uuid primary key default gen_random_uuid(),
    student_id  uuid not null references public.students(id) on delete cascade,
    subject     text not null,
    term        text not null default '1',
    score       numeric(5,2),
    created_at  timestamptz not null default now(),
    unique (student_id, subject, term)
);

-- Notes: broadcast to an entire class/section
create table if not exists public.notes (
    id          uuid primary key default gen_random_uuid(),
    teacher_id  uuid not null references public.teachers(id) on delete cascade,
    grade       text not null,
    section     text not null,
    title       text not null,
    content     text,
    created_at  timestamptz not null default now()
);

-- Schedule entries: day (0=Sun..4=Thu), lesson slot (0..5), subject name
create table if not exists public.schedule_entries (
    id          uuid primary key default gen_random_uuid(),
    teacher_id  uuid not null references public.teachers(id) on delete cascade,
    grade       text not null,
    section     text not null,
    day_index   int not null check (day_index between 0 and 4),
    lesson_index int not null check (lesson_index between 0 and 5),
    subject     text not null,
    created_at  timestamptz not null default now(),
    unique (teacher_id, grade, section, day_index, lesson_index)
);

-- ---------------------------------------------------------------------------
-- 3. Indexes for fast lookups
-- ---------------------------------------------------------------------------
create index if not exists idx_students_class     on public.students (grade, section);
create index if not exists idx_subjects_teacher    on public.subjects (teacher_id);
create index if not exists idx_assignments_class   on public.assignments (grade, section);
create index if not exists idx_exams_class         on public.exams (grade, section);
create index if not exists idx_grades_student      on public.grades (student_id);
create index if not exists idx_notes_class         on public.notes (grade, section);
create index if not exists idx_schedule_class      on public.schedule_entries (teacher_id, grade, section);

-- ---------------------------------------------------------------------------
-- 4. Row Level Security
-- ---------------------------------------------------------------------------
-- The mobile app uses the anon key, so we allow public read/write for all
-- tables. For a production multi-tenant deployment you should tighten these
-- policies, but for a single-school deployment this is acceptable and keeps
-- the app simple.
-- ---------------------------------------------------------------------------
alter table public.teachers         enable row level security;
alter table public.students         enable row level security;
alter table public.subjects         enable row level security;
alter table public.assignments      enable row level security;
alter table public.exams            enable row level security;
alter table public.grades           enable row level security;
alter table public.notes            enable row level security;
alter table public.schedule_entries enable row level security;

-- Permissive policies (anon role can do everything)
create policy "teachers all"         on public.teachers         for all using (true) with check (true);
create policy "students all"         on public.students         for all using (true) with check (true);
create policy "subjects all"         on public.subjects         for all using (true) with check (true);
create policy "assignments all"      on public.assignments      for all using (true) with check (true);
create policy "exams all"            on public.exams            for all using (true) with check (true);
create policy "grades all"           on public.grades           for all using (true) with check (true);
create policy "notes all"            on public.notes            for all using (true) with check (true);
create policy "schedule_entries all" on public.schedule_entries for all using (true) with check (true);

-- ---------------------------------------------------------------------------
-- 5. Realtime publication
-- ---------------------------------------------------------------------------
-- Enable realtime for every table so the app receives live updates over the
-- websocket channel and data syncs instantly across all devices.
-- ---------------------------------------------------------------------------
do $$
begin
    begin
        alter publication supabase_realtime add table public.teachers;
    exception when others then null; end;
    begin
        alter publication supabase_realtime add table public.students;
    exception when others then null; end;
    begin
        alter publication supabase_realtime add table public.subjects;
    exception when others then null; end;
    begin
        alter publication supabase_realtime add table public.assignments;
    exception when others then null; end;
    begin
        alter publication supabase_realtime add table public.exams;
    exception when others then null; end;
    begin
        alter publication supabase_realtime add table public.grades;
    exception when others then null; end;
    begin
        alter publication supabase_realtime add table public.notes;
    exception when others then null; end;
    begin
        alter publication supabase_realtime add table public.schedule_entries;
    exception when others then null; end;
end $$;

-- ---------------------------------------------------------------------------
-- 6. Helpful function: get all FCM tokens for a class/section
-- ---------------------------------------------------------------------------
-- Used by the Edge Function to send push notifications to every student in
-- a given grade + section.
-- ---------------------------------------------------------------------------
create or replace function public.get_class_tokens(p_grade text, p_section text)
returns table (fcm_token text) as $$
    select s.fcm_token
    from public.students s
    where s.grade = p_grade
      and s.section = p_section
      and s.fcm_token is not null
      and s.fcm_token <> '';
$$ language sql stable;

-- Done. Verify with:
-- select * from public.teachers;
