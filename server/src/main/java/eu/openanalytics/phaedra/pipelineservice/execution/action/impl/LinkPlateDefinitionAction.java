/**
 * Phaedra II
 *
 * Copyright (C) 2016-2025 Open Analytics
 *
 * ===========================================================================
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License as published by
 * The Apache Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License for more details.
 *
 * You should have received a copy of the Apache License
 * along with this program.  If not, see <http://www.apache.org/licenses/>
 */
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
import eu.openanalytics.phaedra.platedef.client.PlateDefinitionServiceClient;
import eu.openanalytics.phaedra.platedef.model.PlateTemplate;
import eu.openanalytics.phaedra.plateservice.enumeration.LinkType;

@Component
public class LinkPlateDefinitionAction extends EventBasedAction {

	private static final String TOPIC = "plates";

	private static final String EVENT_REQ_PLATE_DEF_LINK = "requestPlateDefinitionLink";
	private static final String EVENT_NOTIFY_PLATE_DEF_LINKED = "notifyPlateDefinitionLinked";
	
	private static final String JSON_PLATE_ID_SELECTOR = "$.plateId";
	private static final String JSON_OUTCOME_SELECTOR = "$.outcome";

	@Autowired
	private PlateDefinitionServiceClient plateDefinitionServiceClient;
	
	@Override
	protected EventDescriptor buildActionStartMessage(PipelineExecutionContext context) {
		Number plateId = getRequiredVar("plateId", context, null);
		
		String linkTypeName = context.resolveVar("currentStep.action.config.type", "template");
		LinkType linkType = LinkType.findByName(linkTypeName, false);
		if (linkType == null) {
			throw new IllegalArgumentException(String.format("Unsupported link type: %s", linkTypeName));
		}
		
		Long targetId = null;
		
		if (linkType == LinkType.Template) {
			String templateName = context.resolveVar("currentStep.action.config.name", "${barcode}");
			templateName = context.resolveVars(templateName);
		
			List<PlateTemplate> templates = plateDefinitionServiceClient.getPlateTemplatesByName(templateName);
			if (templates.isEmpty()) {
				throw new RuntimeException(
					String.format("Cannot link plate definition: no definition found in source '%s' with name '%s'", linkTypeName, templateName));
			} else if (templates.size() > 1) {
				throw new RuntimeException(
						String.format("Cannot link plate definition: multiple definitions found in source '%s' with name '%s'", linkTypeName, templateName));
			}
			targetId = templates.get(0).getId();
		} else if (linkType == LinkType.PlateDefinition) {
			Number sourceId = getRequiredVar("currentStep.action.config.id", context, null);
			targetId = sourceId.longValue();
		}
		
		String msgToPost = String.format("{ \"plateId\": %d, \"linkType\": \"%s\", \"targetId\": %d }", plateId, linkType.name(), targetId);
		return EventDescriptor.of(TOPIC, EVENT_REQ_PLATE_DEF_LINK, msgToPost);
	}
	
	@Override
	public TriggerDescriptor getActionCompleteTrigger(PipelineExecutionContext context) {
		Number plateId = context.resolveVar("plateId", null);
		
		EventMatchCondition matchesPlateId = new EventMatchCondition(JSON_PLATE_ID_SELECTOR, null, plateId); 
		EventMatchCondition isOK = new EventMatchCondition(JSON_OUTCOME_SELECTOR, null, "OK");
		EventMatchCondition isError = new EventMatchCondition(JSON_OUTCOME_SELECTOR, null, "ERROR");
		
		return GenericEventTrigger.buildTrigger(TOPIC, EVENT_NOTIFY_PLATE_DEF_LINKED,
				Arrays.asList(matchesPlateId, isOK),
				Arrays.asList(matchesPlateId, isError));
	}

}
