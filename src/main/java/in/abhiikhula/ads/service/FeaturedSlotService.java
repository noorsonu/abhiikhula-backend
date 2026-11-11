package in.abhiikhula.ads.service;

import in.abhiikhula.ads.dto.BookFeaturedSlotRequest;
import in.abhiikhula.ads.dto.FeaturedSlotDto;
import in.abhiikhula.ads.dto.SlotAvailabilityDto;
import in.abhiikhula.user.model.User;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface FeaturedSlotService {

    FeaturedSlotDto bookFeaturedSlot(BookFeaturedSlotRequest request, User currentUser);

    SlotAvailabilityDto getSlotAvailability(UUID zoneId, UUID categoryId, LocalDate date);

    List<FeaturedSlotDto> getShopBookedSlots(UUID shopId, User currentUser);

    List<FeaturedSlotDto> listAllSlots(LocalDate start, LocalDate end, User admin);

    void recordClick(UUID slotId);

    void recordImpressions(Collection<UUID> shopIds);

    Set<UUID> getActiveFeaturedShopIds(UUID zoneId, UUID categoryId);
}

