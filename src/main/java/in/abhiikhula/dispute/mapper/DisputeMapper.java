package in.abhiikhula.dispute.mapper;

import in.abhiikhula.dispute.dto.DisputeResponseDto;
import in.abhiikhula.dispute.model.Dispute;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DisputeMapper {

    @Mapping(source = "shop.id", target = "shopId")
    @Mapping(source = "shop.name", target = "shopName")
    @Mapping(source = "token.id", target = "tokenId")
    @Mapping(source = "token.displayToken", target = "displayToken")
    @Mapping(source = "customerPhone", target = "customerPhone", qualifiedByName = "maskPhone")
    DisputeResponseDto toDto(Dispute dispute);

    List<DisputeResponseDto> toDtoList(List<Dispute> disputes);

    @Named("maskPhone")
    default String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "XXXXXX";
        }
        return "XXXXXX" + phone.substring(phone.length() - 4);
    }
}

