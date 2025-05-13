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
package eu.openanalytics.phaedra.pipelineservice.model.config;

import eu.openanalytics.phaedra.pipelineservice.execution.action.ActionDescriptor;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.TriggerDescriptor;

public class PipelineStep {

	private TriggerDescriptor trigger;
	private ActionDescriptor action;
	
	public TriggerDescriptor getTrigger() {
		return trigger;
	}
	public void setTrigger(TriggerDescriptor trigger) {
		this.trigger = trigger;
	}
	public ActionDescriptor getAction() {
		return action;
	}
	public void setAction(ActionDescriptor action) {
		this.action = action;
	}

}
