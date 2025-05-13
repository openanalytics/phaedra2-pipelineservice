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

import com.jayway.jsonpath.JsonPath;

import eu.openanalytics.phaedra.pipelineservice.execution.PipelineExecutionContext;
import eu.openanalytics.phaedra.pipelineservice.execution.event.EventDescriptor;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.TriggerDescriptor;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.impl.GenericEventTrigger;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.impl.GenericEventTrigger.EventMatchCondition;

@Component
public class CaptureMeasurementAction extends EventBasedAction {

	private static final String DC_TOPIC = "datacapture";
	
	private static final String DC_KEY_REQUEST_CAPTURE_JOB = "requestCaptureJob";
	private static final String DC_KEY_NOTIFY_CAPTURE_JOB_UPDATED = "notifyCaptureJobUpdated";
	
	private static final String JSON_SOURCE_PATH_SELECTOR = "$.sourcePath";
	private static final String JSON_MEAS_ID_SELECTOR = "$.measurementId";
	private static final String JSON_BARCODE_SELECTOR = "$.barcode";
	private static final String JSON_STATUS_SELECTOR = "$.statusCode";

	@Override
	protected EventDescriptor buildActionStartMessage(PipelineExecutionContext context) {
		String triggerMessage = getTriggerMessage(context);
		String sourcePath = JsonPath.read(triggerMessage, JSON_SOURCE_PATH_SELECTOR);
		context.setVar("sourcePath", sourcePath);

		String captureConfigId = getRequiredVar("currentStep.action.config.captureConfigId", context, null);
		
		String msgToPost = String.format("{ \"sourcePath\": \"%s\", \"captureConfigId\": \"%s\" }", sourcePath, captureConfigId);
		return EventDescriptor.of(DC_TOPIC, DC_KEY_REQUEST_CAPTURE_JOB, msgToPost);
	}

	@Override
	public TriggerDescriptor getActionCompleteTrigger(PipelineExecutionContext context) {
		String sourcePath = context.resolveVar("sourcePath", null);
		return buildActionCompleteTrigger(sourcePath, null);
	}
	
	@Override
	public void onActionComplete(PipelineExecutionContext context) {
		String triggerMessage = getNextTriggerMessage(context);
		captureMeasVariables(triggerMessage, context);
	}
	
	public static TriggerDescriptor buildActionCompleteTrigger(String sourcePath, String sourcePathPattern) {
		EventMatchCondition matchesSourcePath = new EventMatchCondition(JSON_SOURCE_PATH_SELECTOR, sourcePathPattern, sourcePath); 
		EventMatchCondition hasMeasId = new EventMatchCondition(JSON_MEAS_ID_SELECTOR, "\\d+", null);
		EventMatchCondition isRunning = new EventMatchCondition(JSON_STATUS_SELECTOR, null, "Running");
		EventMatchCondition isError = new EventMatchCondition(JSON_STATUS_SELECTOR, null, "Error");
		
		return GenericEventTrigger.buildTrigger(DC_TOPIC, DC_KEY_NOTIFY_CAPTURE_JOB_UPDATED,
				Arrays.asList(matchesSourcePath, hasMeasId, isRunning),
				Arrays.asList(matchesSourcePath, isError));
	}
	
	public static void captureMeasVariables(String triggerMessage, PipelineExecutionContext context) {
		Number measId = JsonPath.read(triggerMessage, JSON_MEAS_ID_SELECTOR);
		String barcode = JsonPath.read(triggerMessage, JSON_BARCODE_SELECTOR);
		context.setVar("measurementId", measId);
		context.setVar("barcode", barcode);	
	}
}
