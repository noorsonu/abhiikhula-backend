package in.abhiikhula.dashboard.service;

import in.abhiikhula.dashboard.dto.DailyShopSummaryDto;
import in.abhiikhula.dashboard.dto.MerchantShopDashboardDto;
import in.abhiikhula.user.model.User;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ShopDashboardService {

    List<MerchantShopDashboardDto> getMyShops(User merchant);

    DailyShopSummaryDto getDailySummary(UUID shopId, LocalDate date, User merchant);
}

