package eu.openanalytics.phaedra.pipelineservice.execution.action.impl;

import java.util.Arrays;

import org.springframework.stereotype.Component;

import com.jayway.jsonpath.JsonPath;

import eu.openanalytics.phaedra.pipelineservice.execution.PipelineExecutionContext;
import eu.openanalytics.phaedra.pipelineservice.execution.event.EventDescriptor;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.TriggerDescriptor;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.impl.GenericEventTrigger;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.impl.GenericEventTrigger.EventMatchCondition;

@Component
public class CaptureMeasurementAction extends EventBasedAction {

	private static final String DC_TOPIC = "datacapture";
	
	private static final String DC_KEY_REQUEST_CAPTURE_JOB = "requestCaptureJob";
	private static final String DC_KEY_NOTIFY_CAPTURE_JOB_UPDATED = "notifyCaptureJobUpdated";
	
	private static final String JSON_SOURCE_PATH_SELECTOR = "$.sourcePath";
	private static final String JSON_MEAS_ID_SELECTOR = "$.measurementId";
	private static final String JSON_BARCODE_SELECTOR = "$.barcode";
	private static final String JSON_STATUS_SELECTOR = "$.statusCode";

	@Override
	protected EventDescriptor buildActionStartMessage(PipelineExecutionContext context) {
		String triggerMessage = getTriggerMessage(context);
		String sourcePath = JsonPath.read(triggerMessage, JSON_SOURCE_PATH_SELECTOR);
		context.setVar("sourcePath", sourcePath);

		String captureConfigId = getRequiredVar("currentStep.action.config.captureConfigId", context, null);
		
		String msgToPost = String.format("{ 'sourcePath': '%s', 'captureConfigId': '%s' }", sourcePath, captureConfigId);
		return EventDescriptor.of(DC_TOPIC, DC_KEY_REQUEST_CAPTURE_JOB, msgToPost);
	}

	@Override
	public TriggerDescriptor getActionCompleteTrigger(PipelineExecutionContext context) {
		String sourcePath = context.resolveVar("sourcePath", null);
		EventMatchCondition matchSourcePath = EventMatchCondition.builder().key(DC_KEY_NOTIFY_CAPTURE_JOB_UPDATED)
				.payloadSelector(JSON_SOURCE_PATH_SELECTOR).value(sourcePath).build(); 
				
		EventMatchCondition isError = EventMatchCondition.builder().key(DC_KEY_NOTIFY_CAPTURE_JOB_UPDATED)
				.payloadSelector(JSON_STATUS_SELECTOR).value("Error").build();
		
		return GenericEventTrigger.buildTrigger(DC_TOPIC, Arrays.asList(isError), Arrays.asList(matchSourcePath));
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
