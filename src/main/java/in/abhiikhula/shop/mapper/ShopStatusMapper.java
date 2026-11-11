package in.abhiikhula.shop.mapper;

import in.abhiikhula.shop.dto.ShopStatusResponseDto;
import in.abhiikhula.shop.dto.StatusEventHistoryDto;
import in.abhiikhula.shop.model.Shop;
import in.abhiikhula.shop.model.ShopStatusEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ShopStatusMapper {

    @Mapping(target = "crowdLabel", source = "crowdLevel", qualifiedByName = "toCrowdLabel")
    StatusEventHistoryDto toEventDto(ShopStatusEvent event);

    List<StatusEventHistoryDto> toEventDtoList(List<ShopStatusEvent> events);

    @Mapping(target = "shopId", source = "shop.id")
    @Mapping(target = "shopName", source = "shop.name")
    @Mapping(target = "status", source = "shop.status")
    @Mapping(target = "crowdLevel", source = "shop.crowdLevel")
    @Mapping(target = "crowdLabel", source = "shop.crowdLevel", qualifiedByName = "toCrowdLabel")
    @Mapping(target = "note", source = "latestNote")
    @Mapping(target = "lastStatusUpdateAt", source = "shop.lastStatusUpdateAt")
    @Mapping(target = "recentEvents", source = "recentEvents")
    ShopStatusResponseDto toStatusResponseDto(Shop shop, String latestNote, List<StatusEventHistoryDto> recentEvents);

    @Named("toCrowdLabel")
    default String toCrowdLabel(Integer crowdLevel) {
        if (crowdLevel == null) return "Unknown";
        return switch (crowdLevel) {
            case 1 -> "Empty (< 5 min wait)";
            case 2 -> "Light (5–15 min wait)";
            case 3 -> "Crowded (15–35 min wait)";
            case 4 -> "Very Crowded (35+ min wait)";
            default -> "Unknown";
        };
    }
}

