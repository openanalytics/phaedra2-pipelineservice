package eu.openanalytics.phaedra.pipelineservice.execution.action.impl;

import org.springframework.stereotype.Component;

import com.jayway.jsonpath.JsonPath;

import eu.openanalytics.phaedra.pipelineservice.execution.PipelineExecutionContext;
import eu.openanalytics.phaedra.pipelineservice.execution.event.EventDescriptor;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.TriggerDescriptor;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.impl.GenericEventTrigger;

@Component
public class LinkPlateMeasurementAction extends EventBasedAction {

	private static final String DC_TOPIC = "plate";
	private static final String DC_KEY_MEAS_LINKED = "measurementLinked";
	
	private static final String JSON_MEAS_ID_SELECTOR = "$['measurementId']";
	private static final String JSON_BARCODE_SELECTOR = "$['barcode']";

	@Override
	protected EventDescriptor buildActionStartMessage(PipelineExecutionContext context) {
		String triggerMessage = getTriggerMessage(context);
		
		Number measId = JsonPath.read(triggerMessage, JSON_MEAS_ID_SELECTOR);
		String barcode = JsonPath.read(triggerMessage, JSON_BARCODE_SELECTOR);
		long plateId = (long)(Math.random() * 100000);
		
		String msgToPost = String.format("{ 'measurementId': %d, 'barcode': '%s', 'plateId': %d }", measId, barcode, plateId);
		return EventDescriptor.of(DC_TOPIC, DC_KEY_MEAS_LINKED, msgToPost);
	}
	
	@Override
	public TriggerDescriptor getActionCompleteTrigger(PipelineExecutionContext context) {
		String triggerMessage = getTriggerMessage(context);
		Object measId = JsonPath.read(triggerMessage, JSON_MEAS_ID_SELECTOR);
		return GenericEventTrigger.buildDescriptor(DC_TOPIC, DC_KEY_MEAS_LINKED, null, JSON_MEAS_ID_SELECTOR, null, measId);
	}

}
