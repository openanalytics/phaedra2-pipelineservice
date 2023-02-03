package eu.openanalytics.phaedra.pipelineservice.execution.trigger;

import java.util.HashMap;
import java.util.Map;

public class TriggerDescriptor {

	private String type;
	private Map<String, Object> config = new HashMap<String, Object>();
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Map<String, Object> getConfig() {
		return config;
	}
	public void setConfig(Map<String, Object> config) {
		this.config = config;
	}
	
}
