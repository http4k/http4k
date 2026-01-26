package org.http4k.ai.mcp.protocol

enum class ClientProtocolCapability {
    RootChanged,
    Sampling,
    Experimental,
    ElicitationForm,
    ElicitationUrl,
    TaskList,
    TaskCancel,
    TaskSamplingCreateMessage,
    TaskElicitationCreate
}
