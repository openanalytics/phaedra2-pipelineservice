package eu.openanalytics.phaedra.pipelineservice.execution.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import eu.openanalytics.phaedra.pipelineservice.service.PipelineTriggerService;

@Component
public class KafkaEventListener {

	@Autowired
	private PipelineTriggerService triggerService;
	
	@KafkaListener(topicPattern = ".*", groupId = "pipeline-service")
	public void processEvent(String message, 
			@Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
			@Header(name = KafkaHeaders.RECEIVED_TOPIC) String topic) {
		
		EventDescriptor event = EventDescriptor.of(topic, key, message);
		triggerService.matchAndFire(event);
	}
}
