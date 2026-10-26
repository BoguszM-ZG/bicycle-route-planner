package com.bogusz.bicyclerouteplannerbackend.dto.folder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FolderRequestDto(
        @NotBlank(message = "Folder name is required")
        @Size(min = 1, max = 100, message = "Folder name must be between 1 and 100 characters")
        String name
) {
}
