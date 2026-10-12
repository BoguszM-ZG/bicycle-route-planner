package com.bogusz.bicyclerouteplannerbackend.controller;

import com.bogusz.bicyclerouteplannerbackend.dto.auth.UserPrincipal;
import com.bogusz.bicyclerouteplannerbackend.dto.folder.CreateFolderRequestDto;
import com.bogusz.bicyclerouteplannerbackend.dto.folder.FolderDto;
import com.bogusz.bicyclerouteplannerbackend.dto.folder.RenameFolderRequest;
import com.bogusz.bicyclerouteplannerbackend.service.FolderService;
import com.bogusz.bicyclerouteplannerbackend.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FolderController.class)
@AutoConfigureMockMvc(addFilters = false)
class FolderControllerTest {

    private static final long FOLDER_ID = 10L;
    private static final long USER_ID = 99L;
    private static final String FOLDER_NAME = "Weekend Trips";
    private static final Long PARENT_FOLDER_ID = null;

    private static final String FOLDERS_URL = "/api/folders";
    private static final String FOLDERS_URL_WITH_ID = "/api/folders/" + FOLDER_ID;
    private static final String NEW_FOLDER_NAME = "Summer Trips 2026";
    private static final String BLANK_STRING = "";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private FolderService folderService;

    @MockitoBean
    private JwtService jwtService;

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(getMockAuthentication());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturn200AndFolderListWhenGettingUserFolders() throws Exception {
        // Given
        FolderDto folderDto = new FolderDto(FOLDER_ID, FOLDER_NAME, PARENT_FOLDER_ID);
        given(folderService.getUserFolders(USER_ID)).willReturn(List.of(folderDto));

        // When & Then
        mockMvc.perform(get(FOLDERS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].id").value(FOLDER_ID))
                .andExpect(jsonPath("$[0].name").value(FOLDER_NAME));
    }

    @Test
    void shouldReturn201WhenCreatingValidFolder() throws Exception {
        // Given
        CreateFolderRequestDto request = new CreateFolderRequestDto(FOLDER_NAME, PARENT_FOLDER_ID);
        FolderDto response = new FolderDto(FOLDER_ID, FOLDER_NAME, PARENT_FOLDER_ID);

        given(folderService.createFolder(any(), eq(USER_ID))).willReturn(response);

        // When
        mockMvc.perform(post(FOLDERS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(FOLDER_ID))
                .andExpect(jsonPath("$.name").value(FOLDER_NAME));

        // Then
        ArgumentCaptor<CreateFolderRequestDto> createRequestCaptor = ArgumentCaptor.forClass(CreateFolderRequestDto.class);
        then(folderService).should(times(1)).createFolder(createRequestCaptor.capture(), eq(USER_ID));
        
        CreateFolderRequestDto capturedRequest = createRequestCaptor.getValue();
        assertThat(capturedRequest.name()).isEqualTo(FOLDER_NAME);
        assertThat(capturedRequest.parentFolderId()).isEqualTo(PARENT_FOLDER_ID);
    }

    @Test
    void shouldReturn400BadRequestWhenCreatingFolderWithBlankName() throws Exception {
        // Given
        CreateFolderRequestDto badRequest = new CreateFolderRequestDto(BLANK_STRING, PARENT_FOLDER_ID);

        // When & Then
        mockMvc.perform(post(FOLDERS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());

        // Then
        then(folderService).should(never()).createFolder(any(), any());
    }

    @Test
    void shouldReturn204WhenRenamingFolder() throws Exception {
        // Given
        RenameFolderRequest request = new RenameFolderRequest(NEW_FOLDER_NAME);

        // When
        mockMvc.perform(patch(FOLDERS_URL_WITH_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        // Then
        ArgumentCaptor<RenameFolderRequest> renameRequestCaptor = ArgumentCaptor.forClass(RenameFolderRequest.class);
        then(folderService).should(times(1)).renameFolder(eq(FOLDER_ID), renameRequestCaptor.capture(), eq(USER_ID));
        
        RenameFolderRequest capturedRequest = renameRequestCaptor.getValue();
        assertThat(capturedRequest.name()).isEqualTo(NEW_FOLDER_NAME);
    }

    @Test
    void shouldReturn400BadRequestWhenRenamingFolderWithBlankName() throws Exception {
        // Given
        RenameFolderRequest badRequest = new RenameFolderRequest(BLANK_STRING);

        // When & Then
        mockMvc.perform(patch(FOLDERS_URL_WITH_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());

        // Then
        then(folderService).should(never()).renameFolder(any(), any(), any());
    }

    @Test
    void shouldReturn204WhenDeletingFolder() throws Exception {
        // When & Then
        mockMvc.perform(delete(FOLDERS_URL_WITH_ID))
                .andExpect(status().isNoContent());

        // Then
        then(folderService).should(times(1)).deleteFolder(FOLDER_ID, USER_ID);
    }

    private Authentication getMockAuthentication() {
        UserPrincipal principal = new UserPrincipal(USER_ID, "test@test.com");
        return new UsernamePasswordAuthenticationToken(
                principal, null, Collections.emptyList()
        );
    }
}
