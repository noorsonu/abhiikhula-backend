package in.abhiikhula.zone.service;

import in.abhiikhula.zone.dto.CreateZoneRequest;
import in.abhiikhula.zone.dto.ZoneResponseDto;

import java.util.List;
import java.util.UUID;

public interface ZoneService {

    List<ZoneResponseDto> getAllActiveZones();

    ZoneResponseDto getZoneById(UUID id);

    ZoneResponseDto createZone(CreateZoneRequest request);
}

