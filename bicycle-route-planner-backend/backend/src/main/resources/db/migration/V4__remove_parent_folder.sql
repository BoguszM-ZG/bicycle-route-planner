DROP INDEX IF EXISTS idx_folders_parent_id;

ALTER TABLE folders
    DROP COLUMN IF EXISTS parent_folder_id;
