package in.abhiikhula.shop.service;

import in.abhiikhula.shop.dto.ShopStatusResponseDto;
import in.abhiikhula.shop.dto.UpdateStatusRequest;
import in.abhiikhula.user.model.User;

import java.util.UUID;

public interface ShopStatusService {

    ShopStatusResponseDto updateStatus(UUID shopId, UpdateStatusRequest request, User currentUser);

    ShopStatusResponseDto getStatus(UUID shopId);
}

