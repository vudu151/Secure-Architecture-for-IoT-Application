package com.smartparking.service;

import com.smartparking.dto.request.VehicleRequest;
import com.smartparking.dto.response.VehicleDTO;
import java.util.List;

public interface VehicleService {
    List<VehicleDTO> getMyVehicles(Long userId);
    VehicleDTO addVehicle(Long userId, VehicleRequest request);
    void deleteVehicle(Long userId, Long vehicleId);
}
