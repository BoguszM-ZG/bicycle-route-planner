package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.dto.folder.FolderDto;
import com.bogusz.bicyclerouteplannerbackend.dto.folder.FolderRequestDto;
import com.bogusz.bicyclerouteplannerbackend.exception.FolderNotFoundException;
import com.bogusz.bicyclerouteplannerbackend.model.Folder;
import com.bogusz.bicyclerouteplannerbackend.model.User;
import com.bogusz.bicyclerouteplannerbackend.repository.FolderRepository;
import com.bogusz.bicyclerouteplannerbackend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class FolderServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long FOLDER_ID = 10L;
    private static final String FOLDER_NAME = "Folder";
    private static final String NEW_FOLDER_NAME = "Changed Name";

    @Mock
    private FolderRepository folderRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FolderService folderService;

    @Captor
    private ArgumentCaptor<Folder> folderCaptor;

    @Test
    void shouldGetUserFoldersSuccessfully() {
        // GIVEN
        Folder mockFolder = new Folder();
        mockFolder.setId(FOLDER_ID);
        mockFolder.setName(FOLDER_NAME);

        given(folderRepository.findAllByUserId(USER_ID)).willReturn(List.of(mockFolder));

        // WHEN
        List<FolderDto> result = folderService.getUserFolders(USER_ID);

        // THEN
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(FOLDER_ID);
        assertThat(result.getFirst().name()).isEqualTo(FOLDER_NAME);
    }

    @Test
    void shouldCreateFolderSuccessfully() {
        // GIVEN
        FolderRequestDto request = new FolderRequestDto(FOLDER_NAME);
        User mockUser = new User();

        given(userRepository.getReferenceById(USER_ID)).willReturn(mockUser);
        given(folderRepository.save(any(Folder.class))).willAnswer(invocation -> {
            Folder folder = invocation.getArgument(0);
            folder.setId(FOLDER_ID);
            return folder;
        });

        // WHEN
        FolderDto result = folderService.createFolder(request, USER_ID);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(FOLDER_ID);
        assertThat(result.name()).isEqualTo(FOLDER_NAME);

        then(folderRepository).should().save(folderCaptor.capture());
        Folder capturedFolder = folderCaptor.getValue();

        assertThat(capturedFolder.getName()).isEqualTo(FOLDER_NAME);
        assertThat(capturedFolder.getUser()).isEqualTo(mockUser);
    }

    @Test
    void shouldRenameFolderSuccessfully() {
        // GIVEN
        Folder mockFolder = new Folder();
        mockFolder.setName(FOLDER_NAME);
        FolderRequestDto request = new FolderRequestDto(NEW_FOLDER_NAME);

        given(folderRepository.findByIdAndUserId(FOLDER_ID, USER_ID)).willReturn(Optional.of(mockFolder));

        // WHEN
        folderService.renameFolder(FOLDER_ID, request, USER_ID);

        // THEN
        assertThat(mockFolder.getName()).isEqualTo(NEW_FOLDER_NAME);
    }

    @Test
    void shouldThrowExceptionWhenRenamingNonExistentFolder() {
        // GIVEN
        FolderRequestDto request = new FolderRequestDto(NEW_FOLDER_NAME);
        given(folderRepository.findByIdAndUserId(FOLDER_ID, USER_ID)).willReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> folderService.renameFolder(FOLDER_ID, request, USER_ID))
                .isInstanceOf(FolderNotFoundException.class)
                .hasMessageContaining("Folder not found");

        then(folderRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldDeleteFolderSuccessfully() {
        // GIVEN
        Folder mockFolder = new Folder();
        given(folderRepository.findByIdAndUserId(FOLDER_ID, USER_ID)).willReturn(Optional.of(mockFolder));

        // WHEN
        folderService.deleteFolder(FOLDER_ID, USER_ID);

        // THEN
        then(folderRepository).should().delete(mockFolder);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentFolder() {
        // GIVEN
        given(folderRepository.findByIdAndUserId(FOLDER_ID, USER_ID)).willReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> folderService.deleteFolder(FOLDER_ID, USER_ID))
                .isInstanceOf(FolderNotFoundException.class)
                .hasMessageContaining("Folder not found");

        then(folderRepository).shouldHaveNoMoreInteractions();
    }
}
