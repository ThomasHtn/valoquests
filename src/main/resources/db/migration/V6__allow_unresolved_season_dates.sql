-- Match history exposes the season identifier and label, but not its dates.
-- Dates can be enriched later from Riot content data without blocking match import.
ALTER TABLE season ALTER COLUMN starts_at DROP NOT NULL;
ALTER TABLE season ALTER COLUMN ends_at DROP NOT NULL;
