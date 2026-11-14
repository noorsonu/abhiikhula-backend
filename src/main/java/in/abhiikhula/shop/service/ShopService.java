package in.abhiikhula.shop.service;

import in.abhiikhula.shop.dto.CreateShopRequest;
import in.abhiikhula.shop.dto.ShopDetailDto;
import in.abhiikhula.shop.dto.ShopSummaryDto;

import java.util.List;
import java.util.UUID;

public interface ShopService {

    List<ShopSummaryDto> discoverShops(UUID zoneId, UUID categoryId, Boolean openOnly, String q);

    List<ShopSummaryDto> discoverShops(UUID zoneId, UUID categoryId, Boolean openOnly, String q,
                                       Double lat, Double lng, Double radiusKm, String sortBy);

    ShopDetailDto getShopById(UUID id);

    ShopDetailDto getShopBySlug(String slug);

    ShopDetailDto createShop(CreateShopRequest request);

    List<ShopSummaryDto> getShopsByPhone(String phone);
}
