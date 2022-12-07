package eu.openanalytics.phaedra.pipelineservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.openanalytics.phaedra.pipelineservice.dto.PipelineExecution;
import eu.openanalytics.phaedra.pipelineservice.repo.PipelineExecutionRepo;

@Service
public class PipelineExecutionService {

	@Autowired
	private PipelineDefinitionService pipelineDefinitionService;
	
	@Autowired
	private PipelineExecutionRepo pipelineExecutionRepo;
	
	public PipelineExecution executePipeline(long pipelineDefinitionId) {
		return null;
	}
}
