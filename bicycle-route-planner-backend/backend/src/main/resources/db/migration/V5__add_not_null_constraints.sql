ALTER TABLE routes
    ALTER COLUMN route_type SET NOT NULL,
    ALTER COLUMN distance_meters SET NOT NULL,
    ALTER COLUMN elevation_gain SET NOT NULL,
    ALTER COLUMN elevation_loss SET NOT NULL,
    ALTER COLUMN start_point SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE avoidance_zones
    ALTER COLUMN is_active SET NOT NULL;

ALTER TABLE users
    ALTER COLUMN full_name SET NOT NULL,
    ALTER COLUMN is_active SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL;
