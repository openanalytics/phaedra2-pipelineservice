package eu.openanalytics.phaedra.pipelineservice.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.modelmapper.Conditions;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.openanalytics.phaedra.pipelineservice.dto.LogMessageType;
import eu.openanalytics.phaedra.pipelineservice.dto.PipelineExecution;
import eu.openanalytics.phaedra.pipelineservice.dto.PipelineExecutionLog;
import eu.openanalytics.phaedra.pipelineservice.dto.PipelineExecutionStatus;
import eu.openanalytics.phaedra.pipelineservice.repo.PipelineExecutionLogRepo;
import eu.openanalytics.phaedra.pipelineservice.repo.PipelineExecutionRepo;

@Service
public class PipelineExecutionService {

	@Autowired
	private PipelineExecutionRepo executionRepo;
	
	@Autowired
	private PipelineExecutionLogRepo logRepo;
	
//	@Autowired
//	private IAuthorizationService authService;
	
	private ModelMapper modelMapper = new ModelMapper();
	
	private static final String USERNAME_SYSTEM = "System";
	
	public PipelineExecution createNew(Long pipelineId) {
		PipelineExecution execution = new PipelineExecution();
		execution.setPipelineId(pipelineId);
		return createNew(execution);
	}
	
	public PipelineExecution createNew(PipelineExecution execution) {
		execution.setCreatedOn(new Date());
		execution.setCreatedBy(USERNAME_SYSTEM);
		execution.setStatus(PipelineExecutionStatus.CREATED);
		execution.setCurrentStep(0);
		validate(execution, true);
		return executionRepo.save(execution);
	}

	public Optional<PipelineExecution> findById(long id) {
		return executionRepo.findById(id);
	}
	
	public boolean exists(long id) {
		return executionRepo.existsById(id);
	}
	
	public Optional<PipelineExecution> findFirst(Predicate<PipelineExecution> filter) {
		for (PipelineExecution exec: executionRepo.findAll()) {
			if (filter.test(exec)) return Optional.of(exec);
		}
		return Optional.empty();
	}
	
	public List<PipelineExecution> findAll(Predicate<PipelineExecution> filter) {
		List<PipelineExecution> matches = new ArrayList<>();
		for (PipelineExecution exec: executionRepo.findAll()) {
			if (filter == null || filter.test(exec)) matches.add(exec);
		}
		return matches;
	}
	
	public void log(long executionId, int stepNr, String message) {
		PipelineExecutionLog log = new PipelineExecutionLog();
		log.setLogDate(new Date());
		log.setPipelineExecutionId(executionId);
		log.setStepNr(stepNr);
		log.setMessage(message);
		log.setMessageType(LogMessageType.Info.name());
		logRepo.save(log);
	}
	
	public void log(PipelineExecutionLog log) {
		logRepo.save(log);
	}
	
	public List<PipelineExecutionLog> getLog(long executionId) {
		return logRepo.findAllByPipelineExecutionId(executionId);
	}
	
	public PipelineExecution update(PipelineExecution execution) {
		// Look up the existing record
		PipelineExecution existingExecution = executionRepo
				.findById(execution.getId())
				.orElseThrow(() -> new IllegalArgumentException("Pipeline execution not found with ID " + execution.getId()));

//		authService.performOwnershipCheck(existingExecution.getCreatedBy());
		
		// Map the updated fields onto the existing definition
		modelMapper.typeMap(PipelineExecution.class, PipelineExecution.class)
			.setPropertyCondition(Conditions.isNotNull())
			.map(execution, existingExecution);
		
		existingExecution.setUpdatedOn(new Date());
		existingExecution.setUpdatedBy(USERNAME_SYSTEM);
		
		validate(existingExecution, false);
		return executionRepo.save(existingExecution);
	}
	
	private void validate(PipelineExecution exec, boolean isNew) {
		if (isNew) Assert.isTrue(exec.getId() == null, "New pipeline execution must have ID equal to 0");
		Assert.notNull(exec.getPipelineId(), "Pipeline execution must reference a valid pipeline definition");
		Assert.hasText(exec.getCreatedBy(), "Pipeline execution creator cannot be empty");
		Assert.notNull(exec.getCreatedOn(), "Pipeline execution creation date cannot be null");
	}
}
