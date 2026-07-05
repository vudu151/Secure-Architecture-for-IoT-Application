package com.smartparking.service;

import com.smartparking.dto.response.SlotDTO;
import com.smartparking.enums.SlotStatus;

import java.util.List;

public interface ParkingSlotService {
    List<SlotDTO> getAllSlots();
    List<SlotDTO> getAvailableSlots();
    SlotDTO getSlotById(Long id);
    SlotDTO updateSlotStatus(String slotCode, boolean occupied);
}
