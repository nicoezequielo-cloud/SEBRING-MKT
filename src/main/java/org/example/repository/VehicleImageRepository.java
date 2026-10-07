package org.example.repository;

import org.example.model.VehicleImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleImageRepository extends JpaRepository<VehicleImage, Integer> {

    List<VehicleImage> findByVehicleIdOrderBySortOrderAscIdAsc(Integer vehicleId);
}