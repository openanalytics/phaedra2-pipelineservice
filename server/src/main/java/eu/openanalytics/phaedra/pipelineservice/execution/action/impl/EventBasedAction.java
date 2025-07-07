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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import eu.openanalytics.phaedra.pipelineservice.execution.PipelineExecutionContext;
import eu.openanalytics.phaedra.pipelineservice.execution.action.AbstractAction;
import eu.openanalytics.phaedra.pipelineservice.execution.action.ActionExecutionException;
import eu.openanalytics.phaedra.pipelineservice.execution.event.EventDescriptor;

public abstract class EventBasedAction extends AbstractAction {

	protected final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired
	protected KafkaTemplate<String, String> kafkaTemplate;
	
	@Override
	public String getType() {
		String className = getClass().getSimpleName();
		return className.substring(0, className.indexOf("Action"));
	}
	
	@Override
	public void invoke(PipelineExecutionContext context) throws ActionExecutionException {
		EventDescriptor msgToPost = buildActionStartMessage(context);
		kafkaTemplate.send(msgToPost.topic, msgToPost.key, msgToPost.message);
	}
	
	@Override
	public void onActionComplete(PipelineExecutionContext context) {
		// Default: nothing to do.
	}
	
	protected abstract EventDescriptor buildActionStartMessage(PipelineExecutionContext context);
	
	// Helper methods
	// ---------------------------------
	
	/**
	 * Get the message of the event that triggered the current step.
	 */
	protected String getTriggerMessage(PipelineExecutionContext context) {
		return context.resolveVar("currentStep.trigger.message", null);
	}

	/**
	 * Get the message of the event that marked the current step complete.
	 * This is in fact the trigger message of the next step.
	 */
	protected String getNextTriggerMessage(PipelineExecutionContext context) {
		int nextStepNr = context.execution.getCurrentStep() + 1;
		return context.resolveVar(String.format("step.%d.trigger.message", nextStepNr), null);
	}

}
