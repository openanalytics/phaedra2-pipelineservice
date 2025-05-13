/**
 * Phaedra II
 *
 * Copyright (C) 2016-2025 Open Analytics
 *
 * ===========================================================================
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License as published by
 * The Apache Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License for more details.
 *
 * You should have received a copy of the Apache License
 * along with this program.  If not, see <http://www.apache.org/licenses/>
 */
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
 * The topic and key are matched using String equality.
 * The body is matched using a list of conditions.
 */
@Component
public class GenericEventTrigger implements ITrigger {

	private static final String TYPE = "Event";
	
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
		if (errorConditions != null) {
			boolean errorConditionsMet = errorConditions.stream().allMatch(c -> matches(c, event));
			if (errorConditionsMet) return TriggerMatchType.Error;
		}
		
		List<EventMatchCondition> matchConditions = (List<EventMatchCondition>) descriptor.getConfig().get("matchConditions");
		if (matchConditions != null) {
			boolean matchConditionsMet = matchConditions.stream().allMatch(c -> matches(c, event));
			if (matchConditionsMet) return TriggerMatchType.Match;
		}
		
		String selector = (String) descriptor.getConfig().get("selector");
		String pattern = (String) descriptor.getConfig().get("pattern");
		String value = (String) descriptor.getConfig().get("value");
		if (pattern != null || value != null) {
			EventMatchCondition simpleCondition = new EventMatchCondition(selector, pattern, value);
			if (matches(simpleCondition, event)) return TriggerMatchType.Match;
		}
		
		return TriggerMatchType.NoMatch;
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
		
		logger.debug(String.format("Checking event condition (pattern '%s', value '%s') against payload '%s'",
				condition.getValuePattern(), condition.getValue(), payload));
		
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
