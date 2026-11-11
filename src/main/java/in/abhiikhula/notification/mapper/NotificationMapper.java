package in.abhiikhula.notification.mapper;

import in.abhiikhula.notification.dto.NotificationResponseDto;
import in.abhiikhula.notification.model.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(target = "shopId", source = "shop.id")
    @Mapping(target = "shopName", source = "shop.name")
    @Mapping(target = "tokenId", source = "token.id")
    @Mapping(target = "read", source = "isRead")
    NotificationResponseDto toDto(Notification notification);

    List<NotificationResponseDto> toDtoList(List<Notification> notifications);
}

