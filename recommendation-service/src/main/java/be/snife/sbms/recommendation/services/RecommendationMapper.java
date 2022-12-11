package be.snife.sbms.recommendation.services;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import be.snife.sbms.api.core.recommendation.Recommendation;
import be.snife.sbms.recommendation.persistence.RecommendationEntity;

@Mapper(componentModel = "spring")
public interface RecommendationMapper {

	// rate in api model is rating in entity model
	// serviceAddress not in entity model
	@Mappings({ @Mapping(target = "rate", source = "entity.rating"),
			@Mapping(target = "serviceAddress", ignore = true) })
	Recommendation entityToApi(RecommendationEntity entity);

	// rate in api model is rating in entity model
	// version & id not in API model
	@Mappings({ @Mapping(target = "rating", source = "api.rate"), @Mapping(target = "id", ignore = true),
			@Mapping(target = "version", ignore = true) })
	RecommendationEntity apiToEntity(Recommendation api);

	List<Recommendation> entityListToApiList(List<RecommendationEntity> entity);

	List<RecommendationEntity> apiListToEntityList(List<Recommendation> api);
}