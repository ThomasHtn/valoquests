-- Nothing ever wrote these columns: the match import only knows a season's identifier and name.
ALTER TABLE season DROP COLUMN starts_at;
ALTER TABLE season DROP COLUMN ends_at;
