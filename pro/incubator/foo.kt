package io.a2a.schema

import org.http4k.core.Uri
import java.time.Instant

// Type aliases for domain types
typealias TaskId = String
typealias SessionId = String
typealias SkillId = String
typealias OrganizationName = String
typealias VersionId = String

// === JSON-RPC Base Structures

/**
 * Base interface for identifying JSON-RPC messages.
 */
interface JSONRPCMessageIdentifier {
    /**
     * Request identifier. Can be a string, number, or null.
     * Responses must have the same ID as the request they relate to.
     * Notifications (requests without an expected response) should omit the ID or use null.
     */
    val id: Any?
}

/**
 * Base interface for all JSON-RPC messages (Requests and Responses).
 */
interface JSONRPCMessage : JSONRPCMessageIdentifier {
    /**
     * Specifies the JSON-RPC version. Must be "2.0".
     */
    val jsonrpc: String
}

/**
 * Represents a JSON-RPC request object base structure.
 * Specific request types should extend this.
 */
interface JSONRPCRequest : JSONRPCMessage {
    /**
     * The name of the method to be invoked.
     */
    val method: String

    /**
     * Parameters for the method. Can be a structured object, an array, or null/omitted.
     * Specific request interfaces will define the exact type.
     */
    val params: Any?
}

/**
 * Represents a JSON-RPC error object.
 */
data class JSONRPCError<Data, Code>(
    /**
     * A number indicating the error type that occurred.
     */
    val code: Code,

    /**
     * A string providing a short description of the error.
     */
    val message: String,

    /**
     * Optional additional data about the error.
     */
    val data: Data? = null
)

/**
 * Represents a JSON-RPC response object.
 */
interface JSONRPCResponse<R, E> : JSONRPCMessage {
    /**
     * The result of the method invocation. Required on success.
     * Should be null if an error occurred.
     */
    val result: R?

    /**
     * An error object if an error occurred during the request. Required on failure.
     * Should be null if the request was successful.
     */
    val error: JSONRPCError<E, Int>?
}

// === Core A2A Data Structures

/**
 * Represents the state of a task within the A2A protocol.
 */
enum class TaskState {
    SUBMITTED,
    WORKING,
    INPUT_REQUIRED,
    COMPLETED,
    CANCELED,
    FAILED,
    UNKNOWN
}

/**
 * Defines the authentication schemes and credentials for an agent.
 */
data class AgentAuthentication(
    /**
     * List of supported authentication schemes.
     */
    val schemes: List<String>,

    /**
     * Credentials for authentication. Can be a string (e.g., token) or null if not required initially.
     */
    val credentials: String? = null
)

/**
 * Describes the capabilities of an agent.
 */
data class AgentCapabilities(
    /**
     * Indicates if the agent supports streaming responses.
     */
    val streaming: Boolean = false,

    /**
     * Indicates if the agent supports push notification mechanisms.
     */
    val pushNotifications: Boolean = false,

    /**
     * Indicates if the agent supports providing state transition history.
     */
    val stateTransitionHistory: Boolean = false
)

/**
 * Represents the provider or organization behind an agent.
 */
data class AgentProvider(
    /**
     * The name of the organization providing the agent.
     */
    val organization: OrganizationName,

    /**
     * URL associated with the agent provider.
     */
    val url: Uri? = null
)

/**
 * Defines a specific skill or capability offered by an agent.
 */
data class AgentSkill(
    /**
     * Unique identifier for the skill.
     */
    val id: SkillId,

    /**
     * Human-readable name of the skill.
     */
    val name: String,

    /**
     * Optional description of the skill.
     */
    val description: String? = null,

    /**
     * Optional list of tags associated with the skill for categorization.
     */
    val tags: List<String>? = null,

    /**
     * Optional list of example inputs or use cases for the skill.
     */
    val examples: List<String>? = null,

    /**
     * Optional list of input modes supported by this skill, overriding agent defaults.
     */
    val inputModes: List<String>? = null,

    /**
     * Optional list of output modes supported by this skill, overriding agent defaults.
     */
    val outputModes: List<String>? = null
)

/**
 * Represents the metadata card for an agent, describing its properties and capabilities.
 */
