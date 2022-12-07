package eu.openanalytics.phaedra.pipelineservice.dto;

import java.util.Date;

import org.springframework.data.annotation.Id;

public class PipelineExecution {

	@Id
	private Long id;
    
	private Long pipelineId;
    private String versionNumber;
    
    private String status;
    private int nextStep;
    
    private Date createdOn;
    private String createdBy;
    private Date updatedOn;
    private String updatedBy;
    
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
	public String getVersionNumber() {
		return versionNumber;
	}
	public void setVersionNumber(String versionNumber) {
		this.versionNumber = versionNumber;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public int getNextStep() {
		return nextStep;
	}
	public void setNextStep(int nextStep) {
		this.nextStep = nextStep;
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
}
