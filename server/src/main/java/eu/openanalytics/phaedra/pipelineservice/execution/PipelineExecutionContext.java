package eu.openanalytics.phaedra.pipelineservice.execution;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.openanalytics.phaedra.pipelineservice.dto.PipelineDefinition;
import eu.openanalytics.phaedra.pipelineservice.dto.PipelineExecution;
import eu.openanalytics.phaedra.pipelineservice.model.config.PipelineConfig;
import eu.openanalytics.phaedra.pipelineservice.model.config.PipelineConfigParser;

public class PipelineExecutionContext {

	public PipelineDefinition definition;
	public PipelineConfig config;
	public PipelineExecution execution;

	public Map<String, Object> executionVariables = new HashMap<>();

	private static ObjectMapper jsonMapper = new ObjectMapper();
	
	public static PipelineExecutionContext build(PipelineDefinition def, PipelineExecution exec) {
		PipelineExecutionContext ctx = new PipelineExecutionContext();
		ctx.definition = def;
		ctx.execution = exec;
		ctx.config = PipelineConfigParser.parse(def.getConfig());
		
		if (exec != null && exec.getVariables() != null) {
			TypeReference<HashMap<String,Object>> typeRef = new TypeReference<HashMap<String,Object>>() {};
			try {
				ctx.executionVariables = jsonMapper.readValue(exec.getVariables(), typeRef);
			} catch (Exception e) {
				throw new RuntimeException("Failed to parse pipeline execution variables", e);
			}
		}
		
		return ctx;
	}
	
	public String resolveVar(String key, String defaultValue) {
		Object value = executionVariables.get(key);
		if (value == null) return defaultValue;
		return String.valueOf(value);
	}
	
	public void setVar(String key, String value) {
		executionVariables.put(key, value);
	}
	
	public void updateExecutionVariables() {
		try {
			String variablesString = jsonMapper.writeValueAsString(executionVariables);
			execution.setVariables(variablesString);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to serialize pipeline execution variables", e);
		}
	}
}
