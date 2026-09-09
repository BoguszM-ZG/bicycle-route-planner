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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FolderService {
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;


    public List<FolderDto> getUserFolders(String userEmail) {
        List<Folder> allFolders = folderRepository.findAllByUser_Email(userEmail);
        Map<Long, List<Folder>> foldersByParentId = allFolders.stream()
                .collect(Collectors.groupingBy(f ->
                        f.getParentFolder() == null ? 0L : f.getParentFolder().getId()
                ));
        return buildTree(0L, foldersByParentId);
    }

    public FolderDto createFolder(CreateFolderRequestDto request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Folder folder = new Folder();
        folder.setName(request.name());
        folder.setUser(user);

        if (request.parentFolderId() != null) {
            Folder parent = folderRepository.findByIdAndUserEmail(request.parentFolderId(), userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("Parent folder not found"));
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
                .orElseThrow(() -> new IllegalArgumentException("Folder not found or Access denied"));
    }

    private FolderDto folderToFolderDto(Folder folder) {
        return new FolderDto(
                folder.getId(),
                folder.getName(),
                folder.getParentFolder() != null ? folder.getParentFolder().getId() : null,
                List.of()
        );
    }

    private List<FolderDto> buildTree(Long parentId, Map<Long, List<Folder>> foldersByParentId) {
        List<Folder> children = foldersByParentId.getOrDefault(parentId, Collections.emptyList());
        return children.stream()
                .map(folder -> new FolderDto(
                        folder.getId(),
                        folder.getName(),
                        folder.getParentFolder() != null ? folder.getParentFolder().getId() : null,
                        buildTree(folder.getId(), foldersByParentId)
                ))
                .toList();
    }




}
