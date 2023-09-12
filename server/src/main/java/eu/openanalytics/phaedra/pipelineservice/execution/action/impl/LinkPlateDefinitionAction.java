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
import eu.openanalytics.phaedra.plateservice.dto.PlateTemplateDTO;

@Component
public class LinkPlateDefinitionAction extends EventBasedAction {

	private static final String TOPIC = "plates";

	private static final String EVENT_REQ_PLATE_DEF_LINK = "requestPlateDefinitionLink";
	private static final String EVENT_NOTIFY_PLATE_DEF_LINKED = "notifyPlateDefinitionLinked";
	
	private static final String JSON_PLATE_ID_SELECTOR = "$.plateId";
	private static final String JSON_OUTCOME_SELECTOR = "$.outcome";

	@Autowired
	private PlateServiceClient plateServiceClient;
	
	@Override
	protected EventDescriptor buildActionStartMessage(PipelineExecutionContext context) {
		Long plateId = getRequiredVar("plateId", context, null);
		String source = context.resolveVar("currentStep.action.config.source", "template");
		if (!source.toLowerCase().equals("template")) {
			//TODO Support other link sources
			throw new IllegalArgumentException(String.format("Unsupported link source: %s", source));
		}
		
		String templateName = context.resolveVar("currentStep.action.config.name", "${barcode}");
		templateName = context.resolveVars(templateName);
		
		List<PlateTemplateDTO> templates = plateServiceClient.getPlateTemplatesByName(templateName);
		if (templates.isEmpty()) {
			throw new RuntimeException(
				String.format("Cannot link plate definition: no definition found in source '%s' with name '%s'", source, templateName));
		} else if (templates.size() > 1) {
			throw new RuntimeException(
					String.format("Cannot link plate definition: multiple definitions found in source '%s' with name '%s'", source, templateName));
		}
		Long templateId = templates.get(0).getId();
		
		String msgToPost = String.format("{ 'plateId': %d, 'templateId': %d }", plateId, templateId);
		return EventDescriptor.of(TOPIC, EVENT_REQ_PLATE_DEF_LINK, msgToPost);
	}
	
	@Override
	public TriggerDescriptor getActionCompleteTrigger(PipelineExecutionContext context) {
		Long plateId = context.resolveVar("plateId", null);
		
		EventMatchCondition matchesPlateId = new EventMatchCondition(JSON_PLATE_ID_SELECTOR, null, plateId); 
		EventMatchCondition isOK = new EventMatchCondition(JSON_OUTCOME_SELECTOR, null, "OK");
		EventMatchCondition isError = new EventMatchCondition(JSON_OUTCOME_SELECTOR, null, "ERROR");
		
		return GenericEventTrigger.buildTrigger(TOPIC, EVENT_NOTIFY_PLATE_DEF_LINKED,
				Arrays.asList(matchesPlateId, isOK),
				Arrays.asList(matchesPlateId, isError));
	}

}
