package eu.openanalytics.phaedra.pipelineservice.execution.action.impl;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.openanalytics.phaedra.measservice.dto.MeasurementDTO;
import eu.openanalytics.phaedra.measurementservice.client.MeasurementServiceClient;
import eu.openanalytics.phaedra.metadataservice.client.MetadataServiceClient;
import eu.openanalytics.phaedra.metadataservice.dto.PropertyDTO;
import eu.openanalytics.phaedra.pipelineservice.execution.PipelineExecutionContext;
import eu.openanalytics.phaedra.pipelineservice.execution.event.EventDescriptor;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.TriggerDescriptor;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.impl.GenericEventTrigger;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.impl.GenericEventTrigger.EventMatchCondition;
import eu.openanalytics.phaedra.plateservice.client.PlateServiceClient;
import eu.openanalytics.phaedra.plateservice.dto.ExperimentDTO;
import eu.openanalytics.phaedra.plateservice.dto.PlateDTO;

@Component
public class LinkPlateMeasurementAction extends EventBasedAction {

	private static final String TOPIC = "plates";
	
	private static final String EVENT_REQ_PLATE_MEAS_LINK = "requestPlateMeasurementLink";
	private static final String EVENT_NOTIFY_PLATE_MEAS_LINKED = "notifyPlateMeasLinked";
	
	private static final String JSON_MEAS_ID_SELECTOR = "$.measurement.measurementId";
	private static final String JSON_OUTCOME_SELECTOR = "$.outcome";
	
	private static final String VAR_BARCODE = "barcode";
	private static final String VAR_MEAS_ID = "measurementId";
	private static final String VAR_PROJECT_ID = "projectId";
	private static final String VAR_PLATE_ID = "plateId";
	private static final String VAR_EXP_NAME_PATTERN = "experimentNamePattern";
	private static final String VAR_EXP_NAME_PATTERN_GROUP = "experimentNamePatternGroup";
	private static final String VAR_CREATE_EXPERIMENT = "createExperiment";
	private static final String VAR_CREATE_PLATE = "createPlate";
	
	@Autowired
	private PlateServiceClient plateServiceClient;

	@Autowired
	private MeasurementServiceClient measurementServiceClient;
	
	@Autowired
	private MetadataServiceClient metadataServiceClient;
	
	@Override
	protected EventDescriptor buildActionStartMessage(PipelineExecutionContext context) {
		String barcode = getRequiredVar(VAR_BARCODE, context, null);
		Number measId = getRequiredVar(VAR_MEAS_ID, context, null);
		Number projectId = getRequiredVar(VAR_PROJECT_ID, context, null);
		String experimentNamePattern = getRequiredVar(VAR_EXP_NAME_PATTERN, context, null);
		
		MeasurementDTO measurement = measurementServiceClient.getMeasurementByMeasId(measId.longValue());
		if (measurement == null) {
			throw new RuntimeException(String.format("Measurement with ID %d not found", measId));
		}
		
		// Use "sourcePath" property if available, or else the barcode
		List<PropertyDTO> properties = metadataServiceClient.getProperties("MEASUREMENT", measId.longValue());
		String sourcePath = properties.stream().filter(prop -> prop.getPropertyName().equalsIgnoreCase("sourcepath")).map(prop -> prop.getPropertyValue()).findAny().orElse(barcode);
		
		int experimentNameRegexGroup = Integer.valueOf(context.resolveVar(VAR_EXP_NAME_PATTERN_GROUP, "1"));
		Matcher sourcePathMatcher = Pattern.compile(experimentNamePattern).matcher(sourcePath);
		String experimentName = sourcePathMatcher.matches() ? sourcePathMatcher.group(experimentNameRegexGroup) : null;
		if (experimentName == null) {
			throw new RuntimeException(String.format("Failed to resolve experiment name in '%s' using pattern '%s'", sourcePath, experimentNamePattern));
		}
		
		ExperimentDTO experiment = plateServiceClient.getExperiments(projectId.longValue()).stream().filter(exp -> exp.getName().equals(experimentName)).findAny().orElse(null);
		if (experiment == null) {
			boolean createExperiment = Boolean.valueOf(context.resolveVar(VAR_CREATE_EXPERIMENT, "false"));
			if (createExperiment) {
				experiment = plateServiceClient.createExperiment(experimentName, projectId.longValue());
			} else {
				throw new RuntimeException(String.format("Experiment with name '%s' does not exist in project %d. (Hint: to enable experiment auto-creation, use \"%s\": true)",
						experimentName, projectId, VAR_CREATE_EXPERIMENT));
			}
		}
		
		PlateDTO plate = plateServiceClient.getPlatesByExperiment(experiment.getId()).stream().filter(pl -> pl.getBarcode().equals(barcode)).findAny().orElse(null);
		if (plate == null) {
			boolean createPlate = Boolean.valueOf(context.resolveVar(VAR_CREATE_PLATE, "false"));
			if (createPlate) {
				plate = plateServiceClient.createPlate(barcode, experiment.getId(), measurement.getRows(), measurement.getColumns());
			} else {
				throw new RuntimeException(String.format("Plate with barcode '%s' does not exist in experiment '%s'. (Hint: to enable plate auto-creation, use \"%s\": true)",
						barcode, experimentName, VAR_CREATE_PLATE));
			}
		}
		context.setVar(VAR_PLATE_ID, plate.getId());

		String msgToPost = String.format("{ \"%s\": %d, \"%s\": %d }", VAR_MEAS_ID, measId, VAR_PLATE_ID, plate.getId());
		return EventDescriptor.of(TOPIC, EVENT_REQ_PLATE_MEAS_LINK, msgToPost);
	}
	
	@Override
	public TriggerDescriptor getActionCompleteTrigger(PipelineExecutionContext context) {
		Number measId = context.resolveVar(VAR_MEAS_ID, null);
		
		EventMatchCondition matchesMeasId = new EventMatchCondition(JSON_MEAS_ID_SELECTOR, null, measId); 
		EventMatchCondition isOK = new EventMatchCondition(JSON_OUTCOME_SELECTOR, null, "OK");
		EventMatchCondition isError = new EventMatchCondition(JSON_OUTCOME_SELECTOR, null, "ERROR");
		
		return GenericEventTrigger.buildTrigger(TOPIC, EVENT_NOTIFY_PLATE_MEAS_LINKED,
				Arrays.asList(matchesMeasId, isOK),
				Arrays.asList(matchesMeasId, isError));
	}

}