data class AgentCard(
    /**
     * The name of the agent.
     */
    val name: String,

    /**
     * An optional description of the agent.
     */
    val description: String? = null,

    /**
     * The base URL endpoint for interacting with the agent.
     */
    val url: Uri,

    /**
     * Information about the provider of the agent.
     */
    val provider: AgentProvider? = null,

    /**
     * The version identifier for the agent or its API.
     */
    val version: VersionId,

    /**
     * An optional URL pointing to the agent's documentation.
     */
    val documentationUrl: Uri? = null,

    /**
     * The capabilities supported by the agent.
     */
    val capabilities: AgentCapabilities,

    /**
     * Authentication details required to interact with the agent.
     */
    val authentication: AgentAuthentication? = null,

    /**
     * Default input modes supported by the agent (e.g., 'text', 'file', 'json').
     */
    val defaultInputModes: List<String> = listOf("text"),

    /**
     * Default output modes supported by the agent (e.g., 'text', 'file', 'json').
     */
    val defaultOutputModes: List<String> = listOf("text"),

    /**
     * List of specific skills offered by the agent.
     */
    val skills: List<AgentSkill>
)

/**
 * Base class for file content representation.
 */
sealed class FileContent {
    /**
     * Optional name of the file.
     */
    abstract val name: String?

    /**
     * Optional MIME type of the file content.
     */
    abstract val mimeType: String?
}

/**
 * Represents file content as Base64 encoded bytes.
 */
data class FileContentBytes(
    override val name: String? = null,
    override val mimeType: String? = null,
    val bytes: String
) : FileContent()

/**
 * Represents file content as a URI reference.
 */
data class FileContentUri(
    override val name: String? = null,
    override val mimeType: String? = null,
    val uri: Uri
) : FileContent()

/**
 * Represents a part of a message containing text content.
 */
data class TextPart(
    /**
     * The text content.
     */
    val text: String,

    /**
     * Optional metadata associated with this text part.
     */
    val metadata: Map<String, Any>? = null
) {
    val type: String = "text"
}

/**
 * Represents a part of a message containing file content.
 */
data class FilePart(
    /**
     * The file content, provided either inline or via URI.
     */
    val file: FileContent,

    /**
     * Optional metadata associated with this file part.
     */
    val metadata: Map<String, Any>? = null
) {
    val type: String = "file"
}

/**
 * Represents a part of a message containing structured data (JSON).
 */
data class DataPart(
    /**
     * The structured data content as a JSON object.
     */
    val data: Map<String, Any>,

    /**
     * Optional metadata associated with this data part.
     */
    val metadata: Map<String, Any>? = null
) {
    val type: String = "data"
}

/**
 * Represents a single part of a multi-part message. Can be text, file, or data.
 */
sealed class Part

// Implement Part subtypes
class TextPartImpl(val content: TextPart) : Part()
class FilePartImpl(val content: FilePart) : Part()
class DataPartImpl(val content: DataPart) : Part()

/**
 * Represents an artifact generated or used by a task, potentially composed of multiple parts.
 */
data class Artifact(
    /**
     * Optional name for the artifact.
     */
    val name: String? = null,

    /**
     * Optional description of the artifact.
     */
    val description: String? = null,

    /**
     * The constituent parts of the artifact.
     */
    val parts: List<Part>,

    /**
     * Optional index for ordering artifacts, especially relevant in streaming or updates.
     */
    val index: Int = 0,

    /**
     * Optional flag indicating if this artifact content should append to previous content (for streaming).
     */
    val append: Boolean? = null,

    /**
     * Optional metadata associated with the artifact.
     */
    val metadata: Map<String, Any>? = null,

    /**
     * Optional flag indicating if this is the last chunk of data for this artifact (for streaming).
     */
    val lastChunk: Boolean? = null
)

/**
 * Represents a message exchanged between a user and an agent.
 */
data class Message(
    /**
     * The role of the sender (user or agent).
     */
    val role: Role,

    /**
     * The content of the message, composed of one or more parts.
     */
    val parts: List<Part>,

    /**
     * Optional metadata associated with the message.
     */
    val metadata: Map<String, Any>? = null
)

/**
 * Enum representing the role of a message sender.
 */
enum class Role {
    USER,
    AGENT
}

/**
 * Represents the status of a task at a specific point in time.
 */
data class TaskStatus(
    /**
     * The current state of the task.
     */
    val state: TaskState,

    /**
     * An optional message associated with the current status (e.g., progress update, final response).
     */
    val message: Message? = null,

    /**
     * The timestamp when this status was recorded.
     */
    val timestamp: Instant? = null
)

