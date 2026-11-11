package in.abhiikhula.dispute.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.abhiikhula.dispute.dto.CreateDisputeRequest;
import in.abhiikhula.dispute.dto.DisputeResponseDto;
import in.abhiikhula.dispute.dto.MerchantReplyDisputeRequest;
import in.abhiikhula.dispute.dto.ResolveDisputeRequest;
import in.abhiikhula.dispute.dto.SupportFaqDto;
import in.abhiikhula.dispute.model.DisputeReason;
import in.abhiikhula.dispute.model.DisputeState;
import in.abhiikhula.dispute.model.ResolutionAction;
import in.abhiikhula.dispute.service.DisputeService;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({DisputeController.class, MerchantDisputeController.class, AdminDisputeController.class})
@AutoConfigureMockMvc(addFilters = false)
class DisputeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DisputeService disputeService;

    private User merchantUser;
    private User adminUser;
    private UUID shopId;
    private UUID disputeId;
    private UUID tokenId;

    @BeforeEach
    void setUp() {
        shopId = UUID.randomUUID();
        disputeId = UUID.randomUUID();
        tokenId = UUID.randomUUID();

        merchantUser = User.builder()
                .id(UUID.randomUUID())
                .name("Ramesh Gupta")
                .phone("9876543210")
                .role(UserRole.SHOP_OWNER)
                .build();

        adminUser = User.builder()
                .id(UUID.randomUUID())
                .name("Platform Admin")
                .phone("9999999999")
                .role(UserRole.ADMIN)
                .build();
    }

    @Test
    @DisplayName("POST /api/disputes should create dispute with auto evidence")
    void createDispute_ShouldSucceed() throws Exception {
        CreateDisputeRequest request = CreateDisputeRequest.builder()
                .shopId(shopId)
                .tokenId(tokenId)
                .customerPhone("9876500000")
                .reason(DisputeReason.WAS_CLOSED)
                .description("Dukan band thi par app me OPEN dikha rha tha")
                .build();

        DisputeResponseDto responseDto = DisputeResponseDto.builder()
                .id(disputeId)
                .shopId(shopId)
                .tokenId(tokenId)
                .customerPhone("9876500000")
                .reason(DisputeReason.WAS_CLOSED)
                .description("Dukan band thi par app me OPEN dikha rha tha")
                .evidenceJson("{\"lastStatusEvents\":[]}")
                .state(DisputeState.NEW)
                .createdAt(Instant.now())
                .build();

        when(disputeService.createDispute(any(CreateDisputeRequest.class), any())).thenReturn(responseDto);

        mockMvc.perform(post("/api/disputes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(disputeId.toString()))
                .andExpect(jsonPath("$.data.reason").value("WAS_CLOSED"))
                .andExpect(jsonPath("$.data.state").value("NEW"));

        verify(disputeService).createDispute(any(CreateDisputeRequest.class), any());
    }

    @Test
    @DisplayName("GET /api/support/faq should return static help categories")
    void getSupportFaqs_ShouldReturnList() throws Exception {
        List<SupportFaqDto> faqs = List.of(
                SupportFaqDto.builder()
                        .category("Tokens")
                        .question("How to cancel token?")
                        .answer("Open your active token and tap Cancel.")
                        .build()
        );

        when(disputeService.getFaqs()).thenReturn(faqs);

        mockMvc.perform(get("/api/support/faq"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].category").value("Tokens"))
                .andExpect(jsonPath("$.data[0].question").value("How to cancel token?"));
    }

    @Test
    @DisplayName("GET /api/shops/{shopId}/disputes should return merchant disputes")
    void getShopDisputes_ShouldReturnList() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(merchantUser, null, List.of())
        );

        DisputeResponseDto item = DisputeResponseDto.builder()
                .id(disputeId)
                .shopId(shopId)
                .reason(DisputeReason.WRONG_WAIT)
                .state(DisputeState.NEW)
                .build();

        when(disputeService.getShopDisputes(eq(shopId), eq(merchantUser))).thenReturn(List.of(item));

        mockMvc.perform(get("/api/shops/{shopId}/disputes", shopId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(disputeId.toString()))
                .andExpect(jsonPath("$.data[0].reason").value("WRONG_WAIT"));
    }

    @Test
    @DisplayName("POST /api/shops/{shopId}/disputes/{disputeId}/reply should update state to IN_REVIEW")
    void replyToDispute_ShouldSucceed() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(merchantUser, null, List.of())
        );

        MerchantReplyDisputeRequest request = MerchantReplyDisputeRequest.builder()
                .reply("Power outage ho gayi thi emergency me isliye shutter girana pada")
                .build();

        DisputeResponseDto responseDto = DisputeResponseDto.builder()
                .id(disputeId)
                .shopId(shopId)
                .state(DisputeState.IN_REVIEW)
                .merchantReply(request.getReply())
                .merchantReplyAt(Instant.now())
                .build();

        when(disputeService.replyToDispute(eq(shopId), eq(disputeId), any(MerchantReplyDisputeRequest.class), eq(merchantUser)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/shops/{shopId}/disputes/{disputeId}/reply", shopId, disputeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("IN_REVIEW"))
                .andExpect(jsonPath("$.data.merchantReply").value("Power outage ho gayi thi emergency me isliye shutter girana pada"));
    }

    @Test
    @DisplayName("GET /api/admin/disputes should return filtered disputes for admin")
    void getAdminDisputes_ShouldReturnList() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminUser, null, List.of())
        );

        DisputeResponseDto item = DisputeResponseDto.builder()
                .id(disputeId)
                .shopId(shopId)
                .state(DisputeState.NEW)
                .build();

        when(disputeService.listDisputesForAdmin(eq(DisputeState.NEW), eq(adminUser))).thenReturn(List.of(item));

        mockMvc.perform(get("/api/admin/disputes")
                        .param("state", "NEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(disputeId.toString()));
    }

    @Test
    @DisplayName("PATCH /api/admin/disputes/{disputeId}/resolve should resolve dispute and apply strike")
    void resolveDispute_ShouldSucceed() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminUser, null, List.of())
        );

        ResolveDisputeRequest request = ResolveDisputeRequest.builder()
                .action(ResolutionAction.STRIKE)
                .notes("Repeated false open reports verified by logs")
                .build();

        DisputeResponseDto responseDto = DisputeResponseDto.builder()
                .id(disputeId)
                .shopId(shopId)
                .state(DisputeState.RESOLVED)
                .resolutionAction(ResolutionAction.STRIKE)
                .resolutionNotes("Repeated false open reports verified by logs")
                .resolvedAt(Instant.now())
                .build();

        when(disputeService.resolveDispute(eq(disputeId), any(ResolveDisputeRequest.class), eq(adminUser)))
                .thenReturn(responseDto);

        mockMvc.perform(patch("/api/admin/disputes/{disputeId}/resolve", disputeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("RESOLVED"))
                .andExpect(jsonPath("$.data.resolutionAction").value("STRIKE"));
    }
}
