package in.abhiikhula.template.repository;

import in.abhiikhula.template.model.NotificationTemplate;
import in.abhiikhula.template.model.TemplateChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    Optional<NotificationTemplate> findBySlugAndChannelAndLocaleAndActiveTrue(String slug, TemplateChannel channel, String locale);

    Optional<NotificationTemplate> findBySlugAndChannelAndLocale(String slug, TemplateChannel channel, String locale);

    List<NotificationTemplate> findByActiveTrue();

    List<NotificationTemplate> findBySlug(String slug);
}

