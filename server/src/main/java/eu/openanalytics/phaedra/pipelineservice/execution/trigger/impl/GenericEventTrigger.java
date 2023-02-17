package eu.openanalytics.phaedra.pipelineservice.execution.trigger.impl;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import eu.openanalytics.phaedra.pipelineservice.execution.PipelineExecutionContext;
import eu.openanalytics.phaedra.pipelineservice.execution.event.EventDescriptor;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.ITrigger;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.TriggerDescriptor;

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

	@Override
	public boolean matches(EventDescriptor event, TriggerDescriptor descriptor, PipelineExecutionContext ctx) {
		String filterTopic = (String) descriptor.getConfig().get("topic");
		String filterKey = (String) descriptor.getConfig().get("key");
		String filterSelector = (String) descriptor.getConfig().get("selector");
		String filterPattern = (String) descriptor.getConfig().get("pattern");
		Object filterValue = descriptor.getConfig().get("value");
		
		Object matchValue = event.message;
		if (filterSelector != null) {
			try {
				matchValue = JsonPath.read((String) matchValue, filterSelector);
			} catch (PathNotFoundException e) {
				return false;
			} catch (Exception e) {
				logger.debug(String.format("Event matching error using JSONPath selector '%s'", filterSelector), e);
				return false;
			}
		}
		
		boolean matchesPattern = filterPattern == null || Pattern.compile(filterPattern).matcher(String.valueOf(matchValue)).matches();
		boolean matchesValue = filterValue == null || filterValue.equals(matchValue);
		
		return (event.topic.equalsIgnoreCase(filterTopic) && event.key.equalsIgnoreCase(filterKey) && matchesPattern && matchesValue);
	}

	public static TriggerDescriptor buildDescriptor(String topic, String key, String selector, String pattern, Object value) {
		TriggerDescriptor trigger = new TriggerDescriptor();
		trigger.setType(TYPE);
		trigger.getConfig().put("topic", topic);
		trigger.getConfig().put("key", key);
		if (selector != null) trigger.getConfig().put("selector", selector);
		if (pattern != null) trigger.getConfig().put("pattern", pattern);
		if (value != null) trigger.getConfig().put("value", value);
		return trigger;
	}
}
