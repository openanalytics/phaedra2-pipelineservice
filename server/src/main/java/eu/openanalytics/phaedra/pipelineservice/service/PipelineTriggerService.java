package eu.openanalytics.phaedra.pipelineservice.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
import eu.openanalytics.phaedra.pipelineservice.execution.event.EventDescriptor;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.ITrigger;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.TriggerDescriptor;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.TriggerMatchType;
import eu.openanalytics.phaedra.pipelineservice.execution.trigger.TriggerRegistry;
import eu.openanalytics.phaedra.pipelineservice.model.config.PipelineStep;

//TODO Handle errors
@Service
public class PipelineTriggerService {

	@Autowired
	private PipelineDefinitionService definitionService;
	
	@Autowired
	private PipelineExecutionService executionService;
	
	@Autowired
	private ActionRegistry actionRegistry;
	
	@Autowired
	private TriggerRegistry triggerRegistry;
	
	private Map<String, RegisteredTrigger> registeredTriggers = new ConcurrentHashMap<>();
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private static final int PIPELINE_FIRST_STEP = 1;
	private static final int PIPELINE_COMPLETE_STEP = -1;
	
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
		logger.debug(String.format("Registered trigger [pipeline %d] [step %d]", definitionId, stepNr));
	}
	
	public void unregisterTrigger(Long definitionId, Long executionId, int stepNr) {
		registeredTriggers.values().stream()
			.filter(t -> t.pipelineId == definitionId
					&& (executionId == null || t.executionId == executionId)
					&& (executionId == null || t.stepNr == stepNr))
			.findAny().ifPresent(t -> registeredTriggers.remove(t.id));
		logger.debug(String.format("Unregistered trigger [pipeline %d] [step %d]", definitionId, stepNr));
	}
	
	public boolean matchAndFire(EventDescriptor event) {
		for (RegisteredTrigger rt: registeredTriggers.values()) {
			PipelineExecutionContext ctx = buildExecutionContext(rt, false);
			ITrigger trigger = triggerRegistry.resolve(rt.descriptor);
			
			TriggerMatchType matchType = trigger.matches(event, rt.descriptor, ctx);
			
			if (matchType == TriggerMatchType.Match) {
				logger.debug(String.format("Firing trigger [pipeline %d] [step %d]", rt.pipelineId, rt.stepNr));
				
				ctx = buildExecutionContext(rt, true);
				ctx.setVar(String.format("step.%d.trigger.message", rt.stepNr) , event.message);
				
				handleTriggerFired(rt, ctx);
				return true;
			} else if (matchType == TriggerMatchType.Error) {
				ctx = buildExecutionContext(rt, true);
				ctx.setVar(String.format("step.%d.trigger.message", rt.stepNr) , event.message);
				handleTriggerError(rt, ctx, String.format("%s: %s", event.key, event.message));
				return true;
			}
		}
		return false;
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
	
	private PipelineExecutionContext buildExecutionContext(RegisteredTrigger trigger, boolean createNewExecution) {
		// Look up the pipeline and parse its config.
		PipelineDefinition def = definitionService.findById(trigger.pipelineId)
				.orElseThrow(() -> new IllegalArgumentException(String.format("Invalid pipeline ID: %d", trigger.pipelineId)));
		
		// Look up or create a PipelineExecution.
		PipelineExecution exec = null;
		
		if (trigger.stepNr == PIPELINE_FIRST_STEP) {
			if (createNewExecution) {
				exec = executionService.createNew(trigger.pipelineId);
				executionService.log(exec.getId(), trigger.stepNr, "New execution started");
			}
		} else {
			exec = executionService.findById(trigger.executionId)
					.orElseThrow(() -> new IllegalArgumentException(String.format("Invalid pipeline execution ID: %d", trigger.executionId)));
		}

		return PipelineExecutionContext.build(def, exec);
	}
	
	private void handleTriggerFired(RegisteredTrigger trigger, PipelineExecutionContext context) {
		if (trigger.stepNr != PIPELINE_FIRST_STEP) {
			// Deactivate the step trigger that was just fired.
			unregisterTrigger(trigger.pipelineId, trigger.executionId, trigger.stepNr);
		}
		
		if (trigger.stepNr == PIPELINE_COMPLETE_STEP) {
			// No further steps to invoke: the pipeline is completed.
			context.execution.setStatus(PipelineExecutionStatus.COMPLETED);
			executionService.update(context.execution);
			executionService.log(context.execution.getId(), trigger.stepNr, "Execution completed");
			logger.debug(String.format("Pipeline %d is now completed", trigger.pipelineId));
			return;
		}
		
		// Figure out which step to invoke.
		PipelineStep stepToInvoke = (trigger.stepNr <= context.config.getSteps().size()) 
				? context.config.getSteps().get(trigger.stepNr - 1) : null;
		if (stepToInvoke == null) {
			throw new IllegalStateException("Pipeline step not found: " + trigger.stepNr);
		} else {
			context.updateExecutionVariables();
			context.execution.setStatus(PipelineExecutionStatus.RUNNING);
			context.execution.setCurrentStep(trigger.stepNr);
			executionService.update(context.execution);
		}
		
		// Activate the trigger for the next step.
		int nextStepNr = trigger.stepNr + 1;
		if (nextStepNr > context.config.getSteps().size()) nextStepNr = PIPELINE_COMPLETE_STEP;
		registerTrigger(trigger.pipelineId, context.execution.getId(), nextStepNr);
		
		// Finally, invoke the action corresponding to the current step.
		IAction actionToInvoke = actionRegistry.resolve(stepToInvoke.getAction());
		executionService.log(context.execution.getId(), trigger.stepNr, String.format("Invoking action: %s", actionToInvoke.getType()));
		logger.debug(String.format("Invoking action %s [pipeline %d] [step %d]", actionToInvoke.getType(), context.definition.getId(), trigger.stepNr));
		try {
			actionToInvoke.invoke(context);
		} catch (Throwable t) {
			handleTriggerError(trigger, context, t.getMessage());
		}
	}

	private void handleTriggerError(RegisteredTrigger trigger, PipelineExecutionContext context, String errorMessage) {
		logger.debug(String.format("Error while running action [pipeline %d] [step %d]: %s", context.definition.getId(), context.execution.getCurrentStep(), errorMessage));
		
		context.updateExecutionVariables();
		context.execution.setStatus(PipelineExecutionStatus.ERROR);
		executionService.log(context.execution.getId(), trigger.stepNr, String.format("Error during pipeline step %d: %s", context.execution.getCurrentStep(), errorMessage));
		executionService.update(context.execution);
	}

	private static class RegisteredTrigger {
		public String id;
		public TriggerDescriptor descriptor;
		public Long pipelineId;
		public Long executionId;
		public int stepNr;
	}
}
