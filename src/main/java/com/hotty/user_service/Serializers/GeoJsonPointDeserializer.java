package com.hotty.user_service.Serializers;

import java.io.IOException;

import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public class GeoJsonPointDeserializer extends JsonDeserializer<GeoJsonPoint> {
    @Override
    public GeoJsonPoint deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        JsonNode coords = node.get("coordinates");
        double lon = coords.get(0).asDouble();
        double lat = coords.get(1).asDouble();
        return new GeoJsonPoint(lon, lat);
    }
}