package eu.openanalytics.phaedra.pipelineservice.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import eu.openanalytics.phaedra.pipelineservice.dto.PipelineDefinition;
import eu.openanalytics.phaedra.pipelineservice.dto.PipelineDefinitionStatus;
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
import eu.openanalytics.phaedra.pipelineservice.service.PipelineDefinitionService.PipelineDefinitionChangeListener;
import eu.openanalytics.phaedra.pipelineservice.service.PipelineExecutionService.PipelineExecutionChangeListener;
import eu.openanalytics.phaedra.util.auth.IAuthorizationService;

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
	
	@Autowired
	private IAuthorizationService authService;
	
	@Autowired
	protected KafkaTemplate<String, String> kafkaTemplate;
	
	private Map<String, RegisteredTrigger> registeredTriggers = new ConcurrentHashMap<>();
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private static final int PIPELINE_FIRST_STEP = 1;
	private static final int PIPELINE_COMPLETE_STEP = -1;
	
	@PostConstruct
	public void init() {
		definitionService.addPipelineDefinitionChangeListener(new PipelineDefinitionChangeListener() {
			@Override
			public void onStatusChanged(PipelineDefinition definition) {
				// If the PipelineDefinition has status ENABLED, schedule its trigger(s).
				// If it has status DISABLED, unschedule all of its triggers.
				if (definition.getStatus() == PipelineDefinitionStatus.ENABLED) {
					registerPipelineTrigger(definition.getId());
				} else {
					unregisterPipelineTrigger(definition.getId());
				}
			}
			@Override
			public void onConfigChanged(PipelineDefinition definition) {
				// If the PipelineDefinition has its config changed, re-schedule its trigger(s).
				unregisterPipelineTrigger(definition.getId());
				registerPipelineTrigger(definition.getId());
			}
		});
		executionService.addPipelineExecutionChangeListener(new PipelineExecutionChangeListener() {
			@Override
			public void onExecutionCancelled(PipelineExecution exec) {
				unregisterAllTriggers(exec.getId());
			}
		});
	}
	
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
	
	public void unregisterAllTriggers(long executionId) {
		registeredTriggers.values().stream()
			.filter(t -> t.executionId != null && t.executionId == executionId)
			.forEach(t -> registeredTriggers.remove(t.id));
		logger.debug(String.format("Unregistered all triggers [execution %d]", executionId));
	}
	
	public boolean matchAndFire(EventDescriptor event) {
		for (RegisteredTrigger rt: registeredTriggers.values()) {
			ITrigger trigger = triggerRegistry.resolve(rt.descriptor);

			// Build a transient context to be used by the trigger.
			// If the trigger matches or errors, this context will be replaced by a real execution context.
			PipelineExecutionContext ctx = buildExecutionContext(rt, false, null);
			TriggerMatchType matchType = trigger.matches(event, rt.descriptor, ctx);
			
			if (matchType == TriggerMatchType.Match) {
				logger.debug(String.format("Firing trigger [pipeline %d] [step %d]", rt.pipelineId, rt.stepNr));
				
				ctx = buildExecutionContext(rt, true, ctx);
				ctx.setVar(String.format("step.%d.trigger.message", rt.stepNr) , event.message);
				
				handleTriggerFired(rt, ctx);
				return true;
			} else if (matchType == TriggerMatchType.Error) {
				if (rt.stepNr == PIPELINE_FIRST_STEP) {
					// In case a pipeline's initial trigger fails, do not start an execution.
					// Treat this error as a NoMatch
					return false;
				} else {
					ctx = buildExecutionContext(rt, true, ctx);
					ctx.setVar(String.format("step.%d.trigger.message", rt.stepNr) , event.message);
					handleTriggerError(rt, ctx, String.format("%s: %s", event.key, event.message));
					return true;
				}
			}
		}
		return false;
	}
	
	public void postKafkaMessage(String topic, String key, String message) {
		authService.performAccessCheck(p -> authService.hasAdminAccess());
		kafkaTemplate.send(topic, key, message);
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
	
	private PipelineExecutionContext buildExecutionContext(RegisteredTrigger trigger, boolean createNewExecution, PipelineExecutionContext previousCtx) {
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

		PipelineExecutionContext context = PipelineExecutionContext.build(def, exec);
		if (previousCtx != null) {
			context.executionVariables.putAll(previousCtx.executionVariables);
		}
		return context;
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
		
		// Invoke the "onActionComplete" method on the action that was completed.
		int completedStepNr = context.execution.getCurrentStep();
		if (completedStepNr > 0 && completedStepNr <= context.config.getSteps().size()) {
			PipelineStep completedStep = context.config.getSteps().get(completedStepNr - 1);
			IAction completedStepAction = actionRegistry.resolve(completedStep.getAction());
			if (completedStepAction != null) completedStepAction.onActionComplete(context);
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
			context.updateExecutionVariables();
			executionService.update(context.execution);
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
