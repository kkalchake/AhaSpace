-- Week 18: courses gains is_public (non-null) plus nullable source/insights
-- fields, so the pilot curriculum can be shown on a public, unauthenticated
-- demo page without exposing every course to every visitor.
--
-- Ordering: run this after 001-phase-hierarchy-reset.sql. It is independent
-- of that script's DROP/reseed cycle - safe to run before a reseed (the
-- reseed's find-or-create block sets these columns on the row it creates) or
-- after one (the backfill below brings an existing row into the same state).
--
-- Why is_public can't be added NOT NULL directly:
-- Postgres rejects "ADD COLUMN ... NOT NULL" outright on a table with
-- existing rows (no default to satisfy the constraint for rows already
-- there), the same reason 001 and Week 17's 001 use add-nullable /
-- backfill / constrain-after instead of a single ALTER.
--
-- Why source_name/source_url/source_license/insights stay nullable:
-- they only apply to courses that were sourced from an external attributed
-- curriculum (currently just the pilot). ddl-auto: update is perfectly able
-- to add nullable columns on its own, so hand-running this script for those
-- four is not strictly required - they're included here so a database that's
-- skipped straight to this script (rather than restarting the app first)
-- still ends up fully migrated in one pass.
--
-- Idempotent: IF NOT EXISTS on every ADD COLUMN, the is_public backfill only
-- touches rows still NULL, SET NOT NULL / SET DEFAULT are no-ops when already
-- set, and the final pilot-course UPDATE is safe to repeat (it just re-sets
-- the same values). Running this against a fresh database where Hibernate
-- already created every column matching is also a harmless no-op.
--
-- Run manually against the target database, e.g.:
--   psql -h localhost -U ahaspace -d enlightenment -f scripts/week-18/002-course-public-and-source-fields.sql

-- 1. Add is_public nullable first, so the ALTER never fails on existing rows.
ALTER TABLE courses ADD COLUMN IF NOT EXISTS is_public boolean;

-- 2. Backfill pre-existing rows: default to private. Anything that should be
--    public (currently just the pilot course) is flipped by step 4 below.
UPDATE courses SET is_public = false WHERE is_public IS NULL;

-- 3. Now that every row has a value, enforce NOT NULL and set the column
--    default so future inserts that don't specify it default to private too.
ALTER TABLE courses ALTER COLUMN is_public SET NOT NULL;
ALTER TABLE courses ALTER COLUMN is_public SET DEFAULT false;

-- 4. Nullable source-attribution and insights columns. No NOT NULL step
--    needed - courses without an external source simply leave these null.
ALTER TABLE courses ADD COLUMN IF NOT EXISTS source_name varchar(255);
ALTER TABLE courses ADD COLUMN IF NOT EXISTS source_url varchar(512);
ALTER TABLE courses ADD COLUMN IF NOT EXISTS source_license varchar(255);
ALTER TABLE courses ADD COLUMN IF NOT EXISTS insights TEXT;

-- 5. Backfill the pilot course so an already-seeded database matches what a
--    fresh CurriculumSeedRunner run produces (see CurriculumSeedRunner's
--    COURSE_* constants - kept in sync with these literal values by hand).
UPDATE courses
SET is_public = true,
    source_name = 'AI Engineering from Scratch by rohitg00',
    source_url = 'https://github.com/rohitg00/ai-engineering-from-scratch',
    source_license = 'MIT License — Copyright (c) 2026 Rohit Ghumare',
    insights = E'Hands-on, code-first lessons over pure theory\nMath foundations built before jumping into ML\nStructured as phases, each phase a self-contained module'
WHERE title = 'AI Engineering From Scratch';
