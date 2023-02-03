package eu.openanalytics.phaedra.pipelineservice.execution.action.impl;

import org.springframework.stereotype.Component;

import eu.openanalytics.phaedra.pipelineservice.execution.PipelineExecutionContext;
import eu.openanalytics.phaedra.pipelineservice.execution.action.AbstractAction;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.TriggerDescriptor;
import eu.openanalytics.phaedra.pipelineservice.model.config.PipelineStep;
import eu.openanalytics.phaedra.pipelineservice.service.PipelineTriggerService;

@Component
public class CaptureMeasurementAction extends AbstractAction {

	private static final String DC_TOPIC = "datacapture";
	private static final String DC_KEY_MEAS_CAPTURED = "measurementCaptured";
	
	@Override
	public String getType() {
		return "CaptureMeasurement";
	}

	@Override
	public void invoke(PipelineExecutionContext context) {
		logger.debug(String.format("Invoking %s [exec %d][pipeline %d]", getType(), context.execution.getId(), context.definition.getId()));
		
		//TODO Submit a real datacapture request
		sleep(3000);
		kafkaTemplate.send(DC_TOPIC, DC_KEY_MEAS_CAPTURED, "{ \"location\": \"s3://test/location\" }");
	}

	@Override
	public TriggerDescriptor getActionCompleteTrigger(PipelineExecutionContext context) {
		TriggerDescriptor trigger = new TriggerDescriptor();
		trigger.setType(PipelineTriggerService.TRIGGER_TYPE_EVENT_LISTENER);
		trigger.getConfig().put("topic", DC_TOPIC);
		trigger.getConfig().put("key", DC_KEY_MEAS_CAPTURED);
		
		// Look up the pattern from the initial step trigger (assumed to be "newMeasurementAvailable")
		PipelineStep initialStep = context.config.getSteps().get(0);
		String pattern = (String) initialStep.getTrigger().getConfig().get("pattern");
		trigger.getConfig().put("pattern", pattern);
		
		return trigger;
	}
}
