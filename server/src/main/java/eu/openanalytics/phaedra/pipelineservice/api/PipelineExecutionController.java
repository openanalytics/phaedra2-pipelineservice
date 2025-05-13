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

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import eu.openanalytics.phaedra.pipelineservice.dto.PipelineDefinition;
import eu.openanalytics.phaedra.pipelineservice.dto.PipelineExecution;
import eu.openanalytics.phaedra.pipelineservice.dto.PipelineExecutionLog;
import eu.openanalytics.phaedra.pipelineservice.service.PipelineExecutionService;

@RestController
@RequestMapping("/pipeline-executions")
public class PipelineExecutionController {

	@Autowired
	private PipelineExecutionService pipelineExecutionService;
	
	@GetMapping("/{id}")
	public ResponseEntity<PipelineExecution> getPipelineExecution(@PathVariable long id) {
		return ResponseEntity.of(pipelineExecutionService.findById(id));
	}
	
	@GetMapping("/{id}/log")
	public ResponseEntity<List<PipelineExecutionLog>> getPipelineExecutionLog(@PathVariable long id) {
		return ResponseEntity.ok(pipelineExecutionService.getLog(id));
	}
	
	@GetMapping
	public ResponseEntity<List<PipelineExecution>> getAllPipelineExecutions(
			@RequestParam(name = "from", required = false) String from,
			@RequestParam(name = "to", required = false) String to) {
		
		Date fromDate = (from == null) ? null : new Date(Long.parseLong(from));
		Date toDate = (to == null) ? null : new Date(Long.parseLong(to));
		
		if (fromDate == null || toDate == null) {
			return ResponseEntity.ok(pipelineExecutionService.findAll(null));
		} else {
			return ResponseEntity.ok(pipelineExecutionService.findBetween(fromDate, toDate));
		}
	}
	
    @PutMapping("/{id}/cancel")
    public ResponseEntity<PipelineDefinition> cancelPipelineExecution(@PathVariable long id) {
    	try {
	    	pipelineExecutionService.cancelExecution(id);
	    	return new ResponseEntity<>(HttpStatus.OK);
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
		} 
    }
}
