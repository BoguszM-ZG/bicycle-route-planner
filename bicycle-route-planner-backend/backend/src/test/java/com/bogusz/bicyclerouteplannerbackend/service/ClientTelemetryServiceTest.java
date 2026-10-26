package com.bogusz.bicyclerouteplannerbackend.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bogusz.bicyclerouteplannerbackend.dto.telemetry.ClientLogRequestDto;
import com.bogusz.bicyclerouteplannerbackend.model.ClientLogLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class ClientTelemetryServiceTest {
    private static final String MESSAGE = "Test telemetry message";
    private static final String TIMESTAMP = "2026-10-01T12:00:00Z";
    private static final String DETAILS = "App crashed on route calculation";

    private final ClientTelemetryService telemetryService = new ClientTelemetryService();
    private ListAppender<ILoggingEvent> listAppender;
    private Logger clientLogger;

    @BeforeEach
    void setUp() {
        clientLogger = (Logger) LoggerFactory.getLogger("client.telemetry");
        clientLogger.setLevel(Level.DEBUG);
        listAppender = new ListAppender<>();
        listAppender.start();
        clientLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        clientLogger.detachAppender(listAppender);
        listAppender.stop();
    }

    @ParameterizedTest
    @EnumSource(ClientLogLevel.class)
    void shouldProcessClientLogWithoutExceptionsForAnyLevel(ClientLogLevel level) {
        // GIVEN
        ClientLogRequestDto request = new ClientLogRequestDto(
                level,
                MESSAGE,
                TIMESTAMP,
                DETAILS
        );

        // WHEN
        telemetryService.processClientLog(request);

        // THEN
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent event = listAppender.list.getFirst();
        assertThat(event.getLevel()).hasToString(level.name());
        assertThat(event.getFormattedMessage())
                .contains("[CLIENT-SIDE]")
                .contains("Time: " + TIMESTAMP)
                .contains("Msg: " + MESSAGE)
                .contains("Details: " + DETAILS);
    }

    @Test
    void shouldSanitizeCrlfAndNotThrow() {
        // GIVEN
        ClientLogRequestDto request = new ClientLogRequestDto(
                ClientLogLevel.ERROR,
                "Malicious log\r\nFake log line: User logged in",
                "2026-10-01T12:00:00Z\nExtra line",
                "Details with\r\nnewlines"
        );

        // WHEN
        telemetryService.processClientLog(request);

        // THEN
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent event = listAppender.list.getFirst();
        assertThat(event.getFormattedMessage())
                .isEqualTo("[CLIENT-SIDE] Time: 2026-10-01T12:00:00Z Extra line | Msg: Malicious log  Fake log line: User logged in | Details: Details with  newlines")
                .doesNotContain("\r")
                .doesNotContain("\n")
                .doesNotContain("Malicious log\r\nFake log line: User logged in")
                .doesNotContain("2026-10-01T12:00:00Z\nExtra line")
                .doesNotContain("Details with\r\nnewlines");
    }

    @Test
    void shouldReplaceStandaloneCrAndLfWithSpaces() {
        // GIVEN
        ClientLogRequestDto requestCr = new ClientLogRequestDto(
                ClientLogLevel.INFO,
                "Message\rwith CR",
                TIMESTAMP,
                null
        );
        ClientLogRequestDto requestLf = new ClientLogRequestDto(
                ClientLogLevel.WARN,
                "Message\nwith LF",
                TIMESTAMP,
                null
        );

        // WHEN
        telemetryService.processClientLog(requestCr);
        telemetryService.processClientLog(requestLf);

        // THEN
        assertThat(listAppender.list).hasSize(2);
        assertThat(listAppender.list.get(0).getFormattedMessage())
                .contains("Msg: Message with CR")
                .doesNotContain("\r");
        assertThat(listAppender.list.get(1).getFormattedMessage())
                .contains("Msg: Message with LF")
                .doesNotContain("\n");
    }

    @Test
    void shouldHandleNullRequestOrNullLevelSafely() {
        // GIVEN
        ClientLogRequestDto nullLevelRequest = new ClientLogRequestDto(null, MESSAGE, null, null);

        // WHEN
        telemetryService.processClientLog(null);
        telemetryService.processClientLog(nullLevelRequest);

        // THEN
        assertThat(listAppender.list).isEmpty();
    }
}
