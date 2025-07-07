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
package eu.openanalytics.phaedra.pipelineservice.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import eu.openanalytics.phaedra.pipelineservice.dto.PipelineDefinition;
import eu.openanalytics.phaedra.pipelineservice.service.PipelineTriggerService;

@RestController
@RequestMapping("/pipeline-admin")
public class PipelineAdminController {

	@Autowired
	protected PipelineTriggerService triggerService;
	
	@PostMapping("/message")
    public ResponseEntity<PipelineDefinition> postMessage(
    		@RequestBody String message, 
    		@RequestParam(name = "topic") String topic,
    		@RequestParam(name = "key") String key) {
		
		try {
			triggerService.postKafkaMessage(topic, key, message);
			return ResponseEntity.ok().build();
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
		} 
    }
	
}
