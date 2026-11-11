package in.abhiikhula.admin.service;

import in.abhiikhula.admin.dto.AdminShopModerationDto;
import in.abhiikhula.admin.dto.AdminUserModerationDto;
import in.abhiikhula.admin.dto.ForceShopStatusRequest;
import in.abhiikhula.admin.dto.PlatformMetricsDto;
import in.abhiikhula.user.model.User;

import java.util.UUID;

public interface AdminGovernanceService {

    PlatformMetricsDto getPlatformMetrics(User admin);

    AdminShopModerationDto toggleShopActive(UUID shopId, User admin);

    AdminShopModerationDto forceShopStatus(UUID shopId, ForceShopStatusRequest request, User admin);

    AdminUserModerationDto blockUser(UUID userId, User admin);

    AdminUserModerationDto unblockUser(UUID userId, User admin);

    boolean toggleZoneActive(UUID zoneId, User admin);

    boolean toggleCategoryActive(UUID categoryId, User admin);
}