/**
 * Represents a task being processed by an agent.
 */
data class Task(
    /**
     * Unique identifier for the task.
     */
    val id: TaskId,

    /**
     * Optional identifier for the session this task belongs to.
     */
    val sessionId: SessionId? = null,

    /**
     * The current status of the task.
     */
    val status: TaskStatus,

    /**
     * Optional list of artifacts associated with the task (e.g., outputs, intermediate files).
     */
    val artifacts: List<Artifact>? = null,

    /**
     * Optional metadata associated with the task.
     */
    val metadata: Map<String, Any>? = null
)

/**
 * Represents the history of messages exchanged within a task's session.
 */
data class TaskHistory(
    /**
     * List of messages in chronological order.
     */
    val messageHistory: List<Message> = emptyList()
)

/**
 * Represents a status update event for a task, typically used in streaming scenarios.
 */
data class TaskStatusUpdateEvent(
    /**
     * The ID of the task being updated.
     */
    val id: TaskId,

    /**
     * The new status of the task.
     */
    val status: TaskStatus,

    /**
     * Flag indicating if this is the final update for the task.
     */
    val final: Boolean = false,

    /**
     * Optional metadata associated with this update event.
     */
    val metadata: Map<String, Any>? = null
)

/**
 * Represents an artifact update event for a task, typically used in streaming scenarios.
 */
data class TaskArtifactUpdateEvent(
    /**
     * The ID of the task being updated.
     */
    val id: TaskId,

    /**
     * The new or updated artifact for the task.
     */
    val artifact: Artifact,

    /**
     * Flag indicating if this is the final update for the task.
     */
    val final: Boolean = false,

    /**
     * Optional metadata associated with this update event.
     */
    val metadata: Map<String, Any>? = null
)

// Alias for backward compatibility
typealias TaskUpdateEvent = TaskStatusUpdateEvent

// === Error Types (Standard and A2A)

// Error codes as constants
object ErrorCodes {
    // Standard JSON-RPC error codes
    const val PARSE_ERROR: Int = -32700
    const val INVALID_REQUEST: Int = -32600
    const val METHOD_NOT_FOUND: Int = -32601
    const val INVALID_PARAMS: Int = -32602
    const val INTERNAL_ERROR: Int = -32603

    // A2A specific error codes
    const val TASK_NOT_FOUND: Int = -32001
    const val TASK_NOT_CANCELABLE: Int = -32002
    const val PUSH_NOTIFICATION_NOT_SUPPORTED: Int = -32003
    const val UNSUPPORTED_OPERATION: Int = -32004
}

/**
 * Represents an A2A specific error.
 */
typealias A2AError = JSONRPCError<Any?, Int>

// === Push Notifications and Authentication Info

/**
 * Authentication information, potentially including additional properties beyond the standard ones.
 */
data class AuthenticationInfo(
    override val schemes: List<String>,
    override val credentials: String? = null,
    val additionalProperties: Map<String, Any> = emptyMap()
) : AgentAuthentication(schemes, credentials)

/**
 * Information required for setting up push notifications.
 */
data class PushNotificationConfig(
    /**
     * The URL endpoint where the agent should send notifications.
     */
    val url: Uri,

    /**
     * A token to be included in push notification requests for verification/authentication.
     */
    val token: String? = null,

    /**
     * Optional authentication details needed by the agent to call the notification URL.
     */
    val authentication: AuthenticationInfo? = null
)

/**
 * Represents the push notification information associated with a specific task ID.
 * Used as parameters for `tasks/pushNotification/set` and as a result type.
 */
data class TaskPushNotificationConfig(
    /**
     * The ID of the task the notification config is associated with.
     */
    val id: TaskId,

    /**
     * The push notification configuration details.
     */
    val pushNotificationConfig: PushNotificationConfig
)

// ================================================================= A2A Request Parameter Types
// =================================================================

/**
 * Parameters for the `tasks/send` method.
 */
