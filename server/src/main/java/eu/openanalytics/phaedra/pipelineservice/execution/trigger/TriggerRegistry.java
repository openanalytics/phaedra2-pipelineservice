package eu.openanalytics.phaedra.pipelineservice.execution.trigger;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TriggerRegistry {

	@Autowired
	private List<ITrigger> triggerClasses;
	
	public ITrigger resolve(TriggerDescriptor descriptor) {
		return triggerClasses.stream()
				.filter(t -> t.getType().equals(descriptor.getType())).findFirst()
				.orElseThrow(() -> new IllegalArgumentException(String.format("Unsupported trigger type: %s", descriptor.getType())));
	}
}
