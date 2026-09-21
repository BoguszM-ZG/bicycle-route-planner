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

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/folders")
public class FolderController {
    private final FolderService folderService;


    @GetMapping
    public ResponseEntity<List<FolderDto>> getUserFolders(@AuthenticationPrincipal UserPrincipal principal) {
        List<FolderDto> folders = folderService.getUserFolders(principal.id());
        return ResponseEntity.ok(folders);
    }

    @PostMapping
    public ResponseEntity<FolderDto> createFolder(
            @Valid @RequestBody CreateFolderRequestDto request, @AuthenticationPrincipal UserPrincipal principal){
        FolderDto savedFolder = folderService.createFolder(request, principal.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(savedFolder);
    }

    @DeleteMapping("/{folderId}")
    public ResponseEntity<Void> deleteFolder(@PathVariable Long folderId, @AuthenticationPrincipal UserPrincipal principal) {
        folderService.deleteFolder(folderId, principal.id());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{folderId}")
    public ResponseEntity<FolderDto> renameFolder(
            @PathVariable Long folderId,
            @Valid @RequestBody RenameFolderRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        FolderDto updatedFolder = folderService.renameFolder(folderId, request, principal.id());
        return ResponseEntity.ok(updatedFolder);
    }


}
