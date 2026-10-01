package org.example.exception;

public class VehicleNotFoundException extends RuntimeException {
    public VehicleNotFoundException(Integer id) {
        super("Vehicle with id " + id + " not found");
    }
}
