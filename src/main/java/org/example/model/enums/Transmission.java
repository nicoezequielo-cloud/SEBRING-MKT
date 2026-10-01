package org.example.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Transmission {
    MANUAL("Manual"),
    AUTOMATICA("Automática");

    private final String label;

    Transmission(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static Transmission fromLabel(String label) {
        for (Transmission transmission : values()) {
            if (transmission.label.equalsIgnoreCase(label)) {
                return transmission;
            }
        }
        throw new IllegalArgumentException(
                "Invalid transmission '" + label + "'. Allowed values: Manual, Automática");
    }
}
