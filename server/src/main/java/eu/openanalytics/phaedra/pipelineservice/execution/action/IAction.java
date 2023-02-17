package eu.openanalytics.phaedra.pipelineservice.execution.action;

import eu.openanalytics.phaedra.pipelineservice.execution.PipelineExecutionContext;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.TriggerDescriptor;

public interface IAction {

	public String getType();
	
	public void invoke(PipelineExecutionContext context) throws ActionExecutionException;

	public TriggerDescriptor getActionCompleteTrigger(PipelineExecutionContext context);

}
