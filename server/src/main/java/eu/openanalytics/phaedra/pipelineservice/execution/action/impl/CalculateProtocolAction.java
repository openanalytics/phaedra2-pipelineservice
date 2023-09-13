package eu.openanalytics.phaedra.pipelineservice.execution.action.impl;

import java.util.Arrays;

import eu.openanalytics.phaedra.pipelineservice.execution.PipelineExecutionContext;
import eu.openanalytics.phaedra.pipelineservice.execution.event.EventDescriptor;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.TriggerDescriptor;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.impl.GenericEventTrigger;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.impl.GenericEventTrigger.EventMatchCondition;

public class CalculateProtocolAction extends EventBasedAction {

	private static final String TOPIC = "calculations";
	
	private static final String EVENT_REQ_PLATE_CALCULATION = "requestPlateCalculation";
	private static final String EVENT_NOTIFY_CALCULATION_EVENT = "notifyCalculationEvent";
	
	private static final String JSON_PLATE_ID_SELECTOR = "$.plateId";
	private static final String JSON_CALC_STATUS_SELECTOR = "$.calculationStatus";
	
	@Override
	protected EventDescriptor buildActionStartMessage(PipelineExecutionContext context) {
		Number plateId = getRequiredVar("plateId", context, null);
		Number measId = getRequiredVar("measurementId", context, null);
		Number protocolId = getRequiredVar("currentStep.action.config.id", context, null);
		
		String msgToPost = String.format("{ 'plateId': %d, 'measId': %d, 'protocolId': %d }", plateId, measId, protocolId);
		return EventDescriptor.of(TOPIC, EVENT_REQ_PLATE_CALCULATION, msgToPost);
	}
	
	@Override
	public TriggerDescriptor getActionCompleteTrigger(PipelineExecutionContext context) {
		Number plateId = context.resolveVar("plateId", null);

		EventMatchCondition matchesPlateId = new EventMatchCondition(JSON_PLATE_ID_SELECTOR, null, plateId);
		EventMatchCondition isOK = new EventMatchCondition(JSON_CALC_STATUS_SELECTOR, null, "1");
		EventMatchCondition isError = new EventMatchCondition(JSON_CALC_STATUS_SELECTOR, null, "-2");
		
		return GenericEventTrigger.buildTrigger(TOPIC, EVENT_NOTIFY_CALCULATION_EVENT,
				Arrays.asList(matchesPlateId, isOK),
				Arrays.asList(matchesPlateId, isError));
	}

}
