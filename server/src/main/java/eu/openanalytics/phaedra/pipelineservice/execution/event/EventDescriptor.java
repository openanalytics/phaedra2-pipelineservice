package eu.openanalytics.phaedra.pipelineservice.execution.event;

public class EventDescriptor {
	
	public String topic;
	public String key;
	public String message;
	
	public static EventDescriptor of(String topic, String key, String message) {
		EventDescriptor descriptor = new EventDescriptor();
		descriptor.topic = topic;
		descriptor.key = key;
		descriptor.message = message;
		return descriptor;
	}
	
	@Override
	public String toString() {
		return String.format("Event [topic=%s] [key=%s]: %s", topic, key, message);
	}
}