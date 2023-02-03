package eu.openanalytics.phaedra.pipelineservice.repo;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import eu.openanalytics.phaedra.pipelineservice.dto.PipelineExecutionLog;

@Repository
public interface PipelineExecutionLogRepo extends CrudRepository<PipelineExecutionLog, Long> {

	List<PipelineExecutionLog> findAllByPipelineExecutionId(Long pipelineExecutionId);

}
