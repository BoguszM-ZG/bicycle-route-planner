package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.dto.folder.CreateFolderRequestDto;
import com.bogusz.bicyclerouteplannerbackend.dto.folder.FolderDto;
import com.bogusz.bicyclerouteplannerbackend.dto.folder.RenameFolderRequest;
import com.bogusz.bicyclerouteplannerbackend.exception.FolderNotFoundException;
import com.bogusz.bicyclerouteplannerbackend.mapper.FolderMapper;
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
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class FolderServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long FOLDER_ID = 10L;
    private static final Long PARENT_FOLDER_ID = 5L;
    private static final String FOLDER_NAME = "Folder";
    private static final String NEW_FOLDER_NAME = "Changed Name";

    @Mock
    private FolderRepository folderRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FolderMapper folderMapper;

    @InjectMocks
    private FolderService folderService;

    @Captor
    private ArgumentCaptor<Folder> folderCaptor;




    @Test
    void shouldGetUserFoldersSuccessfully() {
        // GIVEN
        Folder mockFolder = new Folder();
        FolderDto mockFolderDto = mock(FolderDto.class);

        given(folderRepository.findAllByUserId(USER_ID)).willReturn(List.of(mockFolder));
        given(folderMapper.toDto(mockFolder)).willReturn(mockFolderDto);

        // WHEN
        List<FolderDto> result = folderService.getUserFolders(USER_ID);

        // THEN
        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isEqualTo(mockFolderDto);
    }


    @Test
    void shouldCreateRootFolderSuccessfully() {
        // GIVEN
        CreateFolderRequestDto request = new CreateFolderRequestDto(FOLDER_NAME, null);
        User mockUser = new User();
        FolderDto mockFolderDto = mock(FolderDto.class);

        given(userRepository.getReferenceById(USER_ID)).willReturn(mockUser);
        given(folderRepository.save(any(Folder.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(folderMapper.toDto(any(Folder.class))).willReturn(mockFolderDto);

        // WHEN
        FolderDto result = folderService.createFolder(request, USER_ID);

        // THEN
        assertThat(result).isNotNull();

        // VERIFY
        then(folderRepository).should().save(folderCaptor.capture());
        Folder capturedFolder = folderCaptor.getValue();

        assertThat(capturedFolder.getName()).isEqualTo(FOLDER_NAME);
        assertThat(capturedFolder.getUser()).isEqualTo(mockUser);
        assertThat(capturedFolder.getParentFolder()).isNull(); // Nie podaliśmy rodzica, więc ma być null
    }

    @Test
    void shouldCreateSubFolderSuccessfully() {
        // GIVEN
        CreateFolderRequestDto request = new CreateFolderRequestDto(FOLDER_NAME, PARENT_FOLDER_ID);
        User mockUser = new User();
        Folder mockParentFolder = new Folder();

        given(userRepository.getReferenceById(USER_ID)).willReturn(mockUser);
        given(folderRepository.findByIdAndUserId(PARENT_FOLDER_ID, USER_ID)).willReturn(Optional.of(mockParentFolder));
        given(folderRepository.save(any(Folder.class))).willAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        folderService.createFolder(request, USER_ID);

        // THEN / VERIFY
        then(folderRepository).should().save(folderCaptor.capture());
        Folder capturedFolder = folderCaptor.getValue();

        assertThat(capturedFolder.getName()).isEqualTo(FOLDER_NAME);
        assertThat(capturedFolder.getParentFolder()).isEqualTo(mockParentFolder);
    }

    @Test
    void shouldThrowExceptionWhenCreatingSubFolderWithNonExistentParent() {
        // GIVEN
        CreateFolderRequestDto request = new CreateFolderRequestDto(FOLDER_NAME, PARENT_FOLDER_ID);
        User mockUser = new User();

        given(userRepository.getReferenceById(USER_ID)).willReturn(mockUser);
        given(folderRepository.findByIdAndUserId(PARENT_FOLDER_ID, USER_ID)).willReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> folderService.createFolder(request, USER_ID))
                .isInstanceOf(FolderNotFoundException.class)
                .hasMessageContaining("Parent folder not found");

        // VERIFY
        then(folderRepository).shouldHaveNoMoreInteractions();
    }


    @Test
    void shouldRenameFolderSuccessfully() {
        // GIVEN
        Folder mockFolder = new Folder();
        mockFolder.setName(FOLDER_NAME);
        RenameFolderRequest request = new RenameFolderRequest(NEW_FOLDER_NAME);

        given(folderRepository.findByIdAndUserId(FOLDER_ID, USER_ID)).willReturn(Optional.of(mockFolder));

        // WHEN
        folderService.renameFolder(FOLDER_ID, request, USER_ID);

        // THEN
        assertThat(mockFolder.getName()).isEqualTo(NEW_FOLDER_NAME);
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

        // VERIFY
        then(folderRepository).shouldHaveNoMoreInteractions();
    }
}