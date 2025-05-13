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
package eu.openanalytics.phaedra.pipelineservice.dto;

import java.util.Date;

import org.springframework.data.annotation.Id;

public class PipelineExecutionLog {

	@Id
	private Long id;
	
	private Long pipelineExecutionId;
	private Date logDate;
	private int stepNr;
	private String message;
	private String messageType;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getPipelineExecutionId() {
		return pipelineExecutionId;
	}
	public void setPipelineExecutionId(Long pipelineExecutionId) {
		this.pipelineExecutionId = pipelineExecutionId;
	}
	public Date getLogDate() {
		return logDate;
	}
	public void setLogDate(Date logDate) {
		this.logDate = logDate;
	}
	public int getStepNr() {
		return stepNr;
	}
	public void setStepNr(int stepNr) {
		this.stepNr = stepNr;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getMessageType() {
		return messageType;
	}
	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}

}