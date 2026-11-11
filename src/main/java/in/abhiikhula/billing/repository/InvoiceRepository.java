package in.abhiikhula.billing.repository;

import in.abhiikhula.billing.model.Invoice;
import in.abhiikhula.billing.model.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    List<Invoice> findByShopIdOrderByCreatedAtDesc(UUID shopId);

    List<Invoice> findByStatusOrderByCreatedAtDesc(InvoiceStatus status);

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    long count();
}

