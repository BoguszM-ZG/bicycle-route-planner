package com.bogusz.bicyclerouteplannerbackend.dto.route;

public record GpxExportDto(byte[] gpxData, String fileName) {
}
