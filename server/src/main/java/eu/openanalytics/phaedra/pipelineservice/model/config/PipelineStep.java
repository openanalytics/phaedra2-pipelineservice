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
