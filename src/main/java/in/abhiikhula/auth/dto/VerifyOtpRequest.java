package in.abhiikhula.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to verify OTP and obtain JWT token")
public class VerifyOtpRequest {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
    @Schema(description = "10-digit Indian mobile number", example = "9876543210")
    private String phone;

    @NotBlank(message = "OTP is required")
    @Size(min = 4, max = 6, message = "OTP must be between 4 and 6 digits")
    @Pattern(regexp = "\\d{4,6}", message = "OTP must be numeric")
    @Schema(description = "OTP received on phone (Dev hardcoded: 1234)", example = "1234")
    private String otp;
}

