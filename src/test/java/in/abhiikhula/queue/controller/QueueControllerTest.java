package in.abhiikhula.queue.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.abhiikhula.common.exception.BadRequestException;
import in.abhiikhula.common.exception.ForbiddenException;
import in.abhiikhula.queue.dto.CancelTokenRequest;
import in.abhiikhula.queue.dto.JoinQueueRequest;
import in.abhiikhula.queue.dto.QueueStatusResponseDto;
import in.abhiikhula.queue.dto.TokenResponseDto;
import in.abhiikhula.queue.model.SessionStatus;
import in.abhiikhula.queue.model.TokenStatus;
import in.abhiikhula.queue.service.QueueService;
import in.abhiikhula.user.model.User;
import in.abhiikhula.user.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QueueController.class)
@AutoConfigureMockMvc(addFilters = false)
class QueueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private QueueService queueService;

    @MockitoBean
    private in.abhiikhula.queue.sse.QueueSseBroadcaster queueSseBroadcaster;

    private User mockUser;
    private UUID shopId;
    private UUID tokenId;

    @BeforeEach
    void setUp() {
        shopId = UUID.randomUUID();
        tokenId = UUID.randomUUID();
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .phone("9876543210")
                .role(UserRole.CUSTOMER)
                .build();

        var auth = new UsernamePasswordAuthenticationToken(mockUser, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("POST /api/shops/{id}/queue/tokens/join issues token successfully")
    void shouldJoinQueueSuccessfully() throws Exception {
        JoinQueueRequest request = JoinQueueRequest.builder()
                .customerName("Amit Sharma")
                .customerPhone("9876543210")
                .build();

        TokenResponseDto responseDto = TokenResponseDto.builder()
                .id(tokenId)
                .shopId(shopId)
                .shopName("Royal Barber")
                .tokenNumber(1)
                .displayToken("#1")
                .status(TokenStatus.WAITING)
                .customerName("Amit Sharma")
                .customerPhone("9876543210")
                .estimatedWaitMin(15)
                .peopleAhead(0)
                .joinedAt(Instant.now())
                .build();

        when(queueService.joinQueue(eq(shopId), any(JoinQueueRequest.class), any(User.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/shops/{shopId}/queue/tokens/join", shopId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.displayToken").value("#1"))
                .andExpect(jsonPath("$.data.status").value("WAITING"))
                .andExpect(jsonPath("$.data.estimatedWaitMin").value(15));
    }

    @Test
    @DisplayName("POST /api/shops/{id}/queue/next calls next customer")
    void shouldCallNextCustomerSuccessfully() throws Exception {
        TokenResponseDto responseDto = TokenResponseDto.builder()
                .id(tokenId)
                .shopId(shopId)
                .tokenNumber(2)
                .displayToken("#2")
                .status(TokenStatus.CALLED)
                .customerName("Raju")
                .customerPhone("9123456789")
                .calledAt(Instant.now())
                .build();

        when(queueService.callNext(eq(shopId), any(User.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/shops/{shopId}/queue/next", shopId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CALLED"))
                .andExpect(jsonPath("$.data.displayToken").value("#2"));
    }

    @Test
    @DisplayName("POST /api/shops/{id}/queue/tokens/{id}/complete marks service complete")
    void shouldCompleteCustomerToken() throws Exception {
        TokenResponseDto responseDto = TokenResponseDto.builder()
                .id(tokenId)
                .shopId(shopId)
                .tokenNumber(2)
                .displayToken("#2")
                .status(TokenStatus.COMPLETED)
                .build();

        when(queueService.completeToken(eq(shopId), eq(tokenId), any(User.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/shops/{shopId}/queue/tokens/{tokenId}/complete", shopId, tokenId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/shops/{id}/queue/tokens/{id}/skip marks token SKIPPED")
    void shouldSkipTokenSuccessfully() throws Exception {
        TokenResponseDto responseDto = TokenResponseDto.builder()
                .id(tokenId)
                .shopId(shopId)
                .tokenNumber(2)
                .displayToken("#2")
                .status(TokenStatus.SKIPPED)
                .build();

        when(queueService.skipToken(eq(shopId), eq(tokenId), any(User.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/shops/{shopId}/queue/tokens/{tokenId}/skip", shopId, tokenId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SKIPPED"));
    }

    @Test
    @DisplayName("POST /api/shops/{id}/queue/delay updates delay broadcast")
    void shouldUpdateQueueDelay() throws Exception {
        in.abhiikhula.queue.dto.UpdateQueueDelayRequest request = in.abhiikhula.queue.dto.UpdateQueueDelayRequest.builder()
                .delayMinutes(20)
                .reason("Extra rush in the shop")
                .build();

        QueueStatusResponseDto responseDto = QueueStatusResponseDto.builder()
                .shopId(shopId)
                .shopName("Royal Barber")
                .sessionStatus(SessionStatus.ACTIVE)
                .waitingCount(3)
                .delayMinutes(20)
                .delayReason("Extra rush in the shop")
                .estimatedWaitMin(65)
                .acceptingTokens(true)
                .build();

        when(queueService.updateQueueDelay(eq(shopId), any(in.abhiikhula.queue.dto.UpdateQueueDelayRequest.class), any(User.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/shops/{shopId}/queue/delay", shopId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.delayMinutes").value(20))
                .andExpect(jsonPath("$.data.delayReason").value("Extra rush in the shop"));
    }

    @Test
    @DisplayName("POST /api/tokens/{id}/checkin marks token checked-in")
    void shouldCheckInSuccessfully() throws Exception {
        in.abhiikhula.queue.dto.CheckInRequest request = in.abhiikhula.queue.dto.CheckInRequest.builder()
                .lat(new java.math.BigDecimal("26.886050"))
                .lng(new java.math.BigDecimal("80.995050"))
                .build();

        TokenResponseDto responseDto = TokenResponseDto.builder()
                .id(tokenId)
                .status(TokenStatus.WAITING)
                .checkedIn(true)
                .checkedInAt(java.time.Instant.now())
                .build();

        when(queueService.checkIn(eq(tokenId), any(in.abhiikhula.queue.dto.CheckInRequest.class), any(User.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/tokens/{tokenId}/checkin", tokenId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.checkedIn").value(true));
    }

    @Test
    @DisplayName("POST /api/tokens/{id}/requeue re-enters waiting queue")
    void shouldReQueueSuccessfully() throws Exception {
        TokenResponseDto responseDto = TokenResponseDto.builder()
                .id(tokenId)
                .tokenNumber(5)
                .displayToken("#5")
                .status(TokenStatus.WAITING)
                .requeued(true)
                .build();

        when(queueService.reQueue(eq(tokenId), any(User.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/tokens/{tokenId}/requeue", tokenId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("WAITING"))
                .andExpect(jsonPath("$.data.requeued").value(true))
                .andExpect(jsonPath("$.data.displayToken").value("#5"));
    }

    @Test
    @DisplayName("POST /api/shops/{id}/queue/pause pauses the queue")
    void shouldPauseQueueSuccessfully() throws Exception {
        in.abhiikhula.queue.dto.PauseQueueRequest request = in.abhiikhula.queue.dto.PauseQueueRequest.builder()
                .pauseMinutes(30)
                .reason("Lunch break")
                .build();

        QueueStatusResponseDto responseDto = QueueStatusResponseDto.builder()
                .shopId(shopId)
                .sessionStatus(SessionStatus.PAUSED)
                .pauseReason("Lunch break")
                .acceptingTokens(false)
                .build();

        when(queueService.pauseQueue(eq(shopId), any(in.abhiikhula.queue.dto.PauseQueueRequest.class), any(User.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/shops/{shopId}/queue/pause", shopId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sessionStatus").value("PAUSED"))
                .andExpect(jsonPath("$.data.acceptingTokens").value(false));
    }

    @Test
    @DisplayName("POST /api/shops/{id}/queue/resume resumes the queue")
    void shouldResumeQueueSuccessfully() throws Exception {
        QueueStatusResponseDto responseDto = QueueStatusResponseDto.builder()
                .shopId(shopId)
                .sessionStatus(SessionStatus.ACTIVE)
                .acceptingTokens(true)
                .build();

        when(queueService.resumeQueue(eq(shopId), any(User.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/shops/{shopId}/queue/resume", shopId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sessionStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.acceptingTokens").value(true));
    }

    @Test
    @DisplayName("POST /api/shops/{id}/queue/reset resets the queue session")
    void shouldResetQueueSuccessfully() throws Exception {
        QueueStatusResponseDto responseDto = QueueStatusResponseDto.builder()
                .shopId(shopId)
                .sessionStatus(SessionStatus.ACTIVE)
                .totalTokensIssued(0)
                .currentServingTokenNumber(0)
                .waitingCount(0)
                .acceptingTokens(true)
                .build();

        when(queueService.resetQueue(eq(shopId), any(User.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/shops/{shopId}/queue/reset", shopId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalTokensIssued").value(0))
                .andExpect(jsonPath("$.data.waitingCount").value(0));
    }

    @Test
    @DisplayName("POST /api/queue/tokens/{id}/cancel cancels waiting token")
    void shouldCancelTokenSuccessfully() throws Exception {
        CancelTokenRequest request = CancelTokenRequest.builder()
                .reason("Change of plans")
                .build();

        TokenResponseDto responseDto = TokenResponseDto.builder()
                .id(tokenId)
                .status(TokenStatus.CANCELLED)
                .build();

        when(queueService.cancelToken(eq(tokenId), any(CancelTokenRequest.class), any(User.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/queue/tokens/{tokenId}/cancel", tokenId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("GET /api/shops/{id}/queue/status returns public queue status")
    void shouldGetLiveQueueStatus() throws Exception {
        QueueStatusResponseDto responseDto = QueueStatusResponseDto.builder()
                .shopId(shopId)
                .shopName("Royal Barber")
                .sessionStatus(SessionStatus.ACTIVE)
                .currentServingTokenNumber(1)
                .currentServingDisplayToken("#1")
                .totalTokensIssued(5)
                .waitingCount(4)
                .estimatedWaitMin(60)
                .acceptingTokens(true)
                .build();

        when(queueService.getQueueStatus(eq(shopId))).thenReturn(responseDto);

        mockMvc.perform(get("/api/shops/{shopId}/queue/status", shopId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.waitingCount").value(4))
                .andExpect(jsonPath("$.data.acceptingTokens").value(true));
    }

    @Test
    @DisplayName("GET /api/queue/tokens/my returns active customer tokens")
    void shouldGetMyActiveTokens() throws Exception {
        TokenResponseDto tokenDto = TokenResponseDto.builder()
                .id(tokenId)
                .shopId(shopId)
                .shopName("Royal Barber")
                .tokenNumber(3)
                .displayToken("#3")
                .status(TokenStatus.WAITING)
                .build();

        when(queueService.getMyActiveTokens(any(User.class))).thenReturn(List.of(tokenDto));

        mockMvc.perform(get("/api/queue/tokens/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].displayToken").value("#3"));
    }

    @Test
    @DisplayName("POST /api/shops/{id}/queue/tokens/join returns 400 when shop is closed")
    void shouldReturn400WhenShopIsClosed() throws Exception {
        when(queueService.joinQueue(eq(shopId), any(JoinQueueRequest.class), any(User.class)))
                .thenThrow(new BadRequestException("Shop is currently CLOSED. Tokens are not being issued."));

        mockMvc.perform(post("/api/shops/{shopId}/queue/tokens/join", shopId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Shop is currently CLOSED. Tokens are not being issued."));
    }

    @Test
    @DisplayName("POST /api/shops/{id}/queue/walk-in issues offline token successfully")
    void shouldIssueWalkInTokenSuccessfully() throws Exception {
        in.abhiikhula.queue.dto.IssueWalkInTokenRequest walkInReq = in.abhiikhula.queue.dto.IssueWalkInTokenRequest.builder()
                .customerName("Sharma Ji")
                .note("Offline walk-in haircut")
                .build();

        TokenResponseDto tokenDto = TokenResponseDto.builder()
                .id(UUID.randomUUID())
                .shopId(shopId)
                .shopName("Gupta Medical Store")
                .tokenNumber(15)
                .displayToken("#15")
                .status(TokenStatus.WAITING)
                .source("WALK_IN")
                .customerName("Sharma Ji")
                .peopleAhead(2)
                .estimatedWaitMin(20)
                .build();

        when(queueService.issueWalkInToken(eq(shopId), any(in.abhiikhula.queue.dto.IssueWalkInTokenRequest.class), any(User.class)))
                .thenReturn(tokenDto);

        mockMvc.perform(post("/api/shops/{shopId}/queue/walk-in", shopId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(walkInReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.displayToken").value("#15"))
                .andExpect(jsonPath("$.data.source").value("WALK_IN"))
                .andExpect(jsonPath("$.data.customerName").value("Sharma Ji"));
    }

    @Test
    @DisplayName("POST /api/shops/{id}/queue/walk-in returns 403 when non-owner tries to issue walk-in token")
    void shouldReturn403WhenNonOwnerTriesToIssueWalkInToken() throws Exception {
        when(queueService.issueWalkInToken(eq(shopId), any(in.abhiikhula.queue.dto.IssueWalkInTokenRequest.class), any(User.class)))
                .thenThrow(new ForbiddenException("You are not authorized to manage the queue for this shop"));

        mockMvc.perform(post("/api/shops/{shopId}/queue/walk-in", shopId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("GET /api/shops/{id}/queue/stream subscribes to SSE real-time stream")
    void shouldSubscribeToQueueStreamSuccessfully() throws Exception {
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = 
                new org.springframework.web.servlet.mvc.method.annotation.SseEmitter();

        when(queueSseBroadcaster.subscribe(eq(shopId), any())).thenReturn(emitter);

        mockMvc.perform(get("/api/shops/{shopId}/queue/stream", shopId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/tokens/{id} returns token status and live ETA (BRD FR-1603)")
    void shouldGetTokenByIdSuccessfully() throws Exception {
        TokenResponseDto responseDto = TokenResponseDto.builder()
                .id(tokenId)
                .tokenNumber(3)
                .displayToken("#3")
                .status(TokenStatus.WAITING)
                .peopleAhead(2)
                .estimatedWaitMin(15)
                .customerPhone("XXXXXX3210")
                .build();

        when(queueService.getTokenById(eq(tokenId), any())).thenReturn(responseDto);

        mockMvc.perform(get("/api/tokens/{tokenId}", tokenId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.displayToken").value("#3"))
                .andExpect(jsonPath("$.data.peopleAhead").value(2))
                .andExpect(jsonPath("$.data.customerPhone").value("XXXXXX3210"));
    }
}

