CREATE INDEX idx_folders_user_id ON folders(user_id);
CREATE INDEX idx_folders_parent_id ON folders(parent_folder_id);
CREATE INDEX idx_routes_user_id ON routes(user_id);
CREATE INDEX idx_routes_folder_id ON routes(folder_id);
CREATE INDEX idx_avoidance_zones_user_id ON avoidance_zones(user_id);
