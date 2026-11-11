package in.abhiikhula.review.mapper;

import in.abhiikhula.review.dto.ReviewDto;
import in.abhiikhula.review.model.Review;
import in.abhiikhula.review.model.ReviewTag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(source = "shop.id", target = "shopId")
    @Mapping(source = "token.id", target = "tokenId")
    @Mapping(source = "token.displayToken", target = "displayToken")
    @Mapping(source = "tags", target = "tags", qualifiedByName = "stringToTagList")
    ReviewDto toDto(Review review);

    List<ReviewDto> toDtoList(List<Review> reviews);

    @Named("stringToTagList")
    default List<ReviewTag> stringToTagList(String tags) {
        if (tags == null || tags.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(tag -> {
                    try {
                        return ReviewTag.valueOf(tag);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(t -> t != null)
                .collect(Collectors.toList());
    }

    @Named("tagListToString")
    default String tagListToString(List<ReviewTag> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        return tags.stream().map(Enum::name).collect(Collectors.joining(","));
    }
}

