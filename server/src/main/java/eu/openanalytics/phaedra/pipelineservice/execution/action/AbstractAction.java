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

	protected void sleep(long ms) {
		logger.debug(String.format("Sleeping %d ms.", ms));
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {}
	}
	
	@Override
	public TriggerDescriptor getActionCompleteTrigger(PipelineExecutionContext context) {
		// Default: no completion trigger, so the step completes immediately after invoking the action.
		return null;
	}
}
