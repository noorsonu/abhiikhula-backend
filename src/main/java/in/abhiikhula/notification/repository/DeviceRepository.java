package in.abhiikhula.notification.repository;

import in.abhiikhula.notification.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {

    Optional<Device> findByUserIdAndFcmToken(UUID userId, String fcmToken);

    List<Device> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}

