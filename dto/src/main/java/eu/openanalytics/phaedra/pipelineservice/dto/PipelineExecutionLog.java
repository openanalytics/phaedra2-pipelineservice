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