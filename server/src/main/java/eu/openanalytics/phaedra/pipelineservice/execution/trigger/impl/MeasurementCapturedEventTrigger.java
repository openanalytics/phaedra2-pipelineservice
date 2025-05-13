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

import org.springframework.stereotype.Component;

import eu.openanalytics.phaedra.pipelineservice.execution.PipelineExecutionContext;
import eu.openanalytics.phaedra.pipelineservice.execution.action.impl.CaptureMeasurementAction;
import eu.openanalytics.phaedra.pipelineservice.execution.event.EventDescriptor;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.TriggerDescriptor;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.TriggerMatchType;

@Component
public class MeasurementCapturedEventTrigger extends GenericEventTrigger {

	private static final String TYPE = "MeasurementCaptured";
	
	@Override
	public String getType() {
		return TYPE;
	}
	
	@Override
	public TriggerMatchType matches(EventDescriptor event, TriggerDescriptor descriptor, PipelineExecutionContext ctx) {
		String pattern = (String) descriptor.getConfig().get("pattern");
		TriggerMatchType match = super.matches(event, CaptureMeasurementAction.buildActionCompleteTrigger(null, pattern), ctx);
		
		if (match == TriggerMatchType.Match) {
			CaptureMeasurementAction.captureMeasVariables(event.message, ctx);
		}
		
		return match;
	}
}
