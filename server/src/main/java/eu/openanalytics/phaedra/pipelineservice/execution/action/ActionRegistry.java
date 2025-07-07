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
