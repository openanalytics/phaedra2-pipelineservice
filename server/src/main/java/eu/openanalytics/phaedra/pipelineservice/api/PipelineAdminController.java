package eu.openanalytics.phaedra.pipelineservice.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import eu.openanalytics.phaedra.pipelineservice.dto.PipelineDefinition;
import eu.openanalytics.phaedra.pipelineservice.service.PipelineTriggerService;

@RestController
@RequestMapping("/pipeline-admin")
public class PipelineAdminController {

	@Autowired
	protected PipelineTriggerService triggerService;
	
	@PostMapping("/message")
    public ResponseEntity<PipelineDefinition> postMessage(
    		@RequestBody String message, 
    		@RequestParam(name = "topic") String topic,
    		@RequestParam(name = "key") String key) {
		
		try {
			triggerService.postKafkaMessage(topic, key, message);
			return ResponseEntity.ok().build();
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
		} 
    }
	
}
