package hr.ingemark.assignment.productapi.util;

import hr.ingemark.assignment.productapi.dto.ProductResponse;
import hr.ingemark.assignment.productapi.model.ProductEntity;

import org.mapstruct.Mapper;

/**
 * Implementation ({@code ProductMapperImpl}) is generated at compile time by the MapStruct
 * annotation processor - see {@code target/generated-sources/annotations} after a build.
 *
 */
@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductResponse toResponse(ProductEntity product);
}
