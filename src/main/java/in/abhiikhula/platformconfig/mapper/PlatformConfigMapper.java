package in.abhiikhula.platformconfig.mapper;

import in.abhiikhula.platformconfig.dto.FeatureFlagDto;
import in.abhiikhula.platformconfig.dto.PlatformConfigDto;
import in.abhiikhula.platformconfig.model.FeatureFlag;
import in.abhiikhula.platformconfig.model.PlatformConfig;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PlatformConfigMapper {

    PlatformConfigDto toDto(PlatformConfig config);

    List<PlatformConfigDto> toConfigDtoList(List<PlatformConfig> configs);

    FeatureFlagDto toDto(FeatureFlag flag);

    List<FeatureFlagDto> toFlagDtoList(List<FeatureFlag> flags);
}

