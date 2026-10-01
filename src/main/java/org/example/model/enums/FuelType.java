package org.example.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FuelType {
    NAFTA("Nafta"),
    DIESEL("Diesel"),
    HIBRIDO("Hibrido"),
    ELECTRICO("Electrico");

    private final String label;

    FuelType(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static FuelType fromLabel(String label) {
        for (FuelType fuelType : values()) {
            if (fuelType.label.equalsIgnoreCase(label)) {
                return fuelType;
            }
        }
        throw new IllegalArgumentException(
                "Invalid fuelType '" + label + "'. Allowed values: Nafta, Diesel, Hibrido, Electrico");
    }
}
