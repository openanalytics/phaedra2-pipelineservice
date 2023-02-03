package eu.openanalytics.phaedra.pipelineservice.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import eu.openanalytics.phaedra.pipelineservice.dto.PipelineExecution;
import eu.openanalytics.phaedra.pipelineservice.service.PipelineExecutionService;

@RestController
public class PipelineExecutionController {

	@Autowired
	private PipelineExecutionService pipelineExecutionService;
	
	@GetMapping(value = "/execution/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<PipelineExecution> getPipelineExecution(@PathVariable long id) {
		return ResponseEntity.of(pipelineExecutionService.findById(id));
	}
	
	@GetMapping(value = "/executions", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<PipelineExecution>> getAllPipelineExecutions() {
		return ResponseEntity.ok(pipelineExecutionService.findAll(null));
	}	
}
