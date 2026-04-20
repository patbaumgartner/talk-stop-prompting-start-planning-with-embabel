package com.patbaumgartner.embabel.mcp.location;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpToolConfig {

	@Bean
	public ToolCallbackProvider locationTools(LocationService locationService) {
		return MethodToolCallbackProvider.builder().toolObjects(locationService).build();
	}

}
