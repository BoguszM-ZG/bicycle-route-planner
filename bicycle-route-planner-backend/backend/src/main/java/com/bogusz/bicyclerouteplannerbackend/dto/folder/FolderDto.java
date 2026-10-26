package com.bogusz.bicyclerouteplannerbackend.dto.folder;


import com.bogusz.bicyclerouteplannerbackend.model.Folder;

public record FolderDto(
        Long id,
        String name
) {
    public static FolderDto from(Folder folder) {
        if (folder == null) {
            return null;
        }
        return new FolderDto(folder.getId(), folder.getName());
    }
}