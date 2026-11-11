package in.abhiikhula.category.mapper;

import in.abhiikhula.category.dto.CategoryResponseDto;
import in.abhiikhula.category.dto.CreateCategoryRequest;
import in.abhiikhula.category.model.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryResponseDto toDto(Category entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Category toEntity(CreateCategoryRequest request);
}
