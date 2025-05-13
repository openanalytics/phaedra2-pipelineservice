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
package eu.openanalytics.phaedra.pipelineservice.execution.action.impl;

import java.util.Arrays;

import org.springframework.stereotype.Component;

import eu.openanalytics.phaedra.pipelineservice.execution.PipelineExecutionContext;
import eu.openanalytics.phaedra.pipelineservice.execution.event.EventDescriptor;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.TriggerDescriptor;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.impl.GenericEventTrigger;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.impl.GenericEventTrigger.EventMatchCondition;

@Component
public class CalculateProtocolAction extends EventBasedAction {

	private static final String TOPIC = "calculations";
	
	private static final String EVENT_REQ_PLATE_CALCULATION = "requestPlateCalculation";
	private static final String EVENT_NOTIFY_CALCULATION_EVENT = "notifyCalculationEvent";
	
	private static final String JSON_PLATE_ID_SELECTOR = "$.plateId";
	private static final String JSON_CALC_STATUS_SELECTOR = "$.calculationStatus";
	
	@Override
	protected EventDescriptor buildActionStartMessage(PipelineExecutionContext context) {
		Number plateId = getRequiredVar("plateId", context, null);
		Number measId = getRequiredVar("measurementId", context, null);
		Number protocolId = getRequiredVar("currentStep.action.config.id", context, null);
		
		String msgToPost = String.format("{ \"plateIds\": [%d], \"measIds\": { \"%d\": %d }, \"protocolId\": %d }", plateId, plateId, measId, protocolId);
		return EventDescriptor.of(TOPIC, EVENT_REQ_PLATE_CALCULATION, msgToPost);
	}
	
	@Override
	public TriggerDescriptor getActionCompleteTrigger(PipelineExecutionContext context) {
		Number plateId = context.resolveVar("plateId", null);

		EventMatchCondition matchesPlateId = new EventMatchCondition(JSON_PLATE_ID_SELECTOR, null, plateId);
		EventMatchCondition isOK = new EventMatchCondition(JSON_CALC_STATUS_SELECTOR, null, "CALCULATION_OK");
		EventMatchCondition isError = new EventMatchCondition(JSON_CALC_STATUS_SELECTOR, null, "CALCULATION_ERROR");
		
		return GenericEventTrigger.buildTrigger(TOPIC, EVENT_NOTIFY_CALCULATION_EVENT,
				Arrays.asList(matchesPlateId, isOK),
				Arrays.asList(matchesPlateId, isError));
	}

}
