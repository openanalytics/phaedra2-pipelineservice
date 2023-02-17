package eu.openanalytics.phaedra.pipelineservice.dto;

import java.util.Date;

import org.springframework.data.annotation.Id;

public class PipelineExecution {

	@Id
	private Long id;
    
	private Long pipelineId;
    
    private PipelineExecutionStatus status;
    private int currentStep;
    
    private Date createdOn;
    private String createdBy;
    private Date updatedOn;
    private String updatedBy;
    
    private String variables;
    
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getPipelineId() {
		return pipelineId;
	}
	public void setPipelineId(Long pipelineId) {
		this.pipelineId = pipelineId;
	}
	public PipelineExecutionStatus getStatus() {
		return status;
	}
	public void setStatus(PipelineExecutionStatus status) {
		this.status = status;
	}
	public int getCurrentStep() {
		return currentStep;
	}
	public void setCurrentStep(int currentStep) {
		this.currentStep = currentStep;
	}
	public Date getCreatedOn() {
		return createdOn;
	}
	public void setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
	}
	public String getCreatedBy() {
		return createdBy;
	}
	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}
	public Date getUpdatedOn() {
		return updatedOn;
	}
	public void setUpdatedOn(Date updatedOn) {
		this.updatedOn = updatedOn;
	}
	public String getUpdatedBy() {
		return updatedBy;
	}
	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}
	public String getVariables() {
		return variables;
	}
	public void setVariables(String variables) {
		this.variables = variables;
	}
}
