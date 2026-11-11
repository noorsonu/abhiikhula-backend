package in.abhiikhula.template.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.abhiikhula.template.dto.*;
import in.abhiikhula.template.model.TemplateChannel;
import in.abhiikhula.template.service.NotificationTemplateService;
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
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationTemplateController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationTemplateService templateService;

    private User adminUser;
    private UUID templateId;

    @BeforeEach
    void setUp() {
        templateId = UUID.randomUUID();
        adminUser = User.builder()
                .id(UUID.randomUUID())
                .name("Super Admin")
                .phone("9999999999")
                .role(UserRole.ADMIN)
                .build();
    }

    @Test
    @DisplayName("GET /api/admin/templates should return all templates")
    void listTemplates_ShouldSucceed() throws Exception {
        NotificationTemplateResponseDto dto = NotificationTemplateResponseDto.builder()
                .id(templateId)
                .slug("two_ahead")
                .channel(TemplateChannel.PUSH)
                .locale("hi")
                .titleTemplate("Aapka number aane wala hai!")
                .bodyTemplate("Aapke aage 2 log hain {{shop}} par.")
                .variables("shop,number")
                .version(1)
                .active(true)
                .build();

        when(templateService.listTemplates()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/admin/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(templateId.toString()))
                .andExpect(jsonPath("$.data[0].slug").value("two_ahead"));
    }

    @Test
    @DisplayName("POST /api/admin/templates should upsert template")
    void upsertTemplate_ShouldSucceed() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminUser, null, List.of())
        );

        UpsertTemplateRequest request = UpsertTemplateRequest.builder()
                .slug("two_ahead")
                .channel(TemplateChannel.PUSH)
                .locale("hi")
                .titleTemplate("Updated Title")
                .bodyTemplate("Updated Body {{shop}}")
                .variables("shop")
                .active(true)
                .build();

        NotificationTemplateResponseDto dto = NotificationTemplateResponseDto.builder()
                .id(templateId)
                .slug("two_ahead")
                .channel(TemplateChannel.PUSH)
                .locale("hi")
                .titleTemplate("Updated Title")
                .bodyTemplate("Updated Body {{shop}}")
                .version(2)
                .active(true)
                .build();

        when(templateService.upsertTemplate(any(UpsertTemplateRequest.class), eq(adminUser))).thenReturn(dto);

        mockMvc.perform(post("/api/admin/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("two_ahead"))
                .andExpect(jsonPath("$.data.version").value(2));

        verify(templateService).upsertTemplate(any(UpsertTemplateRequest.class), eq(adminUser));
    }

    @Test
    @DisplayName("POST /api/admin/templates/preview should substitute placeholders and dry-run")
    void previewTemplate_ShouldSucceed() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminUser, null, List.of())
        );

        RenderTemplateRequest request = RenderTemplateRequest.builder()
                .templateId(templateId)
                .variables(Map.of("shop", "Royal Barber", "number", "12"))
                .testRecipient("9876543210")
                .build();

        RenderedTemplateDto rendered = RenderedTemplateDto.builder()
                .title("Aapka number aane wala hai!")
                .body("Aapke aage sirf 2 log hain Royal Barber par. Token #12.")
                .channel("PUSH")
                .locale("hi")
                .testSent(true)
                .testRecipient("9876543210")
                .build();

        when(templateService.previewOrTestTemplate(any(RenderTemplateRequest.class), eq(adminUser)))
                .thenReturn(rendered);

        mockMvc.perform(post("/api/admin/templates/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.body").value("Aapke aage sirf 2 log hain Royal Barber par. Token #12."))
                .andExpect(jsonPath("$.data.testSent").value(true));
    }

    @Test
    @DisplayName("POST /api/admin/templates/broadcast should send platform announcement")
    void broadcastAnnouncement_ShouldSucceed() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminUser, null, List.of())
        );

        BroadcastRequest request = BroadcastRequest.builder()
                .title("Special Announcement")
                .body("Market remains open during holidays.")
                .targetRole("ALL")
                .build();

        BroadcastResponseDto responseDto = BroadcastResponseDto.builder()
                .id(UUID.randomUUID())
                .title("Special Announcement")
                .body("Market remains open during holidays.")
                .targetRole("ALL")
                .sentCount(150)
                .createdAt(Instant.now())
                .build();

        when(templateService.sendBroadcast(any(BroadcastRequest.class), eq(adminUser)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/admin/templates/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Special Announcement"))
                .andExpect(jsonPath("$.data.sentCount").value(150));
    }
}

