package in.abhiikhula.shop.service;

import in.abhiikhula.category.model.Category;
import in.abhiikhula.category.repository.CategoryRepository;
import in.abhiikhula.common.exception.ResourceNotFoundException;
import in.abhiikhula.shop.dto.CreateShopRequest;
import in.abhiikhula.shop.dto.ShopDetailDto;
import in.abhiikhula.shop.dto.ShopSummaryDto;
import in.abhiikhula.shop.mapper.ShopMapper;
import in.abhiikhula.shop.model.Shop;
import in.abhiikhula.shop.model.ShopHours;
import in.abhiikhula.shop.repository.ShopHoursRepository;
import in.abhiikhula.shop.repository.ShopRepository;
import in.abhiikhula.zone.model.Zone;
import in.abhiikhula.zone.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopServiceImpl implements ShopService {

    private final ShopRepository shopRepository;
    private final ShopHoursRepository shopHoursRepository;
    private final ZoneRepository zoneRepository;
    private final CategoryRepository categoryRepository;
    private final ShopMapper shopMapper;
    private final in.abhiikhula.ads.service.FeaturedSlotService featuredSlotService;

    @Override
    @Transactional(readOnly = true)
    public List<ShopSummaryDto> discoverShops(UUID zoneId, UUID categoryId, Boolean openOnly, String q) {
        return discoverShops(zoneId, categoryId, openOnly, q, null, null, null, "STATUS");
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopSummaryDto> discoverShops(UUID zoneId, UUID categoryId, Boolean openOnly, String q,
                                              Double lat, Double lng, Double radiusKm, String sortBy) {
        log.debug("Discovering shops: zoneId={}, categoryId={}, openOnly={}, q={}, lat={}, lng={}, radiusKm={}, sortBy={}",
                zoneId, categoryId, openOnly, q, lat, lng, radiusKm, sortBy);

        List<Shop> shops = shopRepository.searchShops(zoneId, categoryId, openOnly, q);

        java.util.Set<UUID> featuredShopIds = (zoneId != null && featuredSlotService != null)
                ? featuredSlotService.getActiveFeaturedShopIds(zoneId, categoryId)
                : java.util.Collections.emptySet();

        List<ShopSummaryDto> dtos = shops.stream()
                .map(shop -> {
                    ShopSummaryDto dto = shopMapper.toSummaryDto(shop);
                    dto.setFeatured(featuredShopIds.contains(shop.getId()));
                    if (lat != null && lng != null && shop.getLat() != null && shop.getLng() != null) {
                        double dist = calculateHaversineKm(lat, lng, shop.getLat().doubleValue(), shop.getLng().doubleValue());
                        dto.setDistanceKm(Math.round(dist * 100.0) / 100.0);
                        if (dist < 1.0) {
                            dto.setDistanceDisplay(Math.round(dist * 1000.0) + " m");
                        } else {
                            dto.setDistanceDisplay(String.format(java.util.Locale.ENGLISH, "%.1f km", dist));
                        }
                    }
                    return dto;
                })
                .filter(dto -> {
                    if (radiusKm != null && radiusKm > 0 && lat != null && lng != null) {
                        return dto.getDistanceKm() != null && dto.getDistanceKm() <= radiusKm;
                    }
                    return true;
                })
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));

        if (featuredSlotService != null && !featuredShopIds.isEmpty()) {
            featuredSlotService.recordImpressions(featuredShopIds);
        }

        // BRD Rule: A boost lifts to the top position (BRD Section 21)
        java.util.Comparator<ShopSummaryDto> featuredFirst = java.util.Comparator.comparing(
                (ShopSummaryDto s) -> Boolean.TRUE.equals(s.getFeatured()),
                java.util.Comparator.reverseOrder()
        );

        // Sort results
        String sort = (sortBy == null) ? "STATUS" : sortBy.trim().toUpperCase();
        switch (sort) {
            case "DISTANCE" -> dtos.sort(featuredFirst.thenComparing(
                    ShopSummaryDto::getDistanceKm,
                    java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));
            case "CROWD" -> dtos.sort(featuredFirst.thenComparing(
                    ShopSummaryDto::getCrowdLevel,
                    java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                    .thenComparing(ShopSummaryDto::getDistanceKm, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));
            default -> dtos.sort(featuredFirst.thenComparingInt(
                    (ShopSummaryDto s) -> getStatusPriority(s.getStatus()))
                    .thenComparing(ShopSummaryDto::getCrowdLevel, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                    .thenComparing(ShopSummaryDto::getDistanceKm, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));
        }

        return dtos;
    }

    private int getStatusPriority(in.abhiikhula.shop.model.ShopStatus status) {
        if (status == null) return 5;
        return switch (status) {
            case OPEN -> 1;
            case BUSY -> 2;
            case PAUSED -> 3;
            case CLOSED -> 4;
        };
    }

    private double calculateHaversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double EARTH_RADIUS_KM = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(rLat1) * Math.cos(rLat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    @Override
    @Transactional(readOnly = true)
    public ShopDetailDto getShopById(UUID id) {
        log.debug("Fetching shop detail for id={}", id);
        Shop shop = shopRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", id));
        List<ShopHours> hours = shopHoursRepository.findByShopIdOrderByWeekdayAsc(shop.getId());
        return shopMapper.toDetailDto(shop, hours);
    }

    @Override
    @Transactional(readOnly = true)
    public ShopDetailDto getShopBySlug(String slug) {
        log.debug("Fetching shop detail for slug={}", slug);
        Shop shop = shopRepository.findBySlugWithDetails(slug.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "slug", slug));
        List<ShopHours> hours = shopHoursRepository.findByShopIdOrderByWeekdayAsc(shop.getId());
        return shopMapper.toDetailDto(shop, hours);
    }

    @Override
    @Transactional
    public ShopDetailDto createShop(CreateShopRequest request) {
        log.info("Creating new shop: {} in zone: {}", request.getName(), request.getZoneId());

        Zone zone = null;
        if (request.getZoneId() != null) {
            zone = zoneRepository.findById(request.getZoneId()).orElse(null);
        }
        if (zone == null) {
            zone = zoneRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Zone", "default", "No active zones configured in platform"));
        }

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId()).orElse(null);
        }
        if (category == null) {
            category = categoryRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "default", "No active categories configured in platform"));
        }

        Shop shop = shopMapper.toEntity(request, zone, category);

        // Ensure unique slug if collision occurs
        if (shopRepository.existsBySlug(shop.getSlug())) {
            shop.setSlug(shop.getSlug() + "-" + UUID.randomUUID().toString().substring(0, 6));
        }

        Shop savedShop = shopRepository.save(shop);
        log.info("Shop registered successfully with id: {}", savedShop.getId());
        return shopMapper.toDetailDto(savedShop, List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopSummaryDto> getShopsByPhone(String phone) {
        String clean = phone.replaceAll("\\D", "");
        if (clean.length() > 10) clean = clean.substring(clean.length() - 10);
        List<Shop> shops = shopRepository.findByPhoneContaining(clean);
        return shops.stream().map(shopMapper::toSummaryDto).toList();
    }
}
