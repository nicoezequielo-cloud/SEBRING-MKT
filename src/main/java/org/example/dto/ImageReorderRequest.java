package org.example.dto;

import java.util.List;

public class ImageReorderRequest {

    private List<Integer> imageIds;

    public List<Integer> getImageIds() {
        return imageIds;
    }

    public void setImageIds(List<Integer> imageIds) {
        this.imageIds = imageIds;
    }
}