package eu.openanalytics.phaedra.pipelineservice.execution.action;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ActionRegistry {

	@Autowired
	private List<IAction> actionClasses;
	
	public IAction resolve(ActionDescriptor descriptor) {
		return actionClasses.stream()
				.filter(a -> a.getType().equals(descriptor.getType())).findFirst()
				.orElseThrow(() -> new IllegalArgumentException(String.format("Unsupported action type: %s", descriptor.getType())));
	}
}
