package com.bogusz.bicyclerouteplannerbackend.controller;

import com.bogusz.bicyclerouteplannerbackend.dto.auth.UserPrincipal;
import com.bogusz.bicyclerouteplannerbackend.dto.folder.CreateFolderRequestDto;
import com.bogusz.bicyclerouteplannerbackend.dto.folder.FolderDto;
import com.bogusz.bicyclerouteplannerbackend.dto.folder.RenameFolderRequest;
import com.bogusz.bicyclerouteplannerbackend.service.FolderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/folders")
@Tag(name = "Folders", description = "Management of user route folders")
public class FolderController {
    private final FolderService folderService;


    @GetMapping
    @Operation(summary = "Get user folders", description = "Retrieves all folders belonging to the authenticated user.")
    public ResponseEntity<List<FolderDto>> getUserFolders(@AuthenticationPrincipal UserPrincipal principal) {
        List<FolderDto> folders = folderService.getUserFolders(principal.id());
        return ResponseEntity.ok(folders);
    }

    @PostMapping
    @Operation(summary = "Create folder", description = "Creates a new folder for the authenticated user.")
    public ResponseEntity<FolderDto> createFolder(
            @Valid @RequestBody CreateFolderRequestDto request, @AuthenticationPrincipal UserPrincipal principal){
        FolderDto savedFolder = folderService.createFolder(request, principal.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(savedFolder);
    }

    @DeleteMapping("/{folderId}")
    @Operation(summary = "Delete folder", description = "Deletes a specific folder and all its routes.")
    public ResponseEntity<Void> deleteFolder(@PathVariable Long folderId, @AuthenticationPrincipal UserPrincipal principal) {
        folderService.deleteFolder(folderId, principal.id());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{folderId}")
    @Operation(summary = "Rename folder", description = "Updates the name of an existing folder.")
    public ResponseEntity<FolderDto> renameFolder(
            @PathVariable Long folderId,
            @Valid @RequestBody RenameFolderRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        FolderDto updatedFolder = folderService.renameFolder(folderId, request, principal.id());
        return ResponseEntity.ok(updatedFolder);
    }


}
