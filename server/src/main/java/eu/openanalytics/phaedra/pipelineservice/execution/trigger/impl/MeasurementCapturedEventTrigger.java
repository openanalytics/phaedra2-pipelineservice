package eu.openanalytics.phaedra.pipelineservice.execution.trigger.impl;

import eu.openanalytics.phaedra.pipelineservice.execution.PipelineExecutionContext;
import eu.openanalytics.phaedra.pipelineservice.execution.action.impl.CaptureMeasurementAction;
import eu.openanalytics.phaedra.pipelineservice.execution.event.EventDescriptor;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.TriggerDescriptor;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.TriggerMatchType;

public class MeasurementCapturedEventTrigger extends GenericEventTrigger {

	private static final String TYPE = "MeasurementCaptured";
	
	@Override
	public String getType() {
		return TYPE;
	}
	
	@Override
	public TriggerMatchType matches(EventDescriptor event, TriggerDescriptor descriptor, PipelineExecutionContext ctx) {
		String pattern = (String) descriptor.getConfig().get("pattern");
		TriggerMatchType match = super.matches(event, CaptureMeasurementAction.buildActionCompleteTrigger(null, pattern), ctx);
		
		if (match == TriggerMatchType.Match) {
			CaptureMeasurementAction.captureMeasVariables(event.message, ctx);
		}
		
		return match;
	}
}
