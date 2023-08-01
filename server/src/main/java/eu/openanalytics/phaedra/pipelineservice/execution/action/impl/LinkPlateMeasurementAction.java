package eu.openanalytics.phaedra.pipelineservice.execution.action.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.openanalytics.phaedra.pipelineservice.execution.PipelineExecutionContext;
import eu.openanalytics.phaedra.pipelineservice.execution.event.EventDescriptor;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.TriggerDescriptor;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.impl.GenericEventTrigger;
import eu.openanalytics.phaedra.plateservice.client.PlateServiceClient;
import eu.openanalytics.phaedra.plateservice.dto.PlateDTO;

@Component
public class LinkPlateMeasurementAction extends EventBasedAction {

	private static final String TOPIC = "plate";
	private static final String EVENT_REQ_PLATE_MEAS_LINK = "requestPlateMeasurementLink";
	private static final String EVENT_MEAS_LINKED = "measurementLinked";
	
	private static final String JSON_MEAS_ID_SELECTOR = "$.measurementId";
	
	@Autowired
	private PlateServiceClient plateServiceClient;
	
	@Override
	protected EventDescriptor buildActionStartMessage(PipelineExecutionContext context) {
		String barcode = getRequiredVar("barcode", context, null);
		Long measId = getRequiredVar("measurementId", context, null);
		Long projectId = getRequiredVar("currentStep.action.config.projectId", context, null);
		
		List<Long> experimentIds = plateServiceClient.getExperiments(projectId).stream().map(exp -> exp.getId()).toList();
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
		Long measId = context.resolveVar("measurementId", null);
		return GenericEventTrigger.buildDescriptor(TOPIC, EVENT_MEAS_LINKED, null, JSON_MEAS_ID_SELECTOR, null, measId);
	}

}
