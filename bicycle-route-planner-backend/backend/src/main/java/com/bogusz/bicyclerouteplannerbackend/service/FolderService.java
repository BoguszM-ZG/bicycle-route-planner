package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.dto.folder.CreateFolderRequestDto;
import com.bogusz.bicyclerouteplannerbackend.dto.folder.FolderDto;
import com.bogusz.bicyclerouteplannerbackend.dto.folder.RenameFolderRequest;
import com.bogusz.bicyclerouteplannerbackend.model.Folder;
import com.bogusz.bicyclerouteplannerbackend.model.User;
import com.bogusz.bicyclerouteplannerbackend.repository.FolderRepository;
import com.bogusz.bicyclerouteplannerbackend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.util.List;

@Service
@RequiredArgsConstructor
public class FolderService {
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;


    public List<FolderDto> getUserFolders(String userEmail) {
        List<Folder> allFolders = folderRepository.findAllByUser_Email(userEmail);
        return allFolders.stream()
                .map(this::folderToFolderDto)
                .toList();
    }

    public FolderDto createFolder(CreateFolderRequestDto request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Folder folder = new Folder();
        folder.setName(request.name());
        folder.setUser(user);

        if (request.parentFolderId() != null) {
            Folder parent = folderRepository.findByIdAndUserEmail(request.parentFolderId(), userEmail)
                    .orElseThrow(() -> new com.bogusz.bicyclerouteplannerbackend.exception.FolderNotFoundException("Parent folder not found or access denied"));
            folder.setParentFolder(parent);
        }

        return folderToFolderDto(folderRepository.save(folder));
    }

    @Transactional
    public void deleteFolder(Long folderId, String userEmail) {
        Folder folder = getValidFolderForUser(folderId, userEmail);
        folderRepository.delete(folder);
    }

    @Transactional
    public FolderDto renameFolder(Long folderId, RenameFolderRequest request, String userEmail){
        Folder folder = getValidFolderForUser(folderId, userEmail);

        folder.setName(request.name());

        return folderToFolderDto(folder);
    }


    private Folder getValidFolderForUser(Long folderId, String userEmail) {
        return folderRepository.findByIdAndUserEmail(folderId, userEmail)
                .orElseThrow(() -> new com.bogusz.bicyclerouteplannerbackend.exception.FolderNotFoundException("Folder not found or access denied"));
    }

    private FolderDto folderToFolderDto(Folder folder) {
        return new FolderDto(
                folder.getId(),
                folder.getName(),
                folder.getParentFolder() != null ? folder.getParentFolder().getId() : null
        );
    }

}
