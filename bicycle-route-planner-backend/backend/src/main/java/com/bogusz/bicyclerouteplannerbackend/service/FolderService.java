package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.dto.folder.CreateFolderRequestDto;
import com.bogusz.bicyclerouteplannerbackend.dto.folder.FolderDto;
import com.bogusz.bicyclerouteplannerbackend.dto.folder.RenameFolderRequest;
import com.bogusz.bicyclerouteplannerbackend.exception.FolderNotFoundException;
import com.bogusz.bicyclerouteplannerbackend.model.Folder;
import com.bogusz.bicyclerouteplannerbackend.model.User;
import com.bogusz.bicyclerouteplannerbackend.repository.FolderRepository;
import com.bogusz.bicyclerouteplannerbackend.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.util.List;

@Service
@RequiredArgsConstructor
public class FolderService {
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;


    @Transactional(readOnly = true)
    public List<FolderDto> getUserFolders(Long userId) {
        List<Folder> allFolders = folderRepository.findAllByUserId(userId);
        return allFolders.stream()
                .map(this::folderToFolderDto)
                .toList();
    }

    @Transactional
    public FolderDto createFolder(CreateFolderRequestDto request, Long userId) {
        User user = userRepository.getReferenceById(userId);

        Folder folder = new Folder();
        folder.setName(request.name());
        folder.setUser(user);

        if (request.parentFolderId() != null) {
            Folder parent = folderRepository.findByIdAndUserId(request.parentFolderId(), userId)
                    .orElseThrow(() -> new FolderNotFoundException("Parent folder not found or access denied"));
            folder.setParentFolder(parent);
        }

        return folderToFolderDto(folderRepository.save(folder));
    }

    @Transactional
    public void deleteFolder(Long folderId, Long userId) {
        Folder folder = getValidFolderForUser(folderId, userId);
        folderRepository.delete(folder);
    }

    @Transactional
    public FolderDto renameFolder(Long folderId, RenameFolderRequest request, Long userId){
        Folder folder = getValidFolderForUser(folderId, userId);

        folder.setName(request.name());

        return folderToFolderDto(folder);
    }



    private Folder getValidFolderForUser(Long folderId, Long userId) {
        return folderRepository.findByIdAndUserId(folderId, userId)
                .orElseThrow(() -> new FolderNotFoundException("Folder not found or access denied"));
    }

    private FolderDto folderToFolderDto(Folder folder) {
        return new FolderDto(
                folder.getId(),
                folder.getName(),
                folder.getParentFolder() != null ? folder.getParentFolder().getId() : null
        );
    }

}
