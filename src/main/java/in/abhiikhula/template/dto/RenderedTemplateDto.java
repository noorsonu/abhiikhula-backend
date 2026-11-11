package in.abhiikhula.template.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Rendered template preview result")
public class RenderedTemplateDto {

    private String title;
    private String body;
    private String channel;
    private String locale;
    private boolean testSent;
    private String testRecipient;
}

