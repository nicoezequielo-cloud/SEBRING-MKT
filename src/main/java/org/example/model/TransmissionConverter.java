package org.example.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.example.model.enums.Transmission;

@Converter(autoApply = true)
public class TransmissionConverter implements AttributeConverter<Transmission, String> {

    @Override
    public String convertToDatabaseColumn(Transmission transmission) {
        return transmission != null ? transmission.getLabel() : null;
    }

    @Override
    public Transmission convertToEntityAttribute(String dbValue) {
        return dbValue != null ? Transmission.fromLabel(dbValue) : null;
    }
}
