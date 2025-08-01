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
package eu.openanalytics.phaedra.pipelineservice.service;

import static org.apache.commons.collections4.CollectionUtils.*;

import eu.openanalytics.phaedra.metadataservice.client.MetadataServiceGraphQlClient;
import eu.openanalytics.phaedra.metadataservice.dto.MetadataDTO;
import eu.openanalytics.phaedra.metadataservice.dto.TagDTO;
import eu.openanalytics.phaedra.metadataservice.dto.PropertyDTO;
import eu.openanalytics.phaedra.metadataservice.enumeration.ObjectClass;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.Conditions;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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
	private MetadataServiceGraphQlClient metadataServiceGraphQlClient;

	@Autowired
	private IAuthorizationService authService;

	private List<PipelineDefinitionChangeListener> changeListeners = new ArrayList<>();

	private ModelMapper modelMapper = new ModelMapper();

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@EventListener(ApplicationReadyEvent.class)
	public void initializeDefinitionTriggers() {
		logger.debug("Initializing all pipelines");
		pipelineDefinitionRepo.findAll().forEach(pd -> {
			try {
				handleStatusChanged(pd);
			} catch (Exception e) {
				logger.warn(String.format("Failed to initialize pipeline %s (%d)", pd.getName(), pd.getId()), e);
			}
		});
	}

	public Optional<PipelineDefinition> findById(long id) {
		Optional<PipelineDefinition> def = pipelineDefinitionRepo.findById(id);
		if (def.isPresent()) {
			enrichWithMetadata(List.of(def.get()));
			return def;
		}
		return def;
	}

	public List<PipelineDefinition> findByName(String name) {
		List<PipelineDefinition> result = pipelineDefinitionRepo.findAllByName(name);
		enrichWithMetadata(result);
		return result;
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
		enrichWithMetadata(matches);
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
		if (configChanged) handleConfigChanged(newDefinition);
		return newDefinition;
	}

	public void delete(long definitionId) {
		PipelineDefinition def = findById(definitionId).orElseThrow(() -> new IllegalArgumentException("Invalid pipeline ID: " + definitionId));
		authService.performOwnershipCheck(def.getCreatedBy());

		pipelineDefinitionRepo.deleteById(definitionId);
	}

	public void addPipelineDefinitionChangeListener(PipelineDefinitionChangeListener listener) {
		changeListeners.add(listener);
	}

	private void validate(PipelineDefinition def, boolean isNew) {
		if (isNew) Assert.isTrue(def.getId() == null, "New pipeline definition must have ID equal to 0");
		Assert.hasText(def.getName(), "Pipeline definition name cannot be empty");
		Assert.hasText(def.getCreatedBy(), "Pipeline definition creator cannot be empty");
		Assert.notNull(def.getCreatedOn(), "Pipeline definition creation date cannot be null");
		Assert.isTrue(VersionUtils.isValidVersionNumber(def.getVersionNumber()), "Pipeline definition must have a valid version number");
		//TODO Parse and validate config
	}

	private void handleStatusChanged(PipelineDefinition definition) {
		for (PipelineDefinitionChangeListener l: changeListeners) {
			l.onStatusChanged(definition);
		}
	}

	private void handleConfigChanged(PipelineDefinition definition) {
		for (PipelineDefinitionChangeListener l: changeListeners) {
			l.onConfigChanged(definition);
		}
	}

	private void enrichWithMetadata(List<PipelineDefinition> pipelineDefinitions) {
		if (isNotEmpty(pipelineDefinitions)) {
			Map<Long, PipelineDefinition> pipelineDefinitionMap = new HashMap<>();
			List<Long> pipelineDefinitionIds = new ArrayList<>(pipelineDefinitions.size());
			for (PipelineDefinition pipelineDefinition : pipelineDefinitions) {
				pipelineDefinitionMap.put(pipelineDefinition.getId(), pipelineDefinition);
				pipelineDefinitionIds.add(pipelineDefinition.getId());
			}

			List<MetadataDTO> pipelineDefinitionMetadataList = metadataServiceGraphQlClient
					.getMetadata(pipelineDefinitionIds, ObjectClass.PIPELINE);

			for (MetadataDTO metadata : pipelineDefinitionMetadataList) {
				PipelineDefinition pipelineDefinition = pipelineDefinitionMap.get(metadata.getObjectId());
				if (pipelineDefinition != null) {
					pipelineDefinition.setTags(metadata.getTags().stream()
							.map(TagDTO::getTag)
							.toList());
					List<PropertyDTO> propertyDTOs = new ArrayList<>(metadata.getProperties().size());
					for (PropertyDTO property : metadata.getProperties()) {
						propertyDTOs.add(property);
					}
					pipelineDefinition.setProperties(propertyDTOs);
				}
			}
		}
	}

	public static class PipelineDefinitionChangeListener {
		public void onStatusChanged(PipelineDefinition def) {}
		public void onConfigChanged(PipelineDefinition def) {}
	}
}
