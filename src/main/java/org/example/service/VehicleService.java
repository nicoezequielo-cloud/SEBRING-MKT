package org.example.service;

import org.example.dto.VehicleRequest;
import org.example.exception.VehicleNotFoundException;
import org.example.model.Vehicle;
import org.example.repository.VehicleRepository;
import org.springframework.stereotype.Service;

@Service
public class VehicleService {

    private static final String DEFAULT_CURRENCY = "ARS";
    private static final String DEFAULT_STATUS = "available";
    private static final String DELETED_STATUS = "deleted";

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public Vehicle createVehicle(VehicleRequest request) {
        Vehicle vehicle = new Vehicle();
        applyRequest(vehicle, request);
        return vehicleRepository.save(vehicle);
    }

    public Vehicle updateVehicle(Integer id, VehicleRequest request) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new VehicleNotFoundException(id));
        applyRequest(vehicle, request);
        return vehicleRepository.save(vehicle);
    }

    // Logical delete: vehicles are referenced by transactions, so the row must stay in place.
    public void deleteVehicle(Integer id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new VehicleNotFoundException(id));
        vehicle.setStatus(DELETED_STATUS);
        vehicleRepository.save(vehicle);
    }

    private void applyRequest(Vehicle vehicle, VehicleRequest request) {
        vehicle.setBrand(request.getBrand());
        vehicle.setModel(request.getModel());
        vehicle.setVersion(request.getVersion());
        vehicle.setYear(request.getYear());
        vehicle.setPrice(request.getPrice());
        vehicle.setCurrency(blankToDefault(request.getCurrency(), DEFAULT_CURRENCY));
        vehicle.setStock(request.getStock() != null ? request.getStock() : 0);
        vehicle.setTransmission(request.getTransmission());
        vehicle.setFuelType(request.getFuelType());
        vehicle.setEngine(request.getEngine());
        vehicle.setImageUrl(request.getImageUrl());
        vehicle.setDescription(request.getDescription());
        vehicle.setStatus(blankToDefault(request.getStatus(), DEFAULT_STATUS));
    }

    private String blankToDefault(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
