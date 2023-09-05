package eu.openanalytics.phaedra.pipelineservice.execution.action.impl;

import org.springframework.stereotype.Component;

import com.jayway.jsonpath.JsonPath;

import eu.openanalytics.phaedra.pipelineservice.execution.PipelineExecutionContext;
import eu.openanalytics.phaedra.pipelineservice.execution.event.EventDescriptor;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.TriggerDescriptor;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.impl.GenericEventTrigger;

@Component
public class CaptureMeasurementAction extends EventBasedAction {

	private static final String DC_TOPIC = "datacapture";
	
	private static final String DC_KEY_SUBMIT_CAPTURE_JOB = "submitCaptureJob";
	private static final String DC_KEY_MEAS_CAPTURED = "measurementCaptured";
	private static final String DC_KEY_MEAS_CAPTURE_ERR = "measurementCaptureError";
	
	private static final String JSON_LOCATION_SELECTOR = "$.location";
	private static final String JSON_MEAS_ID_SELECTOR = "$.measurementId";
	private static final String JSON_BARCODE_SELECTOR = "$.barcode";

	@Override
	protected EventDescriptor buildActionStartMessage(PipelineExecutionContext context) {
		String triggerMessage = getTriggerMessage(context);
		String location = JsonPath.read(triggerMessage, JSON_LOCATION_SELECTOR);
		context.setVar("location", location);

		String captureConfigId = getRequiredVar("currentStep.trigger.config.captureConfigId", context, null);
		
		String msgToPost = String.format("{ 'location': '%s', 'captureConfigId': '%s' }", location, captureConfigId);
		return EventDescriptor.of(DC_TOPIC, DC_KEY_SUBMIT_CAPTURE_JOB, msgToPost);
	}

	@Override
	public TriggerDescriptor getActionCompleteTrigger(PipelineExecutionContext context) {
		String location = context.resolveVar("location", null);
		return GenericEventTrigger.buildDescriptor(
				DC_TOPIC, DC_KEY_MEAS_CAPTURED, DC_KEY_MEAS_CAPTURE_ERR, JSON_LOCATION_SELECTOR, null, location);
	}
	
	@Override
	public void onActionComplete(PipelineExecutionContext context) {
		String triggerMessage = getNextTriggerMessage(context);
		Number measId = JsonPath.read(triggerMessage, JSON_MEAS_ID_SELECTOR);
		String barcode = JsonPath.read(triggerMessage, JSON_BARCODE_SELECTOR);
		context.setVar("measurementId", measId);
		context.setVar("barcode", barcode);
	}
}
