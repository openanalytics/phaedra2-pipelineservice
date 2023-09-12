package eu.openanalytics.phaedra.pipelineservice.execution.trigger.impl;

import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.jayway.jsonpath.JsonPath;

import eu.openanalytics.phaedra.pipelineservice.execution.PipelineExecutionContext;
import eu.openanalytics.phaedra.pipelineservice.execution.event.EventDescriptor;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.ITrigger;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.TriggerDescriptor;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.TriggerMatchType;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * A generic type of trigger that is based on (Kafka) events.
 * This means that the trigger will fire whenever an event is received
 * that matches:
 * 
 * <ul>
 * 	<li>A specific topic</li>
 * 	<li>A specific key</li>
 * 	<li>A specific message body</li>
 * </ul>
 * 
 * The topic and key are matched using String equality.
 * The body is matched using an optional JSONPath selector and either String equality or Regex matching.
 */
@Component
public class GenericEventTrigger implements ITrigger {

	private static final String TYPE = "GenericEventTrigger";
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Override
	public String getType() {
		return TYPE;
	}

	@SuppressWarnings("unchecked")
	@Override
	public TriggerMatchType matches(EventDescriptor event, TriggerDescriptor descriptor, PipelineExecutionContext ctx) {
		String filterTopic = (String) descriptor.getConfig().get("topic");
		if (!event.topic.equalsIgnoreCase(filterTopic)) return TriggerMatchType.NoMatch;
		
		String filterKey = (String) descriptor.getConfig().get("key");
		if (!event.key.equalsIgnoreCase(filterKey)) return TriggerMatchType.NoMatch;
		
		List<EventMatchCondition> errorConditions = (List<EventMatchCondition>) descriptor.getConfig().get("errorConditions");
		boolean errorConditionsMet = errorConditions != null && errorConditions.stream().allMatch(c -> matches(c, event));
		if (errorConditionsMet) return TriggerMatchType.Error;
		
		List<EventMatchCondition> matchConditions = (List<EventMatchCondition>) descriptor.getConfig().get("matchConditions");
		boolean matchConditionsMet = matchConditions == null || matchConditions.stream().allMatch(c -> matches(c, event));
		if (matchConditionsMet) return TriggerMatchType.Match;
		else return TriggerMatchType.NoMatch;
	}

	private boolean matches(EventMatchCondition condition, EventDescriptor event) {
		Object payload = event.message;
		String payloadSelector = condition.getPayloadSelector();
		if (payloadSelector != null) {
			try {
				payload = JsonPath.read((String) payload, payloadSelector);
			} catch (Exception e) {
				logger.debug(String.format("Event matching error using JSONPath selector '%s'", payloadSelector), e);
				return false;
			}
		}
		
		String pattern = condition.getValuePattern();
		boolean matchesPattern = pattern == null || Pattern.compile(pattern).matcher(String.valueOf(payload)).matches();
		if (!matchesPattern) return false;
		
		boolean matchesValue = condition.getValue() == null || payload.equals(condition.getValue());
		if (!matchesValue) return false;
		
		return true;
	}
	
	public static TriggerDescriptor buildTrigger(String topic, String key,
			List<EventMatchCondition> matchConditions, List<EventMatchCondition> errorConditions) {
		TriggerDescriptor trigger = new TriggerDescriptor();
		trigger.setType(TYPE);
		trigger.getConfig().put("topic", topic);
		trigger.getConfig().put("key", key);
		trigger.getConfig().put("matchConditions", matchConditions);
		trigger.getConfig().put("errorConditions", errorConditions);
		return trigger;
	}
	
	@Data
	@AllArgsConstructor
	public static class EventMatchCondition {
		private String payloadSelector;
		private String valuePattern;
		private Object value;
	}
}
