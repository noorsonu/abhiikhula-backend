package in.abhiikhula.user.service;

import in.abhiikhula.user.dto.DeleteAccountRequest;
import in.abhiikhula.user.dto.UpdateUserProfileRequest;
import in.abhiikhula.user.dto.UserProfileResponseDto;
import in.abhiikhula.user.model.User;

public interface UserService {

    UserProfileResponseDto getProfile(User currentUser);

    UserProfileResponseDto updateProfile(User currentUser, UpdateUserProfileRequest request);

    void deleteAccount(User currentUser, DeleteAccountRequest request);
}

