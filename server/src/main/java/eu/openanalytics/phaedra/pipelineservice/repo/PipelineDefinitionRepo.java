package eu.openanalytics.phaedra.pipelineservice.repo;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import eu.openanalytics.phaedra.pipelineservice.dto.PipelineDefinition;

@Repository
public interface PipelineDefinitionRepo extends CrudRepository<PipelineDefinition, Long> {

	List<PipelineDefinition> findAllByName(String name);

}
