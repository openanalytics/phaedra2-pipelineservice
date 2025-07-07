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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import eu.openanalytics.phaedra.pipelineservice.service.PipelineTriggerService;

@Component
public class KafkaEventListener {

	@Autowired
	private PipelineTriggerService triggerService;
	
	@KafkaListener(topicPattern = ".*", groupId = "pipeline-service")
	public void processEvent(String message, 
			@Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
			@Header(name = KafkaHeaders.RECEIVED_TOPIC) String topic) {
		
		EventDescriptor event = EventDescriptor.of(topic, key, message);
		triggerService.matchAndFire(event);
	}
}
