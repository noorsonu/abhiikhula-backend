package in.abhiikhula.platformconfig.service;

import in.abhiikhula.platformconfig.dto.FeatureFlagDto;
import in.abhiikhula.platformconfig.dto.PlatformConfigDto;
import in.abhiikhula.platformconfig.dto.ToggleFlagRequest;
import in.abhiikhula.platformconfig.dto.UpdateConfigRequest;
import in.abhiikhula.platformconfig.mapper.PlatformConfigMapper;
import in.abhiikhula.platformconfig.model.ConfigScope;
import in.abhiikhula.platformconfig.model.FeatureFlag;
import in.abhiikhula.platformconfig.model.PlatformConfig;
import in.abhiikhula.platformconfig.repository.FeatureFlagRepository;
import in.abhiikhula.platformconfig.repository.PlatformConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformConfigServiceImpl implements PlatformConfigService {

    private final PlatformConfigRepository configRepository;
    private final FeatureFlagRepository flagRepository;
    private final PlatformConfigMapper configMapper;

    // Fast in-memory read caches to avoid hitting PostgreSQL on every token/search request
    private final Map<String, String> configCache = new ConcurrentHashMap<>();
    private final Map<String, Boolean> flagCache = new ConcurrentHashMap<>();

    @Override
    @Transactional(readOnly = true)
    public List<PlatformConfigDto> getAllConfigs() {
        return configMapper.toConfigDtoList(configRepository.findAllByOrderByConfigKeyAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public String getStringConfig(String key, String defaultValue) {
        return configCache.computeIfAbsent(key, k -> configRepository
                .findByConfigKeyAndScope(k, ConfigScope.GLOBAL)
                .map(PlatformConfig::getConfigValue)
                .orElse(defaultValue));
    }

    @Override
    public int getIntConfig(String key, int defaultValue) {
        String val = getStringConfig(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid integer config for key {}: '{}'. Returning default {}", key, val, defaultValue);
            return defaultValue;
        }
    }

    @Override
    public double getDoubleConfig(String key, double defaultValue) {
        String val = getStringConfig(key, String.valueOf(defaultValue));
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid double config for key {}: '{}'. Returning default {}", key, val, defaultValue);
            return defaultValue;
        }
    }

    @Override
    @Transactional
    public PlatformConfigDto updateConfig(String key, UpdateConfigRequest request) {
        ConfigScope scope = request.getScope() != null ? request.getScope() : ConfigScope.GLOBAL;
        log.info("Updating platform config: {} (scope: {}) to '{}'", key, scope, request.getValue());

        PlatformConfig config = (scope == ConfigScope.GLOBAL)
                ? configRepository.findByConfigKeyAndScope(key, ConfigScope.GLOBAL)
                .orElseGet(() -> PlatformConfig.builder()
                        .configKey(key)
                        .scope(ConfigScope.GLOBAL)
                        .build())
                : configRepository.findByConfigKeyAndScopeAndScopeId(key, scope, request.getScopeId())
                .orElseGet(() -> PlatformConfig.builder()
                        .configKey(key)
                        .scope(scope)
                        .scopeId(request.getScopeId())
                        .build());

        config.setConfigValue(request.getValue().trim());
        if (request.getDescription() != null) {
            config.setDescription(request.getDescription());
        }

        PlatformConfig saved = configRepository.save(config);

        // Invalidate in-memory cache
        configCache.remove(key);

        return configMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeatureFlagDto> getAllFlags() {
        return configMapper.toFlagDtoList(flagRepository.findAllByOrderByFlagKeyAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFlagEnabled(String key, boolean defaultState) {
        return flagCache.computeIfAbsent(key, k -> flagRepository
                .findByFlagKeyAndScope(k, ConfigScope.GLOBAL)
                .map(FeatureFlag::getEnabled)
                .orElse(defaultState));
    }

    @Override
    @Transactional
    public FeatureFlagDto toggleFlag(String key, ToggleFlagRequest request) {
        ConfigScope scope = request.getScope() != null ? request.getScope() : ConfigScope.GLOBAL;
        log.info("Toggling feature flag: {} (scope: {}) to enabled={}", key, scope, request.getEnabled());

        FeatureFlag flag = (scope == ConfigScope.GLOBAL)
                ? flagRepository.findByFlagKeyAndScope(key, ConfigScope.GLOBAL)
                .orElseGet(() -> FeatureFlag.builder()
                        .flagKey(key)
                        .scope(ConfigScope.GLOBAL)
                        .build())
                : flagRepository.findByFlagKeyAndScopeAndScopeId(key, scope, request.getScopeId())
                .orElseGet(() -> FeatureFlag.builder()
                        .flagKey(key)
                        .scope(scope)
                        .scopeId(request.getScopeId())
                        .build());

        flag.setEnabled(request.getEnabled());
        if (request.getDescription() != null) {
            flag.setDescription(request.getDescription());
        }

        FeatureFlag saved = flagRepository.save(flag);

        // Invalidate in-memory cache
        flagCache.remove(key);

        return configMapper.toDto(saved);
    }
}

