package com.bogusz.bicyclerouteplannerbackend.repository;

import com.bogusz.bicyclerouteplannerbackend.model.Folder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FolderRepository extends JpaRepository<Folder, Long> {
}