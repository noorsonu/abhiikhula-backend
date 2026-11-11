package in.abhiikhula.review.dto;

import in.abhiikhula.review.model.ReviewTag;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Customer verified review representation")
public class ReviewDto {

    private UUID id;
    private UUID shopId;
    private UUID tokenId;
    private String displayToken;
    private String customerName;
    private Integer stars;
    private String text;
    private List<ReviewTag> tags;
    private Boolean verifiedToken;
    private String reply;
    private Instant replyAt;
    private Boolean hidden;
    private Boolean abuseReported;
    private Instant createdAt;
}

