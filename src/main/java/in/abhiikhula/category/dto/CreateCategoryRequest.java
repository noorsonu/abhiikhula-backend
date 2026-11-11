package in.abhiikhula.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload to register a new category / vertical")
public class CreateCategoryRequest {

    @NotBlank(message = "Category name is mandatory")
    @Schema(description = "Category name", example = "Medical & Pharmacy")
    private String name;

    @NotBlank(message = "Category slug is mandatory")
    @Schema(description = "Category slug", example = "medical-pharmacy")
    private String slug;

    @Schema(description = "Icon name/identifier", example = "pill")
    private String icon;

    @Min(value = 1, message = "Default service duration must be at least 1 minute")
    @Schema(description = "Default service time in minutes", example = "5")
    @Builder.Default
    private Integer defaultServiceMin = 15;

    @Schema(description = "Whether category is active", example = "true")
    @Builder.Default
    private Boolean active = true;
}

