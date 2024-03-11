package eu.openanalytics.phaedra.pipelineservice.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import eu.openanalytics.phaedra.pipelineservice.dto.PipelineDefinition;
import eu.openanalytics.phaedra.pipelineservice.service.PipelineDefinitionService;

@RestController
@RequestMapping("/pipeline-definitions")
public class PipelineDefinitionController {

	@Autowired
	private PipelineDefinitionService pipelineDefinitionService;

	@GetMapping("/{id}")
	public ResponseEntity<PipelineDefinition> getPipelineDefinition(@PathVariable long id) {
		return ResponseEntity.of(pipelineDefinitionService.findById(id));
	}

	@GetMapping
	public ResponseEntity<List<PipelineDefinition>> getAllPipelineDefinitions() {
		return ResponseEntity.ok(pipelineDefinitionService.findAll(null));
	}

	@PostMapping
    public ResponseEntity<PipelineDefinition> createPipelineDefinition(@RequestBody PipelineDefinition definition) {
		try {
			PipelineDefinition result = pipelineDefinitionService.createNew(definition);
			return new ResponseEntity<>(result, HttpStatus.CREATED);
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
		}
    }

    @PutMapping("/{id}")
    public ResponseEntity<PipelineDefinition> updatePipelineDefinition(@PathVariable long id, @RequestBody PipelineDefinition definition) {
    	try {
    		definition.setId(id);
	    	PipelineDefinition result = pipelineDefinitionService.update(definition);
	        return new ResponseEntity<>(result, HttpStatus.OK);
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
		}
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePipelineDefinition(@PathVariable long id) {
    	try {
    		pipelineDefinitionService.delete(id);
    		return new ResponseEntity<>(HttpStatus.OK);
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
		}
    }
}
