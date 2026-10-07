package org.example.controller;

import org.example.dto.ImageReorderRequest;
import org.example.exception.ImageNotFoundException;
import org.example.exception.VehicleNotFoundException;
import org.example.model.Vehicle;
import org.example.model.VehicleImage;
import org.example.repository.VehicleImageRepository;
import org.example.repository.VehicleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ImageController {

    private static final String UPLOAD_DIR = "uploads/vehicles";

    private final VehicleRepository vehicleRepository;
    private final VehicleImageRepository imageRepository;

    public ImageController(VehicleRepository vehicleRepository, VehicleImageRepository imageRepository) {
        this.vehicleRepository = vehicleRepository;
        this.imageRepository = imageRepository;
    }

    @PostMapping("/vehicles/{id}/images")
    public List<VehicleImage> uploadImages(@PathVariable Integer id,
                                           @RequestParam("file") MultipartFile[] files) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new VehicleNotFoundException(id));

        File folder = new File(UPLOAD_DIR + "/" + id).getAbsoluteFile();
        if (!folder.exists() && !folder.mkdirs()) {
            throw new RuntimeException("Could not create upload folder " + folder.getAbsolutePath());
        }

        List<VehicleImage> saved = new ArrayList<>();
        int nextOrder = nextSortOrder(id);
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }
            String extension = getExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID().toString().replace("-", "") + extension;
            File dest = new File(folder, filename).getAbsoluteFile();
            try {
                file.transferTo(dest);
            } catch (IOException e) {
                throw new RuntimeException("Error saving uploaded file", e);
            }
            VehicleImage image = new VehicleImage();
            image.setVehicle(vehicle);
            image.setUrl("/uploads/vehicles/" + id + "/" + filename);
            image.setSortOrder(nextOrder++);
            saved.add(imageRepository.save(image));
        }
        return saved;
    }

    @PostMapping("/vehicles/{id}/images/reorder")
    public List<VehicleImage> reorderImages(@PathVariable Integer id,
                                            @RequestBody ImageReorderRequest request) {
        List<VehicleImage> images = imageRepository.findByVehicleIdOrderBySortOrderAscIdAsc(id);
        Map<Integer, VehicleImage> byId = new HashMap<>();
        for (VehicleImage image : images) {
            byId.put(image.getId(), image);
        }
        int position = 0;
        for (Integer imageId : request.getImageIds()) {
            VehicleImage image = byId.get(imageId);
            if (image == null) {
                continue;
            }
            image.setSortOrder(position++);
            imageRepository.save(image);
        }
        return imageRepository.findByVehicleIdOrderBySortOrderAscIdAsc(id);
    }

    private int nextSortOrder(Integer vehicleId) {
        return imageRepository.findByVehicleIdOrderBySortOrderAscIdAsc(vehicleId).size();
    }

    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable Integer imageId) {
        VehicleImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ImageNotFoundException(imageId));

        String relative = image.getUrl();
        File file = new File(relative.startsWith("/") ? relative.substring(1) : relative).getAbsoluteFile();
        if (file.exists()) {
            file.delete();
        }
        imageRepository.delete(image);
        return ResponseEntity.noContent().build();
    }

    private String getExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }
}