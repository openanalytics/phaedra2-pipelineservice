package eu.openanalytics.phaedra.pipelineservice.execution.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import eu.openanalytics.phaedra.pipelineservice.execution.PipelineExecutionContext;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.TriggerDescriptor;

public abstract class AbstractAction implements IAction {

	protected final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired
	protected KafkaTemplate<String, String> kafkaTemplate;

	/**
	 * By providing an implicit "action complete" trigger, the user does not have
	 * to define an explicit trigger in the next step of their pipeline.
	 * Instead, this "action complete" trigger will be registered automatically
	 * into the next step of the pipeline.
	 */
	@Override
	public TriggerDescriptor getActionCompleteTrigger(PipelineExecutionContext context) {
		// Default: no completion trigger, so the step completes immediately after invoking the action.
		return null;
	}
	
	protected void sleep(long ms) {
		logger.debug(String.format("Sleeping %d ms.", ms));
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {}
	}
	
	/**
	 * Get the message of the event that triggered the current step.
	 */
	protected String getTriggerMessage(PipelineExecutionContext context) {
		int stepNr = context.execution.getCurrentStep();
		String triggerMessage = context.resolveVar(String.format("step.%d.trigger.message", stepNr), null);
		return triggerMessage;
	}
}
