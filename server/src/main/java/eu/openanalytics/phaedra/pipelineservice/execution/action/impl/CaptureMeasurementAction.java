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
	private static final String DC_KEY_MEAS_CAPTURED = "measurementCaptured";
	private static final String DC_KEY_MEAS_CAPTURE_ERR = "measurementCaptureError";
	
	private static final String JSON_LOCATION_SELECTOR = "$['location']";

	@Override
	protected EventDescriptor buildActionStartMessage(PipelineExecutionContext context) {
		String triggerMessage = getTriggerMessage(context);
		
		String location = JsonPath.read(triggerMessage, JSON_LOCATION_SELECTOR);
		long measId = (long)(Math.random() * 100000);
		
		String[] locationParts = location.split("/");
		String barcode = locationParts[locationParts.length - 1];

		String msgToPost = String.format("{ 'location': '%s', 'measurementId': %d, 'barcode': '%s' }", location, measId, barcode);
		
		boolean mockError = Math.random() < 0.1;
		return mockError ? 
				EventDescriptor.of(DC_TOPIC, DC_KEY_MEAS_CAPTURE_ERR, msgToPost)
				:
				EventDescriptor.of(DC_TOPIC, DC_KEY_MEAS_CAPTURED, msgToPost);
	}

	@Override
	public TriggerDescriptor getActionCompleteTrigger(PipelineExecutionContext context) {
		String triggerMessage = getTriggerMessage(context);
		String location = JsonPath.read(triggerMessage, JSON_LOCATION_SELECTOR);
		return GenericEventTrigger.buildDescriptor(
				DC_TOPIC, DC_KEY_MEAS_CAPTURED, DC_KEY_MEAS_CAPTURE_ERR, 
				JSON_LOCATION_SELECTOR, null, location);
	}
}
