package com.patbaumgartner.embabel.mcp.planner;

import com.embabel.agent.core.ToolGroup;
import com.embabel.agent.core.ToolGroupDescription;
import com.embabel.agent.tools.mcp.McpToolGroup;
import com.embabel.agent.tools.mcp.ToolCallContextMcpMetaConverter;
import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.Collections;
import java.util.List;

/**
 * Registers the "location" MCP tool group.
 *
 * All MCP tools from the configured MCP clients are exposed under the "location" role,
 * which agent actions reference via context.ai().withToolGroups("location").
 */
@Configuration
public class McpToolsConfig {

	private final List<McpSyncClient> mcpSyncClients;

	public McpToolsConfig(@Lazy List<McpSyncClient> mcpSyncClients) {
		this.mcpSyncClients = mcpSyncClients;
	}

	@Bean
	public ToolGroup locationToolGroup() {
		var description = ToolGroupDescription.create("Location and room booking tools via MCP", "location");
		return new McpToolGroup(description, "location", "MCP Location Server", Collections.emptySet(), mcpSyncClients,
				toolCallback -> true, ToolCallContextMcpMetaConverter.noOp());
	}

}
