package eu.openanalytics.phaedra.pipelineservice.execution.action.impl;

import org.springframework.stereotype.Component;

import com.jayway.jsonpath.JsonPath;

import eu.openanalytics.phaedra.pipelineservice.execution.PipelineExecutionContext;
import eu.openanalytics.phaedra.pipelineservice.execution.event.EventDescriptor;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.TriggerDescriptor;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.impl.GenericEventTrigger;

@Component
public class LinkPlateDefinitionAction extends EventBasedAction {

	private static final String DC_TOPIC = "plate";
	private static final String DC_KEY_PLATE_LINKED = "plateDefinitionLinked";
	
	private static final String JSON_PLATE_ID_SELECTOR = "$['plateId']";

	@Override
	protected EventDescriptor buildActionStartMessage(PipelineExecutionContext context) {
		String triggerMessage = getTriggerMessage(context);
		Number plateId = JsonPath.read(triggerMessage, JSON_PLATE_ID_SELECTOR);
		String msgToPost = String.format("{ 'plateId': %d }", plateId);
		return EventDescriptor.of(DC_TOPIC, DC_KEY_PLATE_LINKED, msgToPost);
	}
	
	@Override
	public TriggerDescriptor getActionCompleteTrigger(PipelineExecutionContext context) {
		String triggerMessage = getTriggerMessage(context);
		Object plateId = JsonPath.read(triggerMessage, JSON_PLATE_ID_SELECTOR);
		return GenericEventTrigger.buildDescriptor(DC_TOPIC, DC_KEY_PLATE_LINKED, null, JSON_PLATE_ID_SELECTOR, null, plateId);
	}

}
