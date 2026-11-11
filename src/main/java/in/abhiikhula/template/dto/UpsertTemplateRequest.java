package in.abhiikhula.template.dto;

import in.abhiikhula.template.model.TemplateChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Create or update notification template (BRD FR-1402)")
public class UpsertTemplateRequest {

    @NotBlank(message = "Slug is required")
    @Schema(example = "two_ahead")
    private String slug;

    @NotNull(message = "Channel is required")
    @Schema(example = "PUSH")
    private TemplateChannel channel;

    @NotBlank(message = "Locale is required")
    @Schema(example = "hi")
    private String locale;

    @NotBlank(message = "Title template is required")
    @Schema(example = "Aapka number aane wala hai!")
    private String titleTemplate;

    @NotBlank(message = "Body template is required")
    @Schema(example = "Aapke aage sirf 2 log hain {{shop}} par. Token #{{number}}.")
    private String bodyTemplate;

    @Schema(example = "shop,number,eta")
    private String variables;

    private Boolean active;
}

