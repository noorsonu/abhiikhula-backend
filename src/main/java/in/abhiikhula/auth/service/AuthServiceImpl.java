package in.abhiikhula.auth.service;

import in.abhiikhula.auth.dto.AuthResponse;
import in.abhiikhula.auth.dto.SendOtpRequest;
import in.abhiikhula.auth.dto.SendOtpResponse;
import in.abhiikhula.auth.dto.VerifyOtpRequest;
import in.abhiikhula.auth.jwt.JwtUtil;
import in.abhiikhula.auth.otp.OtpStore;
import in.abhiikhula.common.exception.ResourceNotFoundException;
import in.abhiikhula.common.exception.UnauthorizedException;
import in.abhiikhula.user.model.User;
import in.abhiikhula.user.model.UserRole;
import in.abhiikhula.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final OtpStore otpStore;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Override
    public SendOtpResponse sendOtp(SendOtpRequest request) {
        String phone = request.getPhone().trim();
        log.info("Sending OTP request for phone: {}", phone);

        String otp = otpStore.generateAndStore(phone);

        User user = userRepository.findByPhone(phone).orElse(null);
        boolean isRegistered = (user != null && !Boolean.TRUE.equals(user.getDeleted()));
        String existingName = user != null ? user.getName() : null;
        Integer existingAge = user != null ? user.getAge() : null;

        return SendOtpResponse.builder()
                .message("OTP sent successfully to " + phone + " (Dev OTP: 1234)")
                .expiresInSeconds(otpStore.ttlSeconds())
                .registered(isRegistered)
                .name(existingName)
                .age(existingAge)
                .build();
    }

    @Override
    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        String phone = request.getPhone().trim();
        String otp = request.getOtp().trim();
        log.info("Verifying OTP for phone: {}", phone);

        boolean isValid = otpStore.verify(phone, otp);
        if (!isValid) {
            throw new UnauthorizedException("Invalid or expired OTP. Please request a new one.");
        }

        // Check if user exists (Login) or is fresh (Register)
        boolean isNewUser = false;
        User user = userRepository.findByPhone(phone).orElse(null);

        if (user == null) {
            log.info("Phone {} not registered. Auto-registering new user.", phone);
            user = User.builder()
                    .phone(phone)
                    .name(request.getName() != null && !request.getName().isBlank() ? request.getName().trim() : null)
                    .age(request.getAge())
                    .role(UserRole.CUSTOMER)
                    .language("hi")
                    .blocked(false)
                    .build();
            user = userRepository.save(user);
            isNewUser = true;
        } else {
            boolean updated = false;
            if (request.getName() != null && !request.getName().isBlank() && (user.getName() == null || user.getName().isBlank())) {
                user.setName(request.getName().trim());
                updated = true;
            }
            if (request.getAge() != null && user.getAge() == null) {
                user.setAge(request.getAge());
                updated = true;
            }
            if (updated) {
                user = userRepository.save(user);
            }
            if (Boolean.TRUE.equals(user.getBlocked())) {
                throw new UnauthorizedException("Your account has been suspended. Please contact support.");
            } else if (Boolean.TRUE.equals(user.getDeleted())) {
                throw new UnauthorizedException("This account has been deleted. Please register with a new number or contact support.");
            }
        }

        String token = jwtUtil.generateToken(user);
        log.info("User {} authenticated successfully. New user: {}", user.getId(), isNewUser);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .phone(user.getPhone())
                .name(user.getName())
                .age(user.getAge())
                .role(user.getRole())
                .newUser(isNewUser)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public User getCurrentUser(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing or invalid Authorization header");
        }
        String jwt = token.substring(7);
        if (!jwtUtil.isValid(jwt)) {
            throw new UnauthorizedException("Expired or invalid JWT token");
        }
        UUID userId = jwtUtil.extractUserId(jwt);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        if (Boolean.TRUE.equals(user.getDeleted())) {
            throw new UnauthorizedException("Account has been deleted.");
        }
        if (Boolean.TRUE.equals(user.getBlocked())) {
            throw new UnauthorizedException("Account has been suspended.");
        }
        return user;
    }
}

