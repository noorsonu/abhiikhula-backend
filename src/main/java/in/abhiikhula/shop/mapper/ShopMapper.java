package in.abhiikhula.shop.mapper;

import in.abhiikhula.category.model.Category;
import in.abhiikhula.shop.dto.CreateShopRequest;
import in.abhiikhula.shop.dto.ShopDetailDto;
import in.abhiikhula.shop.dto.ShopHoursDto;
import in.abhiikhula.shop.dto.ShopSummaryDto;
import in.abhiikhula.shop.model.Shop;
import in.abhiikhula.shop.model.ShopHours;
import in.abhiikhula.shop.model.ShopStatus;
import in.abhiikhula.zone.model.Zone;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.text.Normalizer;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Mapper(componentModel = "spring")
public interface ShopMapper {

    Pattern NONLATIN = Pattern.compile("[^\\w-]");
    Pattern WHITESPACE = Pattern.compile("[\\s]");

    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "categoryIcon", source = "category.icon")
    @Mapping(target = "crowdLabel", source = "crowdLevel", qualifiedByName = "toCrowdLabel")
    @Mapping(target = "distanceKm", ignore = true)
    @Mapping(target = "distanceDisplay", ignore = true)
    @Mapping(target = "featured", ignore = true)
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "reviewCount", ignore = true)
    @Mapping(target = "etaAccuracy", ignore = true)
    ShopSummaryDto toSummaryDto(Shop shop);

    @Mapping(target = "id", source = "shop.id")
    @Mapping(target = "zoneId", source = "shop.zone.id")
    @Mapping(target = "zoneName", source = "shop.zone.name")
    @Mapping(target = "categoryId", source = "shop.category.id")
    @Mapping(target = "categoryName", source = "shop.category.name")
    @Mapping(target = "categoryIcon", source = "shop.category.icon")
    @Mapping(target = "defaultServiceMin", source = "shop.category.defaultServiceMin")
    @Mapping(target = "name", source = "shop.name")
    @Mapping(target = "slug", source = "shop.slug")
    @Mapping(target = "address", source = "shop.address")
    @Mapping(target = "lat", source = "shop.lat")
    @Mapping(target = "lng", source = "shop.lng")
    @Mapping(target = "phone", source = "shop.phone")
    @Mapping(target = "status", source = "shop.status")
    @Mapping(target = "crowdLevel", source = "shop.crowdLevel")
    @Mapping(target = "crowdLabel", source = "shop.crowdLevel", qualifiedByName = "toCrowdLabel")
    @Mapping(target = "tokenCap", source = "shop.tokenCap")
    @Mapping(target = "verified", source = "shop.verified")
    @Mapping(target = "strikes", source = "shop.strikes")
    @Mapping(target = "operatingHours", source = "hours")
    @Mapping(target = "lastStatusUpdateAt", source = "shop.lastStatusUpdateAt")
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "reviewCount", ignore = true)
    @Mapping(target = "etaAccuracy", ignore = true)
    @Mapping(target = "createdAt", source = "shop.createdAt")
    ShopDetailDto toDetailDto(Shop shop, List<ShopHours> hours);

    ShopHoursDto toHoursDto(ShopHours shopHours);

    List<ShopHoursDto> toHoursDtoList(List<ShopHours> shopHoursList);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "request.name")
    @Mapping(target = "slug", source = "request.name", qualifiedByName = "slugify")
    @Mapping(target = "address", source = "request.address")
    @Mapping(target = "lat", source = "request.lat")
    @Mapping(target = "lng", source = "request.lng")
    @Mapping(target = "phone", source = "request.phone")
    @Mapping(target = "status", constant = "OPEN")
    @Mapping(target = "crowdLevel", constant = "1")
    @Mapping(target = "tokenCap", source = "request.tokenCap")
    @Mapping(target = "verified", constant = "false")
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "zone", source = "zone")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "lastStatusUpdateAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Shop toEntity(CreateShopRequest request, Zone zone, Category category);

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

    @Named("slugify")
    default String slugify(String input) {
        if (input == null) return "";
        String nowhitespace = WHITESPACE.matcher(input.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }
}
