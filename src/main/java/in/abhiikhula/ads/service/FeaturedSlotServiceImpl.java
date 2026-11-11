package in.abhiikhula.ads.service;

import in.abhiikhula.ads.dto.BookFeaturedSlotRequest;
import in.abhiikhula.ads.dto.FeaturedSlotDto;
import in.abhiikhula.ads.dto.SlotAvailabilityDto;
import in.abhiikhula.ads.mapper.FeaturedSlotMapper;
import in.abhiikhula.ads.model.FeaturedSlot;
import in.abhiikhula.ads.repository.FeaturedSlotRepository;
import in.abhiikhula.common.exception.BadRequestException;
import in.abhiikhula.common.exception.ForbiddenException;
import in.abhiikhula.common.exception.ResourceNotFoundException;
import in.abhiikhula.common.exception.UnauthorizedException;
import in.abhiikhula.shop.model.Shop;
import in.abhiikhula.shop.model.ShopStatus;
import in.abhiikhula.shop.repository.ShopRepository;
import in.abhiikhula.user.model.User;
import in.abhiikhula.user.model.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeaturedSlotServiceImpl implements FeaturedSlotService {

    private final FeaturedSlotRepository featuredSlotRepository;
    private final ShopRepository shopRepository;
    private final FeaturedSlotMapper featuredSlotMapper;

    private static final int MAX_SLOTS_PER_CATEGORY_PER_DATE = 2; // BRD FR-1001

    @Override
    @Transactional
    public FeaturedSlotDto bookFeaturedSlot(BookFeaturedSlotRequest request, User currentUser) {
        Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", request.getShopId()));

        verifyShopAccess(shop, currentUser);

        if (!Boolean.TRUE.equals(shop.getActive())) {
            throw new BadRequestException("Cannot book featured slot for an inactive shop");
        }

        LocalDate slotDate = request.getSlotDate();
        UUID zoneId = shop.getZone().getId();
        UUID categoryId = shop.getCategory().getId();

        // BRD Rule: One slot per shop per zone per date
        if (featuredSlotRepository.existsByZoneIdAndSlotDateAndShopId(zoneId, slotDate, shop.getId())) {
            throw new BadRequestException("Shop '" + shop.getName() + "' already has a featured slot booked for " + slotDate);
        }

        // BRD FR-1001: Maximum 2 slots per zone and category per date
        List<FeaturedSlot> existingSlots = featuredSlotRepository
                .findByZoneIdAndCategoryIdAndSlotDateAndActiveTrueOrderByPositionAsc(zoneId, categoryId, slotDate);

        if (existingSlots.size() >= MAX_SLOTS_PER_CATEGORY_PER_DATE) {
            throw new BadRequestException("All " + MAX_SLOTS_PER_CATEGORY_PER_DATE + " featured slots for this category on " + slotDate + " are fully booked");
        }

        Set<Integer> takenPositions = existingSlots.stream()
                .map(FeaturedSlot::getPosition)
                .collect(Collectors.toSet());

        int allocatedPosition;
        if (request.getPreferredPosition() != null && !takenPositions.contains(request.getPreferredPosition())) {
            allocatedPosition = request.getPreferredPosition();
        } else {
            allocatedPosition = !takenPositions.contains(1) ? 1 : 2;
        }

        FeaturedSlot slot = FeaturedSlot.builder()
                .zone(shop.getZone())
                .category(shop.getCategory())
                .shop(shop)
                .slotDate(slotDate)
                .position(allocatedPosition)
                .active(true)
                .impressions(0)
                .clicks(0)
                .build();

        FeaturedSlot saved = featuredSlotRepository.save(slot);
        log.info("Featured slot booked: shop={}, date={}, position={}, zone={}", shop.getName(), slotDate, allocatedPosition, zoneId);

        return featuredSlotMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SlotAvailabilityDto getSlotAvailability(UUID zoneId, UUID categoryId, LocalDate date) {
        LocalDate checkDate = date != null ? date : LocalDate.now();
        List<FeaturedSlot> booked = featuredSlotRepository
                .findByZoneIdAndCategoryIdAndSlotDateAndActiveTrueOrderByPositionAsc(zoneId, categoryId, checkDate);

        Set<Integer> taken = booked.stream().map(FeaturedSlot::getPosition).collect(Collectors.toSet());
        List<Integer> available = new ArrayList<>();
        for (int i = 1; i <= MAX_SLOTS_PER_CATEGORY_PER_DATE; i++) {
            if (!taken.contains(i)) {
                available.add(i);
            }
        }

        return SlotAvailabilityDto.builder()
                .date(checkDate)
                .availablePositions(available)
                .fullyBooked(available.isEmpty())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeaturedSlotDto> getShopBookedSlots(UUID shopId, User currentUser) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", shopId));
        verifyShopAccess(shop, currentUser);

        List<FeaturedSlot> slots = featuredSlotRepository.findByShopIdOrderBySlotDateDesc(shopId);
        return featuredSlotMapper.toDtoList(slots);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeaturedSlotDto> listAllSlots(LocalDate start, LocalDate end, User admin) {
        validateAdmin(admin);
        LocalDate s = start != null ? start : LocalDate.now().minusDays(7);
        LocalDate e = end != null ? end : LocalDate.now().plusDays(30);

        List<FeaturedSlot> slots = featuredSlotRepository.findBySlotDateBetweenOrderBySlotDateDesc(s, e);
        return featuredSlotMapper.toDtoList(slots);
    }

    @Override
    @Transactional
    public void recordClick(UUID slotId) {
        featuredSlotRepository.findById(slotId).ifPresent(slot -> {
            slot.setClicks(slot.getClicks() + 1);
            featuredSlotRepository.save(slot);
        });
    }

    @Override
    @Transactional
    public void recordImpressions(Collection<UUID> shopIds) {
        if (shopIds == null || shopIds.isEmpty()) return;
        LocalDate today = LocalDate.now();

        for (UUID shopId : shopIds) {
            List<FeaturedSlot> slots = featuredSlotRepository.findByShopIdOrderBySlotDateDesc(shopId);
            for (FeaturedSlot s : slots) {
                if (s.getSlotDate().equals(today) && Boolean.TRUE.equals(s.getActive())) {
                    s.setImpressions(s.getImpressions() + 1);
                    featuredSlotRepository.save(s);
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> getActiveFeaturedShopIds(UUID zoneId, UUID categoryId) {
        LocalDate today = LocalDate.now();
        List<FeaturedSlot> slots;
        if (categoryId != null) {
            slots = featuredSlotRepository.findByZoneIdAndCategoryIdAndSlotDateAndActiveTrueOrderByPositionAsc(zoneId, categoryId, today);
        } else {
            slots = featuredSlotRepository.findByZoneIdAndSlotDateAndActiveTrueOrderByPositionAsc(zoneId, today);
        }

        // BRD Section 21 Rule: "A closed shop is never shown as featured"
        return slots.stream()
                .map(FeaturedSlot::getShop)
                .filter(shop -> shop.getStatus() != ShopStatus.CLOSED)
                .map(Shop::getId)
                .collect(Collectors.toSet());
    }

    private void verifyShopAccess(Shop shop, User currentUser) {
        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required");
        }
        if (currentUser.getRole() == UserRole.ADMIN) {
            return;
        }
        if (shop.getOwner() == null || !shop.getOwner().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You are not authorized to book slots for this shop");
        }
    }

    private void validateAdmin(User admin) {
        if (admin == null) {
            throw new UnauthorizedException("Admin authentication required");
        }
        if (admin.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Administrative privileges required");
        }
    }
}

