package com.moduplaylist.core.content.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ContentTypeConverter implements AttributeConverter<ContentType, String> {
    @Override
    public String convertToDatabaseColumn(ContentType type) {
        return type == null ? null : type.getValue();
    }

    @Override
    public ContentType convertToEntityAttribute(String value) {
        return value == null ? null : ContentType.fromValue(value);
    }
}
