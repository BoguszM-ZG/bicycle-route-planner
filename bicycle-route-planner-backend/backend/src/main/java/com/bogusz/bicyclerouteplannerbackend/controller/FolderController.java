package com.bogusz.bicyclerouteplannerbackend.controller;

import com.bogusz.bicyclerouteplannerbackend.dto.folder.CreateFolderRequestDto;
import com.bogusz.bicyclerouteplannerbackend.dto.folder.FolderDto;
import com.bogusz.bicyclerouteplannerbackend.dto.folder.RenameFolderRequest;
import com.bogusz.bicyclerouteplannerbackend.service.FolderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/folders")
public class FolderController {
    private final FolderService folderService;


    @GetMapping
    ResponseEntity<List<FolderDto>> getUserFolders(Authentication authentication) {
        List<FolderDto> folders = folderService.getUserFolders(authentication.getName());
        return ResponseEntity.ok(folders);
    }

    @PostMapping
    ResponseEntity<FolderDto> createFolder(
            @Valid @RequestBody CreateFolderRequestDto request, Authentication authentication){
        FolderDto savedFolder = folderService.createFolder(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(savedFolder);
    }

    @DeleteMapping("/{folderId}")
    ResponseEntity<Void> deleteFolder(@PathVariable Long folderId, Authentication authentication) {
        folderService.deleteFolder(folderId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{folderId}")
    ResponseEntity<FolderDto> renameFolder(
            @PathVariable Long folderId,
            @Valid @RequestBody RenameFolderRequest request,
            Authentication authentication) {
        FolderDto updatedFolder = folderService.renameFolder(folderId, request, authentication.getName());
        return ResponseEntity.ok(updatedFolder);
    }


}
