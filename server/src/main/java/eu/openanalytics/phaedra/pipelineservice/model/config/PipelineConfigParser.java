package eu.openanalytics.phaedra.pipelineservice.model.config;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

public class PipelineConfigParser {

	private static ObjectMapper jsonMapper = new ObjectMapper();
	
	public static PipelineConfig parse(String json) {
		try {
			PipelineConfig cfg = jsonMapper.readValue(json, PipelineConfig.class);
			return cfg;
		} catch (IOException e) {
			throw new RuntimeException("Failed to parse pipeline config", e);
		}
	}
}
