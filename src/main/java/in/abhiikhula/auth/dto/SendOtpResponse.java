package in.abhiikhula.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response after OTP is successfully sent")
public class SendOtpResponse {

    @Schema(description = "Human-readable message", example = "OTP sent to 9876543210 (Dev OTP: 1234)")
    private String message;

    @Schema(description = "OTP expiry in seconds", example = "120")
    private int expiresInSeconds;

    @Schema(description = "Whether this phone is already registered as a customer")
    private boolean registered;

    @Schema(description = "Existing customer name if already registered", example = "Abhishek Sharma")
    private String name;

    @Schema(description = "Existing customer age if already registered", example = "25")
    private Integer age;
}

