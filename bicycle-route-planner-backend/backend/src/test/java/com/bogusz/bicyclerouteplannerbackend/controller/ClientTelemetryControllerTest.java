package com.bogusz.bicyclerouteplannerbackend.controller;

import com.bogusz.bicyclerouteplannerbackend.dto.telemetry.ClientLogRequestDto;
import com.bogusz.bicyclerouteplannerbackend.model.ClientLogLevel;
import com.bogusz.bicyclerouteplannerbackend.service.ClientTelemetryService;
import com.bogusz.bicyclerouteplannerbackend.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ClientTelemetryController.class)
@AutoConfigureMockMvc(addFilters = false)
class ClientTelemetryControllerTest {

    private static final String LOGS_URL = "/api/telemetry/logs";
    private static final String MESSAGE = "Frontend crashed";
    private static final String TIMESTAMP = "2026-10-06T23:00:00Z";
    private static final String DETAILS = "NullPointerException at line 42";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ClientTelemetryService telemetryService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void shouldReturn202AcceptedWhenLoggingClientEvent() throws Exception {
        // Given
        ClientLogRequestDto requestDto = new ClientLogRequestDto(ClientLogLevel.ERROR, MESSAGE, TIMESTAMP, DETAILS);

        // When
        mockMvc.perform(post(LOGS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isAccepted());

        // Then
        ArgumentCaptor<ClientLogRequestDto> captor = ArgumentCaptor.forClass(ClientLogRequestDto.class);
        then(telemetryService).should(times(1)).processClientLog(captor.capture());

        ClientLogRequestDto captured = captor.getValue();
        assertThat(captured.level()).isEqualTo(ClientLogLevel.ERROR);
        assertThat(captured.message()).isEqualTo(MESSAGE);
        assertThat(captured.timestamp()).isEqualTo(TIMESTAMP);
        assertThat(captured.details()).isEqualTo(DETAILS);
    }

    @Test
    void shouldReturn400BadRequestWhenLogLevelIsNull() throws Exception {
        // Given
        ClientLogRequestDto requestDto = new ClientLogRequestDto(null, MESSAGE, TIMESTAMP, DETAILS);

        // When & Then
        mockMvc.perform(post(LOGS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());

        then(telemetryService).should(never()).processClientLog(any());
    }

    @Test
    void shouldReturn400BadRequestWhenMessageIsBlank() throws Exception {
        // Given
        ClientLogRequestDto requestDto = new ClientLogRequestDto(ClientLogLevel.WARN, "   ", TIMESTAMP, DETAILS);

        // When & Then
        mockMvc.perform(post(LOGS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());

        then(telemetryService).should(never()).processClientLog(any());
    }
}
