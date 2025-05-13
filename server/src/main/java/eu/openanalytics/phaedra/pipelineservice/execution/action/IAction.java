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
package eu.openanalytics.phaedra.pipelineservice.execution.action;

import eu.openanalytics.phaedra.pipelineservice.execution.PipelineExecutionContext;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.TriggerDescriptor;

public interface IAction {

	public String getType();
	
	public void invoke(PipelineExecutionContext context) throws ActionExecutionException;
	
	/**
	 * By providing an implicit "action complete" trigger, the user does not have
	 * to define an explicit trigger in the next step of their pipeline.
	 * Instead, this "action complete" trigger will be registered automatically
	 * into the next step of the pipeline.
	 */
	public TriggerDescriptor getActionCompleteTrigger(PipelineExecutionContext context);

	/**
	 * This method is invoked when the action has been completed.
	 */
	public void onActionComplete(PipelineExecutionContext context);
	
}
