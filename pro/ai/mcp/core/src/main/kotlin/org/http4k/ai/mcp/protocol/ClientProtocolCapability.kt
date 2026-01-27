package org.http4k.ai.mcp.protocol

enum class ClientProtocolCapability {
    RootChanged,
    Sampling,
    SamplingTools,
    SamplingContext,
    Experimental,
    ElicitationForm,
    ElicitationUrl,
    TaskList,
    TaskCancel,
    TaskSamplingCreateMessage,
    TaskElicitationCreate
}
