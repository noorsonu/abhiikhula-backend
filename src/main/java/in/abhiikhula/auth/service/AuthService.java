package in.abhiikhula.auth.service;

import in.abhiikhula.auth.dto.AuthResponse;
import in.abhiikhula.auth.dto.SendOtpRequest;
import in.abhiikhula.auth.dto.SendOtpResponse;
import in.abhiikhula.auth.dto.VerifyOtpRequest;
import in.abhiikhula.user.model.User;

public interface AuthService {

    SendOtpResponse sendOtp(SendOtpRequest request);

    AuthResponse verifyOtp(VerifyOtpRequest request);

    User getCurrentUser(String token);
}