data class TaskSendParams(
    /**
     * Unique identifier for the task being initiated or continued.
     */
    val id: TaskId,

    /**
     * Optional identifier for the session this task belongs to. If not provided, a new session might be implicitly created depending on the agent.
     */
    val sessionId: SessionId? = null,

    /**
     * The message content to send to the agent for processing.
     */
    val message: Message,

    /**
     * Optional pushNotification information for receiving notifications about this task. Requires agent capability.
     */
    val pushNotification: PushNotificationConfig? = null,

    /**
     * Optional parameter to specify how much message history to include in the response.
     */
    val historyLength: Int? = null,

    /**
     * Optional metadata associated with sending this message.
     */
    val metadata: Map<String, Any>? = null
)

/**
 * Basic parameters used for task ID operations.
 * Used by: `tasks/cancel`, `tasks/pushNotification/get`.
 */
data class TaskIdParams(
    /**
     * The unique identifier of the task.
     */
    val id: TaskId,

    /**
     * Optional metadata to include with the operation.
     */
    val metadata: Map<String, Any>? = null
)

/**
 * Parameters used for querying task-related information by ID.
 * Used by: `tasks/get`, `tasks/getHistory`, `tasks/subscribe`, `tasks/resubscribe`.
 */
data class TaskQueryParams(
    /**
     * The unique identifier of the task.
     */
    val id: TaskId,

    /**
     * Optional metadata to include with the operation.
     */
    val metadata: Map<String, Any>? = null,

    /**
     * Optional history length to retrieve for the task.
     */
    val historyLength: Int? = null
)

// === A2A Request Implementations

/**
 * Request to send a message/initiate a task.
 */
data class SendTaskRequest(
    override val id: Any?,
    override val params: TaskSendParams,
    override val jsonrpc: String = "2.0"
) : JSONRPCRequest {
    override val method: String = "tasks/send"
}

/**
 * Request to retrieve the current state of a task.
 */
data class GetTaskRequest(
    override val id: Any?,
    override val params: TaskQueryParams,
    override val jsonrpc: String = "2.0"
) : JSONRPCRequest {
    override val method: String = "tasks/get"
}

/**
 * Request to cancel a currently running task.
 */
data class CancelTaskRequest(
    override val id: Any?,
    override val params: TaskIdParams,
    override val jsonrpc: String = "2.0"
) : JSONRPCRequest {
    override val method: String = "tasks/cancel"
}

/**
 * Request to set or update the push notification config for a task.
 */
data class SetTaskPushNotificationRequest(
    override val id: Any?,
    override val params: TaskPushNotificationConfig,
    override val jsonrpc: String = "2.0"
) : JSONRPCRequest {
    override val method: String = "tasks/pushNotification/set"
}

/**
 * Request to retrieve the currently configured push notification configuration for a task.
 */
data class GetTaskPushNotificationRequest(
    override val id: Any?,
    override val params: TaskIdParams,
    override val jsonrpc: String = "2.0"
) : JSONRPCRequest {
    override val method: String = "tasks/pushNotification/get"
}

/**
 * Request to resubscribe to updates for a task after a connection interruption.
 */
data class TaskResubscriptionRequest(
    override val id: Any?,
    override val params: TaskQueryParams,
    override val jsonrpc: String = "2.0"
) : JSONRPCRequest {
    override val method: String = "tasks/resubscribe"
}

/**
 * Request to send a message/initiate a task and subscribe to streaming updates.
 */
data class SendTaskStreamingRequest(
    override val id: Any?,
    override val params: TaskSendParams,
    override val jsonrpc: String = "2.0"
) : JSONRPCRequest {
    override val method: String = "tasks/sendSubscribe"
}

// === A2A Response Implementations

/**
 * Response to a `tasks/send` request.
 */
data class SendTaskResponseImpl(
    override val id: Any?,
    override val result: Task?,
    override val error: JSONRPCError<Any?, Int>?,
    override val jsonrpc: String = "2.0"
) : JSONRPCResponse<Task, Any?>

/**
 * Response to a streaming task operation, either through `tasks/sendSubscribe` or a subscription.
 */
data class SendTaskStreamingResponseImpl(
    override val id: Any?,
    override val result: Any?, // Can be TaskStatusUpdateEvent or TaskArtifactUpdateEvent
    override val error: JSONRPCError<Any?, Int>?,
    override val jsonrpc: String = "2.0"
) : JSONRPCResponse<Any, Any?>

/**
 * Response to a `tasks/get` request.
 */
data class GetTaskResponseImpl(
    override val id: Any?,
    override val result: Task?,
    override val error: JSONRPCError<Any?, Int>?,
    override val jsonrpc: String = "2.0"
) : JSONRPCResponse<Task, Any?>

