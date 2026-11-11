package in.abhiikhula.billing.dto;

import in.abhiikhula.billing.model.InvoiceStatus;
import in.abhiikhula.billing.model.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Invoice record representation")
public class InvoiceResponseDto {
    private UUID id;
    private UUID shopId;
    private String invoiceNumber;
    private BigDecimal amount;
    private String currency;
    private InvoiceStatus status;
    private PaymentMethod paymentMethod;
    private String paymentRef;
    private String notes;
    private Instant paidAt;
    private Instant createdAt;
}

