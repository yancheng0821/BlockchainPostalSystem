package com.postal.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.postal.dao.ParcelDao;

import java.io.IOException;

public class ParcelStatusDeserializer extends JsonDeserializer<ParcelDao.ParcelStatus> {

    @Override
    public ParcelDao.ParcelStatus deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText().toUpperCase();
        return ParcelDao.ParcelStatus.valueOf(value);
    }
}
