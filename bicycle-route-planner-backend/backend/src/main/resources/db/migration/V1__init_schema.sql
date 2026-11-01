CREATE EXTENSION IF NOT EXISTS postgis;


CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       email VARCHAR(255) UNIQUE NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       role VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER',
                       is_active BOOLEAN DEFAULT TRUE,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);



CREATE TABLE folders (
                         id BIGSERIAL PRIMARY KEY,
                         user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                         parent_folder_id BIGINT REFERENCES folders(id) ON DELETE CASCADE,
                         name VARCHAR(100) NOT NULL
);


CREATE TABLE routes (
                        id BIGSERIAL PRIMARY KEY,
                        user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                        folder_id BIGINT REFERENCES folders(id) ON DELETE SET NULL,

                        name VARCHAR(150) NOT NULL,
                        route_type VARCHAR(50),
                        target_distance DOUBLE PRECISION,
                        distance_meters DOUBLE PRECISION,
                        elevation_gain DOUBLE PRECISION,
                        elevation_loss DOUBLE PRECISION,

                        start_point GEOMETRY(Point, 4326),
                        route_geom GEOMETRY(LineStringZ, 4326) NOT NULL,

                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE INDEX idx_routes_geom ON routes USING GIST (route_geom);
CREATE INDEX idx_routes_start_point ON routes USING GIST (start_point);


CREATE TABLE avoidance_zones (
                                 id BIGSERIAL PRIMARY KEY,
                                 user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                 name VARCHAR(100) NOT NULL,
                                 zone_geom GEOMETRY(Polygon, 4326) NOT NULL,
                                 is_active BOOLEAN DEFAULT TRUE
);


CREATE INDEX idx_avoidance_geom ON avoidance_zones USING GIST (zone_geom);