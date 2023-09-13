package eu.openanalytics.phaedra.pipelineservice.execution.action.impl;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.openanalytics.phaedra.pipelineservice.execution.PipelineExecutionContext;
import eu.openanalytics.phaedra.pipelineservice.execution.event.EventDescriptor;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.TriggerDescriptor;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.impl.GenericEventTrigger;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.impl.GenericEventTrigger.EventMatchCondition;
import eu.openanalytics.phaedra.plateservice.client.PlateServiceClient;
import eu.openanalytics.phaedra.plateservice.dto.PlateDTO;

@Component
public class LinkPlateMeasurementAction extends EventBasedAction {

	private static final String TOPIC = "plates";
	
	private static final String EVENT_REQ_PLATE_MEAS_LINK = "requestPlateMeasurementLink";
	private static final String EVENT_NOTIFY_PLATE_MEAS_LINKED = "notifyPlateMeasLinked";
	
	private static final String JSON_MEAS_ID_SELECTOR = "$.measurement.measurementId";
	private static final String JSON_OUTCOME_SELECTOR = "$.outcome";
	
	@Autowired
	private PlateServiceClient plateServiceClient;
	
	@Override
	protected EventDescriptor buildActionStartMessage(PipelineExecutionContext context) {
		String barcode = getRequiredVar("barcode", context, null);
		Number measId = getRequiredVar("measurementId", context, null);
		Number projectId = getRequiredVar("currentStep.action.config.projectId", context, null);
		
		List<Long> experimentIds = plateServiceClient.getExperiments(projectId.longValue()).stream().map(exp -> exp.getId()).toList();
		List<PlateDTO> matchingPlates = plateServiceClient.getPlatesByBarcode(barcode).stream()
				.filter(plate -> experimentIds.contains(plate.getExperimentId())).toList();
		
		if (matchingPlates.isEmpty()) {
			throw new RuntimeException(
				String.format("Cannot link plate measurement: no plate found in project %d with barcode matching '%s'", projectId, barcode));
		} else if (matchingPlates.size() > 1) {
			throw new RuntimeException(
				String.format("Cannot link plate measurement: multiple plates found in project %d with barcode matching '%s'", projectId, barcode));
		}
		
		long plateId = matchingPlates.get(0).getId();
		context.setVar("plateId", plateId);
		
		String msgToPost = String.format("{ 'measurementId': %d, 'plateId': %d }", measId, plateId);
		return EventDescriptor.of(TOPIC, EVENT_REQ_PLATE_MEAS_LINK, msgToPost);
	}
	
	@Override
	public TriggerDescriptor getActionCompleteTrigger(PipelineExecutionContext context) {
		Number measId = context.resolveVar("measurementId", null);
		
		EventMatchCondition matchesMeasId = new EventMatchCondition(JSON_MEAS_ID_SELECTOR, null, measId); 
		EventMatchCondition isOK = new EventMatchCondition(JSON_OUTCOME_SELECTOR, null, "OK");
		EventMatchCondition isError = new EventMatchCondition(JSON_OUTCOME_SELECTOR, null, "ERROR");
		
		return GenericEventTrigger.buildTrigger(TOPIC, EVENT_NOTIFY_PLATE_MEAS_LINKED,
				Arrays.asList(matchesMeasId, isOK),
				Arrays.asList(matchesMeasId, isError));
	}

}
