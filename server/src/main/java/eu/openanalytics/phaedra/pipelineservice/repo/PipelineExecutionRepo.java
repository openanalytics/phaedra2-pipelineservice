package eu.openanalytics.phaedra.pipelineservice.repo;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import eu.openanalytics.phaedra.pipelineservice.dto.PipelineExecution;

@Repository
public interface PipelineExecutionRepo extends CrudRepository<PipelineExecution, Long> {

}

