package com.bogusz.bicyclerouteplannerbackend.integration;

import com.bogusz.bicyclerouteplannerbackend.dto.auth.AuthResponse;
import com.bogusz.bicyclerouteplannerbackend.dto.auth.RegisterRequest;
import com.bogusz.bicyclerouteplannerbackend.dto.folder.FolderDto;
import com.bogusz.bicyclerouteplannerbackend.dto.folder.FolderRequestDto;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class FolderIntegrationTest extends BaseIntegrationTest {

    private static final String TEST_USER_EMAIL = "folder.integration@example.com";
    private static final String TEST_USER_PASSWORD = "StrongPassword123!";
    private static final String AUTH_REGISTER_URL = "/api/auth/register";
    private static final String FOLDERS_URL = "/api/folders";
    private static final String FOLDER_NAME = "Summer Trips";
    private static final String NEW_FOLDER_NAME = "Winter Trips";

    @Autowired
    private MockMvc mockMvc;

    private final JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();
    private String userAccessToken;

    @BeforeEach
    void setUp() throws Exception {
        userAccessToken = registerUserAndGetAccessToken(TEST_USER_EMAIL, TEST_USER_PASSWORD, "Folder Tester");
    }

    @Test
    void shouldCreateAndRetrieveFolder() throws Exception {
        // Given
        FolderRequestDto request = new FolderRequestDto(FOLDER_NAME);

        // When
        mockMvc.perform(post(FOLDERS_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value(FOLDER_NAME))
                .andExpect(jsonPath("$.parentFolderId").doesNotExist());

        // Then
        mockMvc.perform(get(FOLDERS_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value(FOLDER_NAME))
                .andExpect(jsonPath("$[0].parentFolderId").doesNotExist());
    }

    @Test
    void shouldRenameFolder() throws Exception {
        // Given
        Long folderId = createDefaultFolder(userAccessToken);
        FolderRequestDto renameRequest = new FolderRequestDto(NEW_FOLDER_NAME);

        // When
        mockMvc.perform(patch(FOLDERS_URL + "/" + folderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(renameRequest)))
                .andExpect(status().isNoContent());

        // Then
        mockMvc.perform(get(FOLDERS_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value(NEW_FOLDER_NAME));
    }

    @Test
    void shouldDeleteFolder() throws Exception {
        // Given
        Long folderId = createDefaultFolder(userAccessToken);

        // When
        mockMvc.perform(delete(FOLDERS_URL + "/" + folderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken))
                .andExpect(status().isNoContent());

        // Then
        mockMvc.perform(get(FOLDERS_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldReturn404WhenDeletingFolderOfAnotherUser() throws Exception {
        // Given
        Long folderId = createDefaultFolder(userAccessToken);
        String thiefAccessToken = registerUserAndGetAccessToken("thief.integration@example.com", "ThiefPassword123!", "Folder Thief");

        // When & Then
        mockMvc.perform(delete(FOLDERS_URL + "/" + folderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + thiefAccessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenRenamingFolderOfAnotherUser() throws Exception {
        // Given
        Long folderId = createDefaultFolder(userAccessToken);
        String thiefAccessToken = registerUserAndGetAccessToken("thief.integration@example.com", "ThiefPassword123!", "Folder Thief");
        FolderRequestDto renameRequest = new FolderRequestDto(NEW_FOLDER_NAME);

        // When & Then
        mockMvc.perform(patch(FOLDERS_URL + "/" + folderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + thiefAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(renameRequest)))
                .andExpect(status().isNotFound());
    }

    private String registerUserAndGetAccessToken(String email, String password, String fullName) throws Exception {
        RegisterRequest request = new RegisterRequest(email, password, fullName);
        String responseContent = mockMvc.perform(post(AUTH_REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        return jsonMapper.readValue(responseContent, AuthResponse.class).accessToken();
    }

    private Long createDefaultFolder(String token) throws Exception {
        FolderRequestDto request = new FolderRequestDto(FOLDER_NAME);
        String responseContent = mockMvc.perform(post(FOLDERS_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return jsonMapper.readValue(responseContent, FolderDto.class).id();
    }
}
