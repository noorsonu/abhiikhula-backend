package in.abhiikhula.claim.mapper;

import in.abhiikhula.claim.dto.ClaimResponseDto;
import in.abhiikhula.claim.model.ShopClaim;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ClaimMapper {

    @Mapping(target = "shopId", source = "shop.id")
    @Mapping(target = "shopName", source = "shop.name")
    @Mapping(target = "applicantId", source = "applicant.id")
    @Mapping(target = "applicantName", source = "applicant.name")
    @Mapping(target = "reviewedByAdminId", source = "reviewedBy.id")
    ClaimResponseDto toDto(ShopClaim claim);

    List<ClaimResponseDto> toDtoList(List<ShopClaim> claims);
}

