package in.abhiikhula.dispute.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Customer FAQ item (BRD FR-1306)")
public class SupportFaqDto {
    private String question;
    private String answer;
    private String category;
}

