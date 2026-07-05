package com.smartparking.repository;

import com.smartparking.entity.DeviceRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceRegistryRepository extends JpaRepository<DeviceRegistry, Long> {
    Optional<DeviceRegistry> findByDeviceUid(String deviceUid);
}
