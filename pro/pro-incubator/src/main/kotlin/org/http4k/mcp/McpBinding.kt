package org.http4k.mcp

sealed interface McpBinding

class ToolBinding : McpBinding
class ResourceBinding : McpBinding
class PromptBinding : McpBinding

