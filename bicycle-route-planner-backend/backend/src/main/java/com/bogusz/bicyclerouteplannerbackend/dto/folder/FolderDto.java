package com.bogusz.bicyclerouteplannerbackend.dto.folder;

import java.util.List;


public record FolderDto(
        Long id,
        String name,
        Long parentFolderId,
        List<FolderDto> subFolders
) {
}
