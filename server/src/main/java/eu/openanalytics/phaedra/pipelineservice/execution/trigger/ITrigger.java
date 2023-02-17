package eu.openanalytics.phaedra.pipelineservice.execution.trigger;

import eu.openanalytics.phaedra.pipelineservice.execution.PipelineExecutionContext;
import eu.openanalytics.phaedra.pipelineservice.execution.event.EventDescriptor;

public interface ITrigger {

	public String getType();
	
	public boolean matches(EventDescriptor event, TriggerDescriptor descriptor, PipelineExecutionContext ctx);

}
