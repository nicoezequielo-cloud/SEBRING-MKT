package org.example.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.example.model.enums.FuelType;

@Converter(autoApply = true)
public class FuelTypeConverter implements AttributeConverter<FuelType, String> {

    @Override
    public String convertToDatabaseColumn(FuelType fuelType) {
        return fuelType != null ? fuelType.getLabel() : null;
    }

    @Override
    public FuelType convertToEntityAttribute(String dbValue) {
        return dbValue != null ? FuelType.fromLabel(dbValue) : null;
    }
}