/**
 * Response to a `tasks/cancel` request.
 */
data class CancelTaskResponseImpl(
    override val id: Any?,
    override val result: Task?,
    override val error: JSONRPCError<Any?, Int>?,
    override val jsonrpc: String = "2.0"
) : JSONRPCResponse<Task, Any?>

/**
 * Response to a `tasks/getHistory` request.
 */
data class GetTaskHistoryResponseImpl(
    override val id: Any?,
    override val result: TaskHistory?,
    override val error: JSONRPCError<Any?, Int>?,
    override val jsonrpc: String = "2.0"
) : JSONRPCResponse<TaskHistory, Any?>

/**
 * Response to a `tasks/pushNotification/set` request.
 */
data class SetTaskPushNotificationResponseImpl(
    override val id: Any?,
    override val result: TaskPushNotificationConfig?,
    override val error: JSONRPCError<Any?, Int>?,
    override val jsonrpc: String = "2.0"
) : JSONRPCResponse<TaskPushNotificationConfig, Any?>

/**
 * Response to a `tasks/pushNotification/get` request.
 */
data class GetTaskPushNotificationResponseImpl(
    override val id: Any?,
    override val result: TaskPushNotificationConfig?,
    override val error: JSONRPCError<Any?, Int>?,
    override val jsonrpc: String = "2.0"
) : JSONRPCResponse<TaskPushNotificationConfig, Any?>

// === Type Aliases for A2A Responses

typealias SendTaskResponse = JSONRPCResponse<Task?, Any?>
typealias SendTaskStreamingResponse = JSONRPCResponse<Any?, Any?>
typealias GetTaskResponse = JSONRPCResponse<Task?, Any?>
typealias CancelTaskResponse = JSONRPCResponse<Task?, Any?>
typealias GetTaskHistoryResponse = JSONRPCResponse<TaskHistory?, Any?>
typealias SetTaskPushNotificationResponse = JSONRPCResponse<TaskPushNotificationConfig?, Any?>
typealias GetTaskPushNotificationResponse = JSONRPCResponse<TaskPushNotificationConfig?, Any?>

// === Union Types for A2A Requests/Responses

/**
 * Represents any valid request defined in the A2A protocol.
 */
sealed class A2ARequest : JSONRPCRequest

// Implementation classes extend A2ARequest
class SendTaskRequestImpl(val req: SendTaskRequest) : A2ARequest() {
    override val id: Any? = req.id
    override val method: String = req.method
    override val params: Any? = req.params
    override val jsonrpc: String = req.jsonrpc
}

class GetTaskRequestImpl(val req: GetTaskRequest) : A2ARequest() {
    override val id: Any? = req.id
    override val method: String = req.method
    override val params: Any? = req.params
    override val jsonrpc: String = req.jsonrpc
}

class CancelTaskRequestImpl(val req: CancelTaskRequest) : A2ARequest() {
    override val id: Any? = req.id
    override val method: String = req.method
    override val params: Any? = req.params
    override val jsonrpc: String = req.jsonrpc
}

class SetTaskPushNotificationRequestImpl(val req: SetTaskPushNotificationRequest) : A2ARequest() {
    override val id: Any? = req.id
    override val method: String = req.method
    override val params: Any? = req.params
    override val jsonrpc: String = req.jsonrpc
}

class GetTaskPushNotificationRequestImpl(val req: GetTaskPushNotificationRequest) : A2ARequest() {
    override val id: Any? = req.id
    override val method: String = req.method
    override val params: Any? = req.params
    override val jsonrpc: String = req.jsonrpc
}

class TaskResubscriptionRequestImpl(val req: TaskResubscriptionRequest) : A2ARequest() {
    override val id: Any? = req.id
    override val method: String = req.method
    override val params: Any? = req.params
    override val jsonrpc: String = req.jsonrpc
}

class SendTaskStreamingRequestImpl(val req: SendTaskStreamingRequest) : A2ARequest() {
    override val id: Any? = req.id
    override val method: String = req.method
    override val params: Any? = req.params
    override val jsonrpc: String = req.jsonrpc
}

/**
 * Represents any valid JSON-RPC response defined in the A2A protocol.
 */
sealed class A2AResponseBase<R, E> : JSONRPCResponse<R, E>

// Implementation classes for A2AResponse subtypes can be added as needed
