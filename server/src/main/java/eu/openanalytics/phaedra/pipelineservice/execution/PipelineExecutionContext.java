package eu.openanalytics.phaedra.pipelineservice.execution;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	
	private static final Pattern TRIGGER_CFG_PATTERN = Pattern.compile("step\\.(\\d+)\\.trigger\\.config\\.(.*)");
	private static final Pattern ACTION_CFG_PATTERN = Pattern.compile("step\\.(\\d+)\\.action\\.config\\.(.*)");
	
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
	
	@SuppressWarnings("unchecked")
	public <T> T resolveVar(String key, T defaultValue) {
		if (key.contains("currentStep.")) {
			int stepNr = execution.getCurrentStep();
			key = key.replace("currentStep.", String.format("step.%d.", stepNr));
		}
		Object value = executionVariables.get(key);
		if (value == null) {
			Matcher m = TRIGGER_CFG_PATTERN.matcher(key);
			if (m.matches()) {
				int stepNr = Integer.parseInt(m.group(1));
				String cfgKey = m.group(2);
				value = config.getSteps().get(stepNr - 1).getTrigger().getConfig().get(cfgKey);
			}
			m = ACTION_CFG_PATTERN.matcher(key);
			if (m.matches()) {
				int stepNr = Integer.parseInt(m.group(1));
				String cfgKey = m.group(2);
				value = config.getSteps().get(stepNr - 1).getAction().getConfig().get(cfgKey);
			}
		}
		if (value == null) return defaultValue;
		return (T) value;
	}
	
	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^{}]*)\\}");
	
	public String resolveVars(String text) {
		Matcher m = PLACEHOLDER_PATTERN.matcher(text);
		StringBuilder sb = new StringBuilder();
		int previousEnd = 0;
		while (m.find()) {
			sb.append(text.substring(previousEnd, m.start()));
			sb.append(String.valueOf(resolveVar(m.group(1), "")));
			previousEnd = m.end();
		}
		if (previousEnd < text.length()) {
			sb.append(text.substring(previousEnd));
		}
		return sb.toString();
	}
	
	public void setVar(String key, Object value) {
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
