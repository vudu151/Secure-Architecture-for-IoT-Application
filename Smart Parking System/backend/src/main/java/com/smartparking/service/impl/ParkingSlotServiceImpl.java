package com.smartparking.service.impl;

import com.smartparking.dto.response.SlotDTO;
import com.smartparking.entity.ParkingSlot;
import com.smartparking.enums.SlotStatus;
import com.smartparking.exception.ResourceNotFoundException;
import com.smartparking.repository.ParkingSlotRepository;
import com.smartparking.service.ParkingSlotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParkingSlotServiceImpl implements ParkingSlotService {

    private final ParkingSlotRepository parkingSlotRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public List<SlotDTO> getAllSlots() {
        return parkingSlotRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<SlotDTO> getAvailableSlots() {
        return parkingSlotRepository.findByStatus(SlotStatus.AVAILABLE).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public SlotDTO getSlotById(Long id) {
        ParkingSlot slot = parkingSlotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parking slot not found with id: " + id));
        return convertToDTO(slot);
    }

    @Override
    @Transactional
    public SlotDTO updateSlotStatus(String slotCode, boolean occupied) {
        log.info("Updating status for slot {} to occupied={}", slotCode, occupied);
        
        ParkingSlot slot = parkingSlotRepository.findBySlotCode(slotCode)
                .orElseThrow(() -> new ResourceNotFoundException("Parking slot not found with code: " + slotCode));

        SlotStatus oldStatus = slot.getStatus();
        SlotStatus newStatus = oldStatus;

        if (occupied) {
            newStatus = SlotStatus.OCCUPIED;
        } else {
            // If it was occupied and now it is vacant, it becomes AVAILABLE
            if (oldStatus == SlotStatus.OCCUPIED) {
                newStatus = SlotStatus.AVAILABLE;
            }
            // If it was RESERVED but still vacant, keep it RESERVED until checked in or expired
        }

        if (oldStatus != newStatus) {
            slot.setStatus(newStatus);
            ParkingSlot updatedSlot = parkingSlotRepository.save(slot);
            SlotDTO dto = convertToDTO(updatedSlot);
            
            // Broadcast the update via WebSocket
            try {
                messagingTemplate.convertAndSend("/topic/slots", dto);
                log.info("Broadcasted slot status update to WebSocket for slot: {}", slotCode);
            } catch (Exception e) {
                log.error("Failed to broadcast WebSocket update for slot {}: {}", slotCode, e.getMessage());
            }
            
            return dto;
        }

        return convertToDTO(slot);
    }

    private SlotDTO convertToDTO(ParkingSlot slot) {
        return SlotDTO.builder()
                .id(slot.getId())
                .slotCode(slot.getSlotCode())
                .zone(slot.getZone())
                .status(slot.getStatus())
                .sensorId(slot.getSensorId())
                .build();
    }
}
