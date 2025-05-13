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
package eu.openanalytics.phaedra.pipelineservice.execution.event;

import org.apache.commons.lang3.StringUtils;

public class EventDescriptor {
	
	public String topic;
	public String key;
	public String message;
	
	public static EventDescriptor of(String topic, String key, String message) {
		EventDescriptor descriptor = new EventDescriptor();
		descriptor.topic = topic;
		descriptor.key = (key == null) ? "" : key;
		descriptor.message = message;
		return descriptor;
	}
	
	@Override
	public String toString() {
		return String.format("Event [topic=%s] [key=%s]: %s", topic, key, StringUtils.abbreviate(message, 50));
	}
}