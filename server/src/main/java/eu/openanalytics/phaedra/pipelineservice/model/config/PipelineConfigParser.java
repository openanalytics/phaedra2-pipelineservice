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
