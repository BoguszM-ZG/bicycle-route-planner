package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.dto.folder.FolderDto;
import com.bogusz.bicyclerouteplannerbackend.dto.folder.FolderRequestDto;
import com.bogusz.bicyclerouteplannerbackend.exception.FolderNotFoundException;
import com.bogusz.bicyclerouteplannerbackend.model.Folder;
import com.bogusz.bicyclerouteplannerbackend.model.User;
import com.bogusz.bicyclerouteplannerbackend.repository.FolderRepository;
import com.bogusz.bicyclerouteplannerbackend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FolderService {
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;


    @Transactional(readOnly = true)
    public List<FolderDto> getUserFolders(Long userId) {
        List<Folder> allFolders = folderRepository.findAllByUserId(userId);
        return allFolders.stream()
                .map(FolderDto::from)
                .toList();
    }

    @Transactional
    public FolderDto createFolder(FolderRequestDto request, Long userId) {
        User user = userRepository.getReferenceById(userId);

        Folder folder = new Folder();
        folder.setName(request.name());
        folder.setUser(user);

        Folder savedFolder = folderRepository.save(folder);
        log.info("Created new folder with ID: {} for user ID: {}", savedFolder.getId(), userId);

        return FolderDto.from(savedFolder);
    }

    @Transactional
    public void deleteFolder(Long folderId, Long userId) {
        Folder folder = getValidFolderForUser(folderId, userId);
        folderRepository.delete(folder);
        log.info("Deleted folder with ID: {} for user ID: {}", folderId, userId);
    }

    @Transactional
    public void renameFolder(Long folderId, FolderRequestDto request, Long userId){
        Folder folder = getValidFolderForUser(folderId, userId);
        folder.setName(request.name());
    }



    private Folder getValidFolderForUser(Long folderId, Long userId) {
        return folderRepository.findByIdAndUserId(folderId, userId)
                .orElseThrow(() -> new FolderNotFoundException("Folder not found or access denied"));
    }

}
