package eu.openanalytics.phaedra.pipelineservice.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.modelmapper.Conditions;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.openanalytics.phaedra.pipelineservice.dto.PipelineDefinition;
import eu.openanalytics.phaedra.pipelineservice.dto.PipelineDefinitionStatus;
import eu.openanalytics.phaedra.pipelineservice.repo.PipelineDefinitionRepo;
import eu.openanalytics.phaedra.util.auth.IAuthorizationService;
import eu.openanalytics.phaedra.util.versioning.VersionUtils;

@Service
public class PipelineDefinitionService {

	@Autowired
	private PipelineDefinitionRepo pipelineDefinitionRepo;
	
	@Autowired
	private PipelineTriggerService pipelineTriggerService;
	
	@Autowired
	private IAuthorizationService authService;
	
	private ModelMapper modelMapper = new ModelMapper();
	
	@PostConstruct
	public void initializeDefinitionTriggers() {
		ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
		executorService.schedule(() -> {
			pipelineDefinitionRepo.findAll().forEach(pd -> handleStatusChanged(pd));
			executorService.shutdown();
		}, 10, TimeUnit.SECONDS);
	}
	
	public Optional<PipelineDefinition> findById(long id) {
		return pipelineDefinitionRepo.findById(id);
	}
	
	public List<PipelineDefinition> findByName(String name) {
		return pipelineDefinitionRepo.findAllByName(name);
	}
	
	public Optional<PipelineDefinition> findFirst(Predicate<PipelineDefinition> filter) {
		for (PipelineDefinition def: pipelineDefinitionRepo.findAll()) {
			if (filter.test(def)) return Optional.of(def);
		}
		return Optional.empty();
	}
	
	public List<PipelineDefinition> findAll(Predicate<PipelineDefinition> filter) {
		List<PipelineDefinition> matches = new ArrayList<>();
		for (PipelineDefinition def: pipelineDefinitionRepo.findAll()) {
			if (filter == null || filter.test(def)) matches.add(def);
		}
		return matches;
	}
	
	public PipelineDefinition createNew(PipelineDefinition definition) {
		authService.performAccessCheck(p -> authService.hasUserAccess());

		definition.setCreatedOn(new Date());
		definition.setCreatedBy(authService.getCurrentPrincipalName());
		if (definition.getVersionNumber() == null) definition.setVersionNumber(VersionUtils.generateNewVersion(null));
		validate(definition, true);
		return pipelineDefinitionRepo.save(definition);
	}
	
	public boolean exists(long id) {
		return pipelineDefinitionRepo.existsById(id);
	}
	
	public PipelineDefinition update(PipelineDefinition definition) {
		// Look up the existing definition
		PipelineDefinition existingDefinition = pipelineDefinitionRepo
				.findById(definition.getId())
				.orElseThrow(() -> new IllegalArgumentException("Pipeline definition not found with ID " + definition.getId()));

		authService.performOwnershipCheck(existingDefinition.getCreatedBy());
		
		boolean statusChanged = definition.getStatus() != existingDefinition.getStatus();
		boolean configChanged = !StringUtils.equals(definition.getConfig(), existingDefinition.getConfig());
		
		// Map the updated fields onto the existing definition
		modelMapper.typeMap(PipelineDefinition.class, PipelineDefinition.class)
			.setPropertyCondition(Conditions.isNotNull())
			.map(definition, existingDefinition);
		
		if (configChanged) {
			existingDefinition.setVersionNumber(VersionUtils.generateNewVersion(existingDefinition.getVersionNumber()));
		}
		existingDefinition.setUpdatedOn(new Date());
		existingDefinition.setUpdatedBy(authService.getCurrentPrincipalName());
		validate(existingDefinition, false);

		PipelineDefinition newDefinition = pipelineDefinitionRepo.save(existingDefinition);
		if (statusChanged) handleStatusChanged(newDefinition);
		return newDefinition;
	}
	
	public void delete(long definitionId) {
		PipelineDefinition def = findById(definitionId).orElseThrow(() -> new IllegalArgumentException("Invalid pipeline ID: " + definitionId));
		authService.performOwnershipCheck(def.getCreatedBy());
		
		pipelineDefinitionRepo.deleteById(definitionId);
	}
	
	private void validate(PipelineDefinition def, boolean isNew) {
		if (isNew) Assert.isTrue(def.getId() == null, "New pipeline definition must have ID equal to 0");
		Assert.hasText(def.getName(), "Pipeline definition name cannot be empty");
		Assert.hasText(def.getCreatedBy(), "Pipeline definition creator cannot be empty");
		Assert.notNull(def.getCreatedOn(), "Pipeline definition creation date cannot be null");
		Assert.isTrue(VersionUtils.isValidVersionNumber(def.getVersionNumber()), "Pipeline definition must have a valid version number");
		//TODO Parse and validate config
	}
	
	/**
	 * If the PipelineDefinition has status ENABLED, schedule its trigger(s).
	 * If it has status DISABLED, unschedule all of its triggers.
	 */
	private void handleStatusChanged(PipelineDefinition definition) {
		if (definition.getStatus() == PipelineDefinitionStatus.ENABLED) {
			pipelineTriggerService.registerPipelineTrigger(definition.getId());
		} else {
			pipelineTriggerService.unregisterPipelineTrigger(definition.getId());
		}
		
	}
	
}
