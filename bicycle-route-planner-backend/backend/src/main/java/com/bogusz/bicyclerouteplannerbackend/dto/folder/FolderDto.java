package com.bogusz.bicyclerouteplannerbackend.dto.folder;


public record FolderDto(
        Long id,
        String name,
        Long parentFolderId
) {
}