package eu.openanalytics.phaedra.pipelineservice.execution.trigger;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import eu.openanalytics.phaedra.pipelineservice.service.PipelineTriggerService;
import eu.openanalytics.phaedra.pipelineservice.service.PipelineTriggerService.RegisteredTrigger;

@Component
public class KafkaEventListener {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private PipelineTriggerService triggerService;
	
	@KafkaListener(topicPattern = ".*")
	public void processEvent(String message, 
			@Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key,
			@Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
		
		EventDescriptor event = EventDescriptor.of(topic, key, message);
		logger.debug(String.format("Consuming event: %s", event));
		
		RegisteredTrigger trigger = triggerService.findMatchingTrigger(rt -> matchesTrigger(event, rt.descriptor));
		if (trigger != null) {
			triggerService.fireTriggerNow(trigger);
		}
	}
	
	private boolean matchesTrigger(EventDescriptor event, TriggerDescriptor trigger) {
		String stepTopic = String.valueOf(trigger.getConfig().get("topic"));
		String stepKey = String.valueOf(trigger.getConfig().get("key"));
		String stepPattern = String.valueOf(trigger.getConfig().get("pattern"));
		
		return (PipelineTriggerService.TRIGGER_TYPE_EVENT_LISTENER.equalsIgnoreCase(trigger.getType())
				&& event.topic.equalsIgnoreCase(stepTopic)
				&& event.key.equalsIgnoreCase(stepKey)
				&& Pattern.compile(stepPattern).matcher(event.message).matches());
	}
	
	private static class EventDescriptor {
		
		public String topic;
		public String key;
		public String message;
		
		public static EventDescriptor of(String topic, String key, String message) {
			EventDescriptor descriptor = new EventDescriptor();
			descriptor.topic = topic;
			descriptor.key = key;
			descriptor.message = message;
			return descriptor;
		}
		
		@Override
		public String toString() {
			return String.format("Event [topic=%s] [key=%s]: %s", topic, key, message);
		}
	}
}
