package eu.openanalytics.phaedra.pipelineservice.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.openanalytics.phaedra.pipelineservice.dto.PipelineDefinition;
import eu.openanalytics.phaedra.pipelineservice.dto.PipelineExecution;
import eu.openanalytics.phaedra.pipelineservice.dto.PipelineExecutionStatus;
import eu.openanalytics.phaedra.pipelineservice.execution.PipelineExecutionContext;
import eu.openanalytics.phaedra.pipelineservice.execution.action.ActionRegistry;
import eu.openanalytics.phaedra.pipelineservice.execution.action.IAction;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.TriggerDescriptor;
import eu.openanalytics.phaedra.pipelineservice.model.config.PipelineStep;

@Service
public class PipelineTriggerService {

	@Autowired
	private PipelineDefinitionService definitionService;
	
	@Autowired
	private PipelineExecutionService executionService;
	
	@Autowired
	private ActionRegistry actionRegistry;
	
	private Map<String, RegisteredTrigger> registeredTriggers = new ConcurrentHashMap<>();
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private static final int PIPELINE_FIRST_STEP = 1;
	private static final int PIPELINE_COMPLETE_STEP = -1;
	
	public static final String TRIGGER_TYPE_EVENT_LISTENER = "EventListener";
	
	public void registerPipelineTrigger(Long definitionId) {
		registerTrigger(definitionId, null, PIPELINE_FIRST_STEP);
	}
	
	public void unregisterPipelineTrigger(Long definitionId) {
		unregisterTrigger(definitionId, null, PIPELINE_FIRST_STEP);
	}
	
	public void registerTrigger(Long definitionId, Long executionId, int stepNr) {
		PipelineDefinition def = definitionService.findById(definitionId)
				.orElseThrow(() -> new IllegalArgumentException(String.format("Invalid pipeline ID: %d", definitionId)));
		
		PipelineExecution exec = executionId == null ? null : executionService.findById(executionId).orElse(null);
		PipelineExecutionContext context = PipelineExecutionContext.build(def, exec);
		
		RegisteredTrigger trigger = new RegisteredTrigger();
		trigger.id = UUID.randomUUID().toString();
		trigger.pipelineId = definitionId;
		trigger.executionId = executionId;
		trigger.stepNr = stepNr;
		trigger.descriptor = getStepTrigger(stepNr, context);
		
		registeredTriggers.put(trigger.id, trigger);
		logger.debug(String.format("Registered trigger for pipeline %d, step %d", definitionId, stepNr));
	}
	
	public void unregisterTrigger(Long definitionId, Long executionId, int stepNr) {
		registeredTriggers.values().stream()
			.filter(t -> t.pipelineId == definitionId
					&& (executionId == null || t.executionId == executionId)
					&& (executionId == null || t.stepNr == stepNr))
			.findAny().ifPresent(t -> registeredTriggers.remove(t.id));
		logger.debug(String.format("Unregistered trigger for pipeline %d, step %d", definitionId, stepNr));
	}
	
	public void fireTriggerNow(RegisteredTrigger trigger) {
		logger.debug(String.format("Firing trigger for pipeline %d, step %d", trigger.pipelineId, trigger.stepNr));
		handleTriggerFired(trigger);
	}
	
	public RegisteredTrigger findMatchingTrigger(Predicate<RegisteredTrigger> filter) {
		return registeredTriggers.values().stream().filter(filter).findAny().orElse(null);
	}
	
	private TriggerDescriptor getStepTrigger(int stepNr, PipelineExecutionContext context) {
		if (stepNr == PIPELINE_COMPLETE_STEP) {
			int stepCount = context.config.getSteps().size();
			PipelineStep previousStep = context.config.getSteps().get(stepCount - 1);
			IAction previousAction = actionRegistry.resolve(previousStep.getAction());
			return previousAction.getActionCompleteTrigger(context);
		}
		
		if (stepNr > context.config.getSteps().size()) throw new IllegalArgumentException(String.format("Invalid pipeline step: %d", stepNr));
		TriggerDescriptor descriptor = context.config.getSteps().get(stepNr - 1).getTrigger();
		
		// If no explicit trigger is given, try to use the implicit trigger of the previous action
		if (descriptor == null && stepNr > 1) {
			PipelineStep previousStep = context.config.getSteps().get(stepNr - 2);
			IAction previousAction = actionRegistry.resolve(previousStep.getAction());
			descriptor = previousAction.getActionCompleteTrigger(context);
		}
		
		return descriptor;
	}
	
	private void handleTriggerFired(RegisteredTrigger trigger) {
		logger.debug(String.format("Handling trigger, type: %s, pipeline: %d, step: %d", 
				(trigger.descriptor == null ? null : trigger.descriptor.getType()), trigger.pipelineId, trigger.stepNr));

		// Look up the pipeline and parse its config.
		PipelineDefinition def = definitionService.findById(trigger.pipelineId)
				.orElseThrow(() -> new IllegalArgumentException(String.format("Invalid pipeline ID: %d", trigger.pipelineId)));
		
		// Look up or create a PipelineExecution.
		PipelineExecution exec = null;
		
		if (trigger.stepNr == PIPELINE_FIRST_STEP) {
			exec = executionService.createNew(trigger.pipelineId);
			executionService.log(exec.getId(), trigger.stepNr, "New execution started");
			// Note: the trigger for step 0 always remains active, as it spawns new pipeline executions. 
		} else {
			exec = executionService.findById(trigger.executionId)
					.orElseThrow(() -> new IllegalArgumentException(String.format("Invalid pipeline execution ID: %d", trigger.executionId)));
			
			// Deactivate the step trigger that was just fired.
			unregisterTrigger(trigger.pipelineId, trigger.executionId, trigger.stepNr);
		}

		PipelineExecutionContext context = PipelineExecutionContext.build(def, exec);
		
		if (trigger.stepNr == PIPELINE_COMPLETE_STEP) {
			// No further steps to invoke: the pipeline is completed.
			exec.setStatus(PipelineExecutionStatus.COMPLETED);
			executionService.update(exec);
			executionService.log(exec.getId(), trigger.stepNr, "Execution completed");
			logger.debug(String.format("Pipeline %d is now completed", trigger.pipelineId));
			return;
		}
		
		// Figure out which step to invoke.
		PipelineStep stepToInvoke = (trigger.stepNr <= context.config.getSteps().size()) 
				? context.config.getSteps().get(trigger.stepNr - 1) : null;
		if (stepToInvoke == null) {
			throw new IllegalStateException("Pipeline step not found: " + trigger.stepNr);
		} else {
			exec.setStatus(PipelineExecutionStatus.RUNNING);
			exec.setCurrentStep(trigger.stepNr);
			executionService.update(exec);
		}
		
		// Activate the trigger for the next step.
		int nextStepNr = trigger.stepNr + 1;
		if (nextStepNr > context.config.getSteps().size()) nextStepNr = PIPELINE_COMPLETE_STEP;
		registerTrigger(trigger.pipelineId, exec.getId(), nextStepNr);
		
		// Finally, invoke the action corresponding to the current step.
		IAction actionToInvoke = actionRegistry.resolve(stepToInvoke.getAction());
		executionService.log(exec.getId(), trigger.stepNr, String.format("Invoking action: %s", stepToInvoke.getAction().getType()));
		actionToInvoke.invoke(context);
	}
	
	public static class RegisteredTrigger {
		
		public String id;
		public TriggerDescriptor descriptor;
		public Long pipelineId;
		public Long executionId;
		public int stepNr;
		
	}

}
