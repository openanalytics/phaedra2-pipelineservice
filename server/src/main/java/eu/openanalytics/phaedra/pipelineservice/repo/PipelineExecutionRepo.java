package eu.openanalytics.phaedra.pipelineservice.repo;

import java.util.Date;
import java.util.List;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import eu.openanalytics.phaedra.pipelineservice.dto.PipelineExecution;

@Repository
public interface PipelineExecutionRepo extends CrudRepository<PipelineExecution, Long> {

	@Query("select * from pipeline_execution e where e.created_on >= :date1 and e.created_on <= :date2")
	List<PipelineExecution> findByCreatedOnRange(Date date1, Date date2);

}

