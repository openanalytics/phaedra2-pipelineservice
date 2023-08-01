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
