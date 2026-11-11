package in.abhiikhula.template.mapper;

import in.abhiikhula.template.dto.BroadcastResponseDto;
import in.abhiikhula.template.dto.NotificationTemplateResponseDto;
import in.abhiikhula.template.model.BroadcastAnnouncement;
import in.abhiikhula.template.model.NotificationTemplate;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationTemplateMapper {

    NotificationTemplateResponseDto toDto(NotificationTemplate template);

    List<NotificationTemplateResponseDto> toDtoList(List<NotificationTemplate> templates);

    BroadcastResponseDto toBroadcastDto(BroadcastAnnouncement announcement);
}

