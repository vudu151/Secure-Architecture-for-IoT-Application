package com.smartparking.service.impl;

import com.smartparking.dto.request.VehicleRequest;
import com.smartparking.dto.response.VehicleDTO;
import com.smartparking.entity.User;
import com.smartparking.entity.Vehicle;
import com.smartparking.exception.BadRequestException;
import com.smartparking.exception.ResourceNotFoundException;
import com.smartparking.repository.UserRepository;
import com.smartparking.repository.VehicleRepository;
import com.smartparking.service.VehicleService;
import com.smartparking.util.CryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final CryptoService cryptoService;

    @Override
    public List<VehicleDTO> getMyVehicles(Long userId) {
        log.info("Fetching vehicles for user id: {}", userId);
        return vehicleRepository.findByUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public VehicleDTO addVehicle(Long userId, VehicleRequest request) {
        log.info("Adding new vehicle for user id: {}, plate: {}", userId, request.getLicensePlate());
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (vehicleRepository.findByLicensePlate(request.getLicensePlate()).isPresent()) {
            throw new BadRequestException("License plate is already registered: " + request.getLicensePlate());
        }

        List<Vehicle> existingVehicles = vehicleRepository.findByUserId(userId);
        boolean isDefault = existingVehicles.isEmpty();

        String encryptedPlate;
        try {
            encryptedPlate = cryptoService.encrypt(request.getLicensePlate());
        } catch (Exception e) {
            log.error("Failed to encrypt license plate: {}", e.getMessage());
            throw new BadRequestException("Encryption failure");
        }

        Vehicle vehicle = Vehicle.builder()
                .user(user)
                .licensePlate(request.getLicensePlate()) // We keep plain text in DB column for basic indexing, but also store encrypted version
                .plateEncrypted(encryptedPlate)
                .vehicleType(request.getVehicleType())
                .isDefault(isDefault)
                .build();

        Vehicle saved = vehicleRepository.save(vehicle);
        return convertToDTO(saved);
    }

    @Override
    @Transactional
    public void deleteVehicle(Long userId, Long vehicleId) {
        log.info("Deleting vehicle id: {} for user id: {}", vehicleId, userId);
        
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + vehicleId));

        if (!vehicle.getUser().getId().equals(userId)) {
            throw new BadRequestException("You do not own this vehicle");
        }

        vehicleRepository.delete(vehicle);
    }

    private VehicleDTO convertToDTO(Vehicle v) {
        String decryptedPlate;
        try {
            decryptedPlate = cryptoService.decrypt(v.getPlateEncrypted());
        } catch (Exception e) {
            log.error("Failed to decrypt license plate for vehicle ID {}: {}", v.getId(), e.getMessage());
            decryptedPlate = v.getLicensePlate(); // Fallback
        }

        return VehicleDTO.builder()
                .id(v.getId())
                .licensePlate(decryptedPlate) // Return the decrypted license plate to user
                .vehicleType(v.getVehicleType())
                .isDefault(v.getIsDefault())
                .createdAt(v.getCreatedAt())
                .build();
    }
}
