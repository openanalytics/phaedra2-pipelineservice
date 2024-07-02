package eu.openanalytics.phaedra.pipelineservice.execution.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import eu.openanalytics.phaedra.pipelineservice.service.PipelineTriggerService;

@Component
public class KafkaEventListener {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private PipelineTriggerService triggerService;
	
	@KafkaListener(topicPattern = ".*", groupId = "pipeline-service")
	public void processEvent(String message, 
			@Header(KafkaHeaders.RECEIVED_KEY) String key,
			@Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
		
		EventDescriptor event = EventDescriptor.of(topic, key, message);
		logger.debug(String.format("Consuming event: %s", event));
		if (!triggerService.matchAndFire(event)) {
			logger.debug(String.format("Discarded event (no match): %s", event));
		}
	}
}
