package be.snife.sbms.review.services;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import be.snife.sbms.api.core.review.Review;
import be.snife.sbms.review.persistence.ReviewEntity;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

	// serviceAddress not in Entity
	@Mappings({ @Mapping(target = "serviceAddress", ignore = true) })
	Review entityToApi(ReviewEntity entity);

	// id & version not in DTO API
	@Mappings({ @Mapping(target = "id", ignore = true), @Mapping(target = "version", ignore = true) })
	ReviewEntity apiToEntity(Review api);

	List<Review> entityListToApiList(List<ReviewEntity> entity);

	List<ReviewEntity> apiListToEntityList(List<Review> api);
}