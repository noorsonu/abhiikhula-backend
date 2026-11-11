package in.abhiikhula.template.service;

import in.abhiikhula.common.exception.BadRequestException;
import in.abhiikhula.common.exception.ForbiddenException;
import in.abhiikhula.common.exception.ResourceNotFoundException;
import in.abhiikhula.template.dto.*;
import in.abhiikhula.template.mapper.NotificationTemplateMapper;
import in.abhiikhula.template.model.BroadcastAnnouncement;
import in.abhiikhula.template.model.NotificationTemplate;
import in.abhiikhula.template.repository.BroadcastAnnouncementRepository;
import in.abhiikhula.template.repository.NotificationTemplateRepository;
import in.abhiikhula.user.model.User;
import in.abhiikhula.user.model.UserRole;
import in.abhiikhula.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationTemplateServiceImpl implements NotificationTemplateService {

    private final NotificationTemplateRepository templateRepository;
    private final BroadcastAnnouncementRepository broadcastRepository;
    private final UserRepository userRepository;
    private final NotificationTemplateMapper templateMapper;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationTemplateResponseDto> listTemplates() {
        return templateMapper.toDtoList(templateRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationTemplateResponseDto getTemplateById(UUID id) {
        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationTemplate", "id", id));
        return templateMapper.toDto(template);
    }

    @Override
    @Transactional
    public NotificationTemplateResponseDto upsertTemplate(UpsertTemplateRequest request, User admin) {
        verifyAdmin(admin);

        Optional<NotificationTemplate> existingOpt = templateRepository.findBySlugAndChannelAndLocale(
                request.getSlug().trim(),
                request.getChannel(),
                request.getLocale().trim()
        );

        NotificationTemplate template;
        if (existingOpt.isPresent()) {
            template = existingOpt.get();
            template.setTitleTemplate(request.getTitleTemplate().trim());
            template.setBodyTemplate(request.getBodyTemplate().trim());
            if (request.getVariables() != null) {
                template.setVariables(request.getVariables().trim());
            }
            if (request.getActive() != null) {
                template.setActive(request.getActive());
            }
            template.setVersion(template.getVersion() + 1);
            log.info("Updated notification template: slug={}, channel={}, version={}", template.getSlug(), template.getChannel(), template.getVersion());
        } else {
            template = NotificationTemplate.builder()
                    .slug(request.getSlug().trim())
                    .channel(request.getChannel())
                    .locale(request.getLocale().trim())
                    .titleTemplate(request.getTitleTemplate().trim())
                    .bodyTemplate(request.getBodyTemplate().trim())
                    .variables(request.getVariables() != null ? request.getVariables().trim() : "")
                    .version(1)
                    .active(request.getActive() != null ? request.getActive() : true)
                    .build();
            log.info("Created new notification template: slug={}, channel={}", template.getSlug(), template.getChannel());
        }

        NotificationTemplate saved = templateRepository.save(template);
        return templateMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RenderedTemplateDto previewOrTestTemplate(RenderTemplateRequest request, User admin) {
        verifyAdmin(admin);

        NotificationTemplate template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("NotificationTemplate", "id", request.getTemplateId()));

        Map<String, String> vars = request.getVariables() != null ? request.getVariables() : Map.of();
        String renderedTitle = renderString(template.getTitleTemplate(), vars);
        String renderedBody = renderString(template.getBodyTemplate(), vars);

        boolean testSent = false;
        if (request.getTestRecipient() != null && !request.getTestRecipient().isBlank()) {
            // Simulated test send (BRD FR-1404)
            log.info("[TEST-SEND] Channel={} to Recipient={}: Title='{}', Body='{}'",
                    template.getChannel(), request.getTestRecipient(), renderedTitle, renderedBody);
            testSent = true;
        }

        return RenderedTemplateDto.builder()
                .title(renderedTitle)
                .body(renderedBody)
                .channel(template.getChannel().name())
                .locale(template.getLocale())
                .testSent(testSent)
                .testRecipient(request.getTestRecipient())
                .build();
    }

    @Override
    @Transactional
    public BroadcastResponseDto sendBroadcast(BroadcastRequest request, User admin) {
        verifyAdmin(admin);

        // BRD FR-1405: Rate-limited to max 1 broadcast per 7 days
        Instant sevenDaysAgo = Instant.now().minus(Duration.ofDays(7));
        Optional<BroadcastAnnouncement> recentBroadcast = broadcastRepository.findFirstByCreatedAtAfterOrderByCreatedAtDesc(sevenDaysAgo);
        if (recentBroadcast.isPresent()) {
            throw new BadRequestException(
                    "Broadcast rate limit exceeded. Only 1 platform announcement allowed per 7 days (BRD FR-1405). Last sent: " + recentBroadcast.get().getCreatedAt()
            );
        }

        int targetUserCount = (int) userRepository.count();

        BroadcastAnnouncement announcement = BroadcastAnnouncement.builder()
                .title(request.getTitle().trim())
                .body(request.getBody().trim())
                .targetRole(request.getTargetRole() != null ? request.getTargetRole().trim() : "ALL")
                .sentBy(admin)
                .sentCount(targetUserCount)
                .build();

        BroadcastAnnouncement saved = broadcastRepository.save(announcement);
        log.info("Broadcast announcement sent: id={}, title='{}', targetCount={}", saved.getId(), saved.getTitle(), saved.getSentCount());

        return templateMapper.toBroadcastDto(saved);
    }

    @Override
    public String renderString(String template, Map<String, String> variables) {
        if (template == null || variables == null || variables.isEmpty()) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            result = result.replace(placeholder, entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }

    private void verifyAdmin(User user) {
        if (user == null || user.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Only platform administrators can manage notification templates");
        }
    }
}

