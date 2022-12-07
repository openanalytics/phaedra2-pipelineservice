package eu.openanalytics.phaedra.pipelineservice.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.modelmapper.Conditions;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.openanalytics.phaedra.pipelineservice.dto.PipelineDefinition;
import eu.openanalytics.phaedra.pipelineservice.repo.PipelineDefinitionRepo;
import eu.openanalytics.phaedra.util.auth.IAuthorizationService;
import eu.openanalytics.phaedra.util.versioning.VersionUtils;

@Service
public class PipelineDefinitionService {

	@Autowired
	private PipelineDefinitionRepo pipelineDefinitionRepo;
	
	@Autowired
	private IAuthorizationService authService;
	
	private ModelMapper modelMapper = new ModelMapper();
	
	public Optional<PipelineDefinition> findById(long id) {
		return pipelineDefinitionRepo.findById(id);
	}
	
	public List<PipelineDefinition> findByName(String name) {
		return pipelineDefinitionRepo.findAllByName(name);
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
		
		// Map the updated fields onto the existing definition
		modelMapper.typeMap(PipelineDefinition.class, PipelineDefinition.class)
			.setPropertyCondition(Conditions.isNotNull())
			.map(definition, existingDefinition);
		
		existingDefinition.setVersionNumber(VersionUtils.generateNewVersion(existingDefinition.getVersionNumber()));
		existingDefinition.setUpdatedOn(new Date());
		existingDefinition.setUpdatedBy(authService.getCurrentPrincipalName());
		validate(existingDefinition, false);
		return pipelineDefinitionRepo.save(existingDefinition);
	}
	
	private void validate(PipelineDefinition def, boolean isNew) {
		if (isNew) Assert.isTrue(def.getId() == null, "New pipeline definition must have ID equal to 0");
		Assert.hasText(def.getName(), "Pipeline definition name cannot be empty");
		Assert.hasText(def.getCreatedBy(), "Pipeline definition creator cannot be empty");
		Assert.notNull(def.getCreatedOn(), "Pipeline definition creation date cannot be null");
		Assert.isTrue(VersionUtils.isValidVersionNumber(def.getVersionNumber()), "Pipeline definition must have a valid version number");
	}
}
