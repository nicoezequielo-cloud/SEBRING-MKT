package org.example.exception;

public class ImageNotFoundException extends RuntimeException {
    public ImageNotFoundException(Integer id) {
        super("Image with id " + id + " not found");
    }
}