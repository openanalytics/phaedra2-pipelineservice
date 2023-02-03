package eu.openanalytics.phaedra.pipelineservice.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import eu.openanalytics.phaedra.pipelineservice.dto.PipelineDefinition;
import eu.openanalytics.phaedra.pipelineservice.service.PipelineDefinitionService;

@RestController
public class PipelineDefinitionController {

	@Autowired
	private PipelineDefinitionService pipelineDefinitionService;
	
	@GetMapping(value = "/pipeline/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<PipelineDefinition> getPipelineDefinition(@PathVariable long id) {
		return ResponseEntity.of(pipelineDefinitionService.findById(id));
	}

	@GetMapping(value = "/pipelines", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<PipelineDefinition>> getAllPipelineDefinitions() {
		return ResponseEntity.ok(pipelineDefinitionService.findAll(null));
	}
	
	@PostMapping(value = "/pipeline", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PipelineDefinition> createPipelineDefinition(@RequestBody PipelineDefinition definition) {
		try {
			PipelineDefinition result = pipelineDefinitionService.createNew(definition);
			return new ResponseEntity<>(result, HttpStatus.CREATED);
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
		} 
    }
	
    @PutMapping(value = "/pipeline", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PipelineDefinition> updatePipelineDefinition(@RequestBody PipelineDefinition definition) {
    	try {
	    	PipelineDefinition result = pipelineDefinitionService.update(definition);
	        return new ResponseEntity<>(result, HttpStatus.OK);
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
		} 
    }
}
