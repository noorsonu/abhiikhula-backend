package in.abhiikhula.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Merchant public reply to a customer review (BRD FR-1103)")
public class OwnerReplyRequest {

    @NotBlank(message = "Reply message cannot be empty")
    @Schema(description = "Official shop response to the customer", example = "Thank you for visiting! We always strive to serve on time.")
    private String reply;
}

