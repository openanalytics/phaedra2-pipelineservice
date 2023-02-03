package eu.openanalytics.phaedra.pipelineservice.execution;

import eu.openanalytics.phaedra.pipelineservice.dto.PipelineDefinition;
import eu.openanalytics.phaedra.pipelineservice.dto.PipelineExecution;
import eu.openanalytics.phaedra.pipelineservice.model.config.PipelineConfig;
import eu.openanalytics.phaedra.pipelineservice.model.config.PipelineConfigParser;

public class PipelineExecutionContext {

	public PipelineDefinition definition;
	public PipelineConfig config;
	public PipelineExecution execution;

	public static PipelineExecutionContext build(PipelineDefinition def, PipelineExecution exec) {
		PipelineExecutionContext ctx = new PipelineExecutionContext();
		ctx.definition = def;
		ctx.execution = exec;
		ctx.config = PipelineConfigParser.parse(def.getConfig());
		return ctx;
	}
}
