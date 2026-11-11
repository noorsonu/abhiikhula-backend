package in.abhiikhula.platformconfig.service;

import in.abhiikhula.platformconfig.dto.FeatureFlagDto;
import in.abhiikhula.platformconfig.dto.PlatformConfigDto;
import in.abhiikhula.platformconfig.dto.ToggleFlagRequest;
import in.abhiikhula.platformconfig.dto.UpdateConfigRequest;

import java.util.List;

public interface PlatformConfigService {

    List<PlatformConfigDto> getAllConfigs();

    String getStringConfig(String key, String defaultValue);

    int getIntConfig(String key, int defaultValue);

    double getDoubleConfig(String key, double defaultValue);

    PlatformConfigDto updateConfig(String key, UpdateConfigRequest request);

    List<FeatureFlagDto> getAllFlags();

    boolean isFlagEnabled(String key, boolean defaultState);

    FeatureFlagDto toggleFlag(String key, ToggleFlagRequest request);
}

