package eu.openanalytics.phaedra.pipelineservice.execution.action.impl;

import org.springframework.stereotype.Component;

import com.jayway.jsonpath.JsonPath;

import eu.openanalytics.phaedra.pipelineservice.execution.PipelineExecutionContext;
import eu.openanalytics.phaedra.pipelineservice.execution.action.AbstractAction;
import eu.openanalytics.phaedra.pipelineservice.execution.action.ActionExecutionException;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.TriggerDescriptor;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.impl.GenericEventTrigger;

@Component
public class CaptureMeasurementAction extends AbstractAction {

	private static final String DC_TOPIC = "datacapture";
	private static final String DC_KEY_MEAS_CAPTURED = "measurementCaptured";
	
	private static final String JSON_LOCATION_SELECTOR = "$['location']";
	
	@Override
	public String getType() {
		return "CaptureMeasurement";
	}

	@Override
	public void invoke(PipelineExecutionContext context) throws ActionExecutionException {
		//TODO Submit a real datacapture request
		sleep(3000);
		
		String triggerMessage = getTriggerMessage(context);
		
		String location = JsonPath.read(triggerMessage, JSON_LOCATION_SELECTOR);
		long measId = (long)(Math.random() * 100000);
		
		String[] locationParts = location.split("/");
		String barcode = locationParts[locationParts.length - 1];
		
		String msgToPost = String.format("{ 'location': '%s', 'measurementId': %d, 'barcode': '%s' }", location, measId, barcode);
		kafkaTemplate.send(DC_TOPIC, DC_KEY_MEAS_CAPTURED, msgToPost);
	}

	@Override
	public TriggerDescriptor getActionCompleteTrigger(PipelineExecutionContext context) {
		String triggerMessage = getTriggerMessage(context);
		String location = JsonPath.read(triggerMessage, JSON_LOCATION_SELECTOR);
		return GenericEventTrigger.buildDescriptor(DC_TOPIC, DC_KEY_MEAS_CAPTURED, JSON_LOCATION_SELECTOR, null, location);
	}
}
