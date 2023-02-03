package eu.openanalytics.phaedra.pipelineservice.model.config;

import java.util.ArrayList;
import java.util.List;

public class PipelineConfig {

	private List<PipelineStep> steps = new ArrayList<>();

	public List<PipelineStep> getSteps() {
		return steps;
	}

	public void setSteps(List<PipelineStep> steps) {
		this.steps = steps;
	}

}
