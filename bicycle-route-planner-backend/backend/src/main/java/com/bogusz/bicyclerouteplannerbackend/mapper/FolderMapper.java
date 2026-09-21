package com.bogusz.bicyclerouteplannerbackend.mapper;

import com.bogusz.bicyclerouteplannerbackend.dto.folder.FolderDto;
import com.bogusz.bicyclerouteplannerbackend.model.Folder;
import org.springframework.stereotype.Component;

@Component
public class FolderMapper {
    
    public FolderDto toDto(Folder folder) {
        return new FolderDto(
                folder.getId(),
                folder.getName(),
                folder.getParentFolder() != null ? folder.getParentFolder().getId() : null
        );
    }
}
