<h1 class="github">Changelog</h1>

This list is not intended to be all-encompassing - it will document major and breaking API 
changes with their rationale when appropriate. Given version `A.B.C.D`, breaking changes are to be expected in version number increments where changes in the `A` or `B` sections:

# FOR FUTURE RELEASES: THIS CHANGELOG HAS NOW MERGED WITH THE MAIN [HTTP4K CHANGELOG](../CHANGELOG.md). 

### v5.26.0.2
- **http4k-connect-*** - [Fix again!] Missing dev.forkhandles:parser4k dependency in http4k-connect-amazon-dynamodb
- **http4k-connect-*** - [Fix again!] Readd implementation dependencies as removed during refactor of build

### v5.26.0.1
- **http4k-connect-*** - [Fix] Missing dev.forkhandles:parser4k dependency in http4k-connect-amazon-dynamodb
- **http4k-connect-*** - [Fix] Readd implementation dependencies as removed during refactor of build

### v5.26.0.0
- **http4k-connect-*** - Upgrade dependencies
- **http4k-connect-ai-azure-fake*** - Replace Handlebars with Pebble (licence reasons)
- **http4k-connect-ai-lmstudio-fake*** - Replace Handlebars with Pebble (licence reasons)
- **http4k-connect-ai-ollama-fake*** - Replace Handlebars with Pebble (licence reasons)
- **http4k-connect-ai-openai-fake*** - Replace Handlebars with Pebble (licence reasons)
- **http4k-connect-amazon-cloudfront-fake*** - Replace Handlebars with Pebble (licence reasons)
- **http4k-connect-amazon-s3-fake*** - Replace Handlebars with Pebble (licence reasons)
- **http4k-connect-amazon-secretsmanager-fake*** - Replace Handlebars with Pebble (licence reasons)
- **http4k-connect-amazon-ses*** - Replace Handlebars with Pebble (licence reasons)
- **http4k-connect-amazon-ses-fake*** - Replace Handlebars with Pebble (licence reasons)
- **http4k-connect-amazon-sns-fake*** - Replace Handlebars with Pebble (licence reasons)
- **http4k-connect-amazon-sqs-fake*** - Replace Handlebars with Pebble (licence reasons)
- **http4k-connect-amazon-sts-fake*** - Replace Handlebars with Pebble (licence reasons)

### v5.25.1.0
- **http4k-connect-*** - Upgrade dependencies

### v5.25.0.0
- **http4k-connect-*** - Upgrade dependencies
- **http4k-connect*** : [Unlikely break] Tightened up nullable types and data class constructor visibility on various APIs

### v5.24.2.0
- **http4k-connect-*** - Upgrade dependencies
- **http4k-connect-ai-** - Chat completion API tightening

### v5.24.1.0
- **http4k-connect-ai-** - Chat completion API tightening

### v5.24.0.0
- **http4k-connect-*** - Upgrade dependencies
- **http4k-connect-ai-openai*** - [Breaking] Tightened up types for completion requests. 
- **http4k-connect-ai-azure*** - [Breaking] Tightened up types for completion requests. 
- **http4k-connect-ai-lmstudio*** - [Breaking] Tightened up types for completion requests. 
- **http4k-connect-ai-ollama*** - [Breaking] Tightened up types for completion requests. 

### v5.23.0.0
- **http4k-connect-*** - Upgrade dependencies including Kotlin to 2.0.20
- **http4k-connect-ai-azure*** - Add new operations.
- **http4k-connect-ai-anthropic*** - [New Module] Support for Anthropic AI inference.

### v5.22.0.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-*** - [Deprecations] Moved some shared types to the core module.
- **http4k-connect-ai-*** - [Breaking] Shared types have been moved to ai-core. Role.user -> Role.Companion.User
- **http4k-connect-ai-azure*** - [New Module] Support for Azure AI inference, and GitHub Models support for prototyping.

### v5.21.0.0
- **http4k-connect-*** - Upgrade dependencies including Kotlin to 2.0.10
- **http4k-connect-core** - [Deprecation] Rename of `Http4kConnectAdapter` to `Http4kConnectClient`
- **http4k-connect-ai-openai-**** - [Breaking] Model ResponseFormat as a sealed class hierarchy. Removed ResponseFormatType as now inherent in the JSON marshalling. Alpha support for `json_schema` response format, but it's just a map right now with no class structure.

### v5.20.0.0
- **http4k-connect-*** - Upgrade dependencies, including Kotshi to 3.0.0. Version bump to highlight that this could be a breaking change if you are using the Kotshi annotation processor.
- **http4k-connect-amazon-s3=*** - Add CommonPrefixes field to S3 ListObjectsV2 response. H/T @kwydler

### v5.19.0.2
- **http4k-connect-*** - [Fix] Add missing @JsonSerializable annotation to ReceiveMessage action

### v5.19.0.1
- **http4k-connect-*** - [Fix] Add missing Kotshi adapter to Core Moshi adapter factory.

### v5.19.0.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-*** - [Breaking] http4k has introduced a breaking change in 5.26.0.0. If you are using the typesafe
  configuration environment, you will need to update your code to use the repackaged `Environment` classes - these are
  now in `org.http4k.config` instead of `org.http4k.cloudnative.env`. Just updating your imports should be sufficient to
  fix this.

### v5.18.0.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-amazon-cognito*** - [Breaking] AWS Cognito: Add support for server side authentication (
  AdminInitiateAuth and AdminRespondToAuthChallenge). H/T @markth0mas
- **http4k-connect-ai-**** - [Breaking] Repackaged `ModelName` to common location. Just update imports!
- **http4k-connect-ai-langchain** -  [Breaking] Added support for LmStudio chat and embedding models. Break is
  renamed: `ChatModelOptions` to `OpenAiChatModelOptions`.
- **http4k-connect-ai-lmstudio*** - [New module!] LmStudio client module and fake so you can connect to a locally
  running LLM server running any model.

### v5.17.1.1
- **http4k-connect-amazon-sqs-*** - [Fix] Type of SQS ReceiveMessage waitTimeSeconds parameter was incorrect. H/T @oharaandrew314

### v5.17.1.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-amazon-s3-*** - Support all storage classes, including the restore lifecycle for glacier. H/T @oharaandrew314
- **http4k-connect-amazon-s3-*** - Support S3 object tagging. H/T @oharaandrew314

### v5.17.0.2
- **http4k-connect-ai-openai** - Choices are not optional in conversation completion.
- **http4k-connect-ai-langchain** - Fix streaming for OpenAiChatLanguageModel. 

### v5.17.0.1
- **http4k-connect-ai-langchain** - Added support for System messages in Ollama models

### v5.17.0.0 
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-ai-*** - Migration of the various AI packages (OpenAI/Langchain) to `http4k-ai-` subpackage name. 
- **http4k-connect-ai-openai** - [Breaking] Use FloatArray for embeddings instead of `List<Float>`
- **http4k-connect-ai-ollama*** - [New module!] Ollama client module so you can use Http4k-connect clients in
  LangChain apps.
- **http4k-connect-ai-langchain** - Added support for Ollama models

### v5.16.0.2
- **http4k-connect-ai-langchain** - Properly support all message types in OpenAI Client.

### v5.16.0.1
- **http4k-connect-langchain** - Tools requests cannot be empty for chat completions in OpenAI Client.

### v5.16.0.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-langchain** - [New module!] LangChain adapter module so you can use Http4k-connect clients in LangChain apps. Currently only OpenAI is supported.
- **http4k-connect-openai-*** - [Breaking] Support tool calls and more modern API version for ChatCompletion.

### v5.15.0.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-amazon-sqs-** - [Possible break] Implement JSON version of SQS in both fake and client. Ensure you are using an up-to-date version of the AWS SDK (which will support the JSON message format). Massive H/T @oharaandrew314

### v5.14.0.0
- **http4k-connect-*** - Upgrade dependencies, including Kotlin to V2!

### v5.13.0.0
- **http4k-connect-*** - Upgrade dependencies, including Kotlin to 1.9.24
- **http4k-connect-amazon-dynamodb*** - Add StreamsEventResponse model. H/T @charlee-dev
- **http4k-connect-evidently*** - [Fix #405] Properly parse evidently project and features names from ARN. H/T @oharaandrew314

### v5.12.2.0
- **http4k-connect-*** - Upgrade dependencies

### v5.12.1.0
- **http4k-connect-*** - Upgrade dependencies

### v5.12.0.0
- **http4k-connect-*** - Upgrade dependencies, and api changes to support new http4k version.

### v5.11.0.0
- **http4k-connect-*** - Upgrade dependencies
- **http4k-connect-*** - [Breaking] Reordering of the parameters in client constructors to put `overrideEndpoint` at the end of the list, since it is the least commonly used. To fix, just reorder your parameters.

### v5.10.1.0
- **http4k-connect-*** - Upgrade dependencies
- **http4k-connect-amazon-dynamodb*** - Align secondary index constructors in DynamoDbTableMapperSchema. H/T @obecker
- **http4k-connect-amazon-dynamodb-fake** - Validate reserved words in DynamoDB condition expressions. H/T @oharaandrew314

### v5.10.0.0
- **http4k-connect-*** - Upgrade dependencies
- **http4k-connect-amazon-dynamodb** - [Breaking] `DynamoDbIndexMapper` now supports custom projections. Closes #391. H/T @oharaandrew314

### v5.9.0.0
- **http4k-connect-amazon-dynamodb** - [Breaking] `keyCondition` in query DSL no longer accepts arbitrary attributes. Fixes #380. H/T @obecker

### v5.8.0.0
- **http4k-connect-*** - Upgrade dependencies, including Kotlin to 1.9.23
- **http4k-connect-amazon-dynamodb** - [Breaking] `ExclusiveStartKey` in `DynamoDbIndexMapper` functions is now an unconstrained `Key`. Fixes #372. H/T @oharaandrew314

### v5.7.0.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-amazon-dynamodb** - Query builder for DynamoDB. H/T @obecker
- **http4k-connect-amazon-dynamodb** - [Breaking] `Query` and `Scan` `Select` field is enum instead of String. To fix, just replace the hardcoded string with the enum! H/T @obecker
- **http4k-connect-amazon-kms** - Support for KeySpec property. H/T @oharaandrew314

### v5.6.15.0
- **http4k-connect-storage-*** StoragePropertyBag allows storage-backed dynamic backing Read/WriteProperties. Extend `StoragePropertyBag` and declare typed properties with `item<TYPE>()`. Properties are stored into the backing storage using standard automarshalling. 

### v5.6.14.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-*** - Add optional endpoint parameter to all AWS service HTTP implementations. H/T @obecker

### v5.6.13.0
- **http4k-connect-*** - Upgrade dependencies.

### v5.6.12.0
- **http4k-connect-*** - Upgrade dependencies.

### v5.6.11.0
- **http4k-connect-amazon-core** - SSO credentials provider now caches credentials until they expire. Stops re-login

### v5.6.10.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-amazon-core** - Simpler API for retrieving the AWS credentials of a custom profile. H/T @obecker
- **http4k-connect-amazon-s3*** - Add parameter to force path-style requests to S3 buckets. H/T @obecker

### v5.6.9.0
- **http4k-connect-amazon-apprunner*** - [New module] Client and fake

### v5.6.8.2
- **http4k-connect-amazon-containercredentials*** - Add Kotshi adapter to Moshi instance.

### v5.6.8.1
- **http4k-connect-*** - Fix AutoMarshalledPageAction not recognising arrays with whitespace.

### v5.6.8.0
- **http4k-connect-*** - Upgrade dependencies.

### v5.6.7.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-amazon-eventbridge*** Events can be sent with the ARN and not just with the EventBusName

### v5.6.6.0
- **http4k-connect-amazon-dynamodb-client*** - [Fix] #344 Handle failures in `DynamoDbTableMapper.delete()` H/T @obecker
- **http4k-connect-amazon-evidently*** Add `updateFeature` to Evidently.  H/T @oharaandrew314

### v5.6.5.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-*** - Fix `AutomarshalledPagedAction` so that it deals with pages of results which do not get returned inside a list but in an object wrapping a list.

### v5.6.4.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-amazon-**** - Added Amazon Presigner for creating pre-signed requests. H/T @oharaandrew314
- **http4k-connect-amazon-dynamodb-fake*** - [Fix] #327 Query algorithm is slight wrong in fake dynamo. H/T @oharaandrew314

### v5.6.3.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-amazon-systemsmanager-fake*** - [Fix] #339 - Fake Systems Manager does not overwrite parameters - returns 400

### v5.6.2.0
- **http4k-connect-*** - Upgrade dependencies.

### v5.6.1.0
- **http4k-connect-*** - Upgrade dependencies.

### v5.6.0.0
- **http4k-connect-*** - Upgrade dependencies, including Kotlin to 1.9.21
- **http4k-connect-amazon-dynamodb-fake** - Add support for sparse indexes. H/T @obecker
- **http4k-connect-openai** [Fix] Optional fields in `getModels` call

### v5.5.1.0
- **http4k-connect-*** - Upgrade dependencies
- **http4k-connect-gitlab** - [New module] Basic client module

### v5.5.0.1
- **http4k-connect-amazon-eventbridge** - Support newline characters inside JSON

### v5.5.0.0
- **http4k-connect-*** - Upgrade dependencies
- **http4k-connect-amazon-iamidentitycenter** - [Breaking] Browser infra moved to core. Simple reimport to fix.

### v5.4.0.0
- **http4k-connect-*** - Upgrade dependencies, including Kotlin to 1.9.20.

### v5.3.0.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-*** - [Breaking - dev] http4k-connect is now built with Java 21.
- **http4k-connect-amazon-iamidentitycenter** - [New module] Client and fake implementation, plus interactive SSO login via a browser.

### v5.2.5.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-*** - Generated extension functions create defaults for collections types when they are defaulted in the core Action.

### v5.2.4.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-*** - Prevent extra dependencies being published in maven artefacts.

### v5.2.3.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-amazon-dynamodb-fake** Support for TransactWriteItems

### v5.2.2.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-amazon-dynamodb** Present a more concise introduction to the DynamoDB table mapper. H/T @oharaandrew314
- **http4k-connect-amazon-cloudwatchlogs** - [New module] Client and fake implementation.

### v5.2.0.0
- **http4k-connect-*** - Upgrade dependencies including Kotlin to 1.9.10.
- **http4k-connect-openai** [Breaking change] Added support for Streaming version of ChatCPT completions to both library and fake implementation.
- **http4k-connect-amazon-evidently** [New module] Add support for this feature flagging service. H/T @oharaandrew314

### v5.1.7.0
- **http4k-connect-amazon-dynamodb-fake** Add support for the entire update expression syntax. H/T @oharaandrew314

### v5.1.6.2
- **http4k-connect-amazon-eventbridge-fake*** - [Fix] Add proper indexing to fake when sending events.

### v5.1.6.1
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-amazon-firehose*** - [Fix] Serialize DeliveryStreamName correctly in Moshi.

### v5.1.6.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-**** - Make generation of IDs more deterministic

### v5.1.5.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-cognito-**** - Add ConfirmForgottenPassword. H/T @dmcg

### v5.1.4.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-amazon-sqs** - Add `DeleteMessageBatch` action.  H/T @oharaandrew314
- **http4k-connect-amazon-containercredentials** - Add container credentials chain @oharaandrew314
- **http4k-connect-amazon-cognito** - Moshi config was incorrect for successful user password auth response H/T @time4tea

### v5.1.3.0
- **http4k-connect-*** - Upgrade dependencies, including Kotlin to 1.9.0.
- **http4k-connect-amazon-dynamodb** - Add scanPage and queryPage operations to DynamoDb table mapper. Pagination can now be controlled by the caller. H/T @oharaandrew314
- **http4k-connect-amazon-dynamodb-fake** - putItem now supports a `ConditionExpression`.
- **http4k-connect-amazon-dynamodb-fake** - [Fix] query and scan will now return the correct LastEvaluatedKey based on the current index. H/T @oharaandrew314
- **http4k-connect-amazon-dynamodb-fake** - [Fix] Condition Expressions now support name substitutions in the `attribute_exists` and `attribute_not_exists` functions
- **http4k-connect-amazon-containercredentials** - [Fix] Handle ARN NOT_SUPPLIED when getting aws credentials and running on AWS AppRunner

### v5.1.2.0
- **http4k-connect-amazon-eventbridge** - [New module] Client and fake implementation.

### v5.1.1.0
- **http4k-connect-amazon-firehose** - [New module] Client and fake implementation.

### v5.1.0.0
- **http4k-connect-*** - Upgrade dependencies, including Kotlin to 1.9.0.
- **http4k-connect-amazon-kms-fake** - Will now generate unique key pairs for each CMK. H/T @oharaandrew314
- **http4k-connect-amazon-kms-fake** - [Fix] Getting the public key for an ECDSA CMK will now work as expected. H/T @oharaandrew314

### v5.0.1.0
- **http4k-connect-*** - Upgrade dependencies

### v5.0.0.0
- **http4k-connect*** : Upgrade to http4k platform v5 version.
- **http4k-connect*** : [Breaking] Remove all previous deprecations from all modules for v4. To upgrade cleanly, first upgrade to `v3.43.0.0` and then re-upgrade to `v5.0.0.0`. This will ensure that you only have to deal with Deprecations between the major versions.
- **http4k-connect-kapt-generator** : [Breaking] This generator module has been removed due to the replacement of Kapt with KSP. To fix, migrate to use the KSP gradle plugin with the `http4k-connect-ksp-generator` module instead. There are no more changes required as it is a drop-in replacement.

### v3.43.1.0
- **http4k-connect-*** - Upgrade dependencies
- **http4k-connect-*** - [Fix] Code generation for pagination was broken when using AutomarshalledPagedAction

### v3.43.0.0
- **http4k-connect-*** - Upgrade dependencies, including Kotlin to 1.8.22

### v3.42.1.0
- **http4k-connect-openai-fake** - Support for NoAuth plugin installation.

### v3.42.0.0
- **http4k-connect-openai-plugin** - [New module] OpenAI plugin development SDK. http4k-connect provides APIs to create plugins for all 3 plugin authorization types - User, Service and OAuth.
- **http4k-connect-openai-fake** - Plugins can now be installed into the `FakeOpenAI` server. All 3 plugin auth types
are supported. 

### v3.41.0.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-openai** - Properly support OAuth plugin types

### v3.40.5.0
- **http4k-connect-*** - Upgrade dependencies.

### v3.40.4.0
- **http4k-connect-openai** - Small fixes.

### v3.40.3.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-openai** - Add CreateEmbeddings call.

### v3.40.2.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-amazon-kms-fake*** - [Fix] Encryption.decryption works with binary messages

### v3.40.1.2
- **http4k-connect-kakfa-rest*** - Correct content type and trimming string for producing records to Kafka v3.

### v3.40.1.1
- **http4k-connect-kakfa-rest*** - Add `produceRecordsWithPartitions()` for production and partitioning in V3 API.

### v3.40.0.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-*** - Support for multiple action types in a single module.
- **http4k-connect-*** - [Deprecation] Action types have been moved to super-package. Custom Actions will need to be updated.
- **http4k-connect-kakfa-rest*** - [Breaking] Rearrangement of action modules and start to support V3 endpoints.

### v3.39.2.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-amazon-dynamodb** - Helper methods for creating sets and lists of value types.

### v3.39.1.0
- **http4k-connect-openai** - Set a sensible limit on the number of max tokens in chat completions.
- **http4k-connect-openai** - [New module] Client and fake.

### v3.39.0.1
- **http4k-connect-kakfa-schemaregistry*** - [Fix] Don't break on registering the same schema.

### v3.39.0.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-mattermost*** - [New module] Initial support for a couple of actions. H/T @tkint
- **http4k-connect-kakfa-schemaregistry*** - [Breaking] Added some actions and tightened up types. Breaks are purely primitive -> ValueType.

### v3.38.1.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-*** - New release process.

### v3.38.0.1
- **http4k-connect-*** - Add missing JsonSerializable annotation

### v3.38.0.0
- **http4k-connect-*** - Upgrade dependencies.

### v3.37.1.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-amazon-dynamo-fake** - Dynamodb query scan pagination. H/T @oharaandrew314

### v3.37.0.1
- **http4k-connect-amazon-dynamo*** - Fixed `copy()` so that it does not stop on first item.
- 
### v3.37.0.0
- **http4k-connect-*** - Upgrade dependencies, including Kotlin to 1.8.20
- **http4k-connect-amazon-dynamo*** - Added `copy()` operation.

### v3.36.0.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-kms-fake-*** - Real fake keys are now used for signing and verifying bytes.

### v3.35.0.0
- **http4k-connect-*** - Upgrade dependencies.

### v3.34.0.1
- **http4k-connect-*** - Fixed pagination to stop when a failure is encountered.

### v3.34.0.0
- **http4k-connect-github** - Better name for GitHub webhook events  

### v3.33.4.0
- **http4k-connect-amazon-secretsmanager-*** - Support for ARNs in FakeSecretsManager and in API.

### v3.33.3.0
- **http4k-connect-kakfa-schemaregistry*** - Changes to register schema version API contract.

### v3.33.2.0
- **http4k-connect-kakfa-rest*** - Avro records can have non-Avro keys.

### v3.33.1.0
- **http4k-connect-kakfa-schemaregistry*** - Changes to register schema version API contract.

### v3.33.0.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-kakfa-schemaregistry*** - [New module] Client and fake.

### v3.32.0.0
- **http4k-connect-kakfa-rest*** - Add helpers for consuming and producing.

### v3.31.1.0
- **http4k-connect-kakfa-rest*** - Adding message partitioning strategies.

### v3.31.0.0
- **http4k-connect-kakfa-rest*** - [Break] Support for Avro message and Schema marshalling. Rework API for ease of use

### v3.30.0.0
- **http4k-connect-kakfa-rest*** - [Rename Module] Fixes to binary message formats and auto-commit. Add Seeking for offsets.

### v3.29.1.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-kakfa-http-proxy*** - [New module] For sending messages to Kafka without the need for the entire Kafka broker infrastructure.

### v3.29.0.0
- **http4k-connect-*** - Upgrade dependencies.

### v3.28.0.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-sns-fake*** [Breaking] Change SNSMessage to include subject and attributes

### v3.27.1.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-ksp-generator*** - Generation of clients is now done via KSP instead of KAPT.

### v3.27.0.2
- **http4k-connect-ksp-generator*** - Support for Object action classes.

### v3.27.0.1
- **http4k-connect-amazon-containercredentials** - [Fix] AWS_CONTAINER_AUTHORIZATION_TOKEN is optional. 

### v3.27.0.0
- **http4k-connect-*** - Upgrade dependencies, including Kotlin to 1.8.0
- **http4k-connect-ksp-generator*** - [New module] A version of Action and Client code generator written using KSP.
- **http4k-connect-amazon-instancemetadata** - Add Amazon RegionProvider with environment, profile, and imds support. H/T @oharaandrew314

### v3.26.4.0
- **http4k-connect-*** - Upgrade dependencies
- **http4k-connect-amazon-instancemetadata** - [New module]  Query metadata and credentials from the current Amazon EC2 environment. H/T @oharaandrew314
- **http4k-connect-amazon-ec2credentials** - Deprecated.  Use the **http4k-connect-amazon-instancemetadata** module

### v3.26.3.0
- **http4k-connect-cognito** - We now generate action code using Kapt, as per the other clients.
- **http4k-connect-cognito-fake** - Fixes to login page.

### v3.26.2.0
- **http4k-connect-*** - Upgrade dependencies
- **http4k-connect-amazon-dynamodb** - Add support for DynamoDb ImportTable and related actions. H/T @alex859

### v3.26.1.0
- **http4k-connect-amazon-dynamodb** - Add tableMapper batchGet and batchDelete operations. H/T @oharaandrew314
- **http4k-connect-cognito-fake** - Enforce matching of Client Secret as well as ClientId 
- **http4k-connect-cognito-fake** - Make Fake support OIDC token endpoint parameters (client_credentials_basic)

### v3.26.0.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-cognito*** - Implement OAuth server with JWT signing and Well Known endpoint.

### v3.25.5.0
- **http4k-connect-amazon-containercredentials** - Support passing of full CC URL and auth token header. This makes us compatible with AWS Snapstart
- **http4k-connect-storage-http** - Replace Swagger UI implementation.
- **http4k-connect-*** - Upgrade dependencies.

### v3.25.4.0
- **http4k-connect-*** - Upgrade dependencies.

### v3.25.3.0
- **http4k-connect-*** - Upgrade dependencies.

### v3.25.2.0
- **http4k-connect-*** - Upgrade dependencies.

### v3.25.1.0
- **http4k-connect-*** - Upgrade dependencies.

### v3.25.0.0
- **http4k-connect-*** - Upgrade dependencies.

### v3.24.0.0
- **http4k-connect-google-analytics*** - [New module] Split Google Analytics clients to support UA and GA4.

### v3.23.2.0
- **http4k-connect-*** - Upgrade dependencies. 

### v3.23.1.0
- **http4k-connect-*** - Upgrade dependencies. 

### v3.23.0.0
- **http4k-connect-*** - Upgrade dependencies. 

### v3.22.2.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-amazon-dynamo*** - Added some useful methods for mapping Attributes such as `Attribute.map(BiDiMapping)` and `Attribute.list(BiDiMapping)`

### v3.22.1.0
- **http4k-connect-*** - Upgrade dependencies.

### v3.22.0.0
- **http4k-connect-*** - Upgrade dependencies, including Kotlin to 1.7.20.

### v3.21.3.1
- **http4k-connect-*** - Republish of 3.21.3.0.

### v3.21.3.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-core** - Tweak to ProfileCredentialsProvider. H/T @oharaandrew314

### v3.21.2.1
- **http4k-connect-*** - Republish of 3.21.2.0.

### v3.21.2.0
- **http4k-connect-*** - Upgrade dependencies.

### v3.21.1.0
- **http4k-connect-*** - Upgrade dependencies.

### v3.21.0.0
- **http4k-connect-amazon-sns-fake*** - [Unlikely Break] FakeSNS now works on a single region only - so published messages need to match the region which the Fake was created with.

### v3.20.0.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-amazon-sns-fake*** - [Breaking] Enable setting of region - this now defaults to the test region `ldn-north-1`, but you can override if you need to do so in your testing environment.

### v3.19.2.0
- **http4k-connect-*** - Upgrade dependencies.

### v3.19.1.0
- **http4k-connect-*** - Upgrade dependencies.

### v3.19.0.0
- **http4k-connect-amazon-ses** - Allow both text and html body on emails.

### v3.18.1.3
- **http4k-connect-amazon-dynamodb-fake** - Fix batchWriteItem with multiple requests per table. H/T @oharaandrew314

### v3.18.1.2
- **http4k-connect-amazon-dynamodb-fake** - Fix request/response for fake dynamodb batch operations. H/T @oharaandrew314

### v3.18.1.1
- **http4k-connect-amazon-dynamodb-fake** - `PutItem` now only replaces items with the correct Primary Key. H/T @oharaandrew314

### v3.18.1.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-*** - Added convenience automarshalling for pagination.
- **http4k-connect-amazon-dynamodb-fake** - New actions supported: `Query`, `Scan`, `Updateitem`. H/T @oharaandrew314 for the contributions.

### v3.18.0.0
- **http4k-connect-*** - Upgrade dependencies, including Kotlin to 1.7.0.

### v3.17.3.1
- **http4k-connect-*** - Fix broken POMs which removed all runtime dependencies

### v3.17.3.0
- **http4k-connect-*** - Upgrade dependencies.

### v3.17.2.0
- **http4k-connect-*** - Upgrade dependencies, including Kotlin to 1.6.21.
- **http4k-connect-amazon-dynamodb-** - Add auto Lens extensions for object mapping: `Moshi.autoDynamoLens<AnObject>()`

### v3.17.1.0
- **http4k-connect-*** - Upgrade dependencies, including Kotlin to 1.6.20.

### v3.17.0.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-amazon-containercredentials** Added loading of credentials from Container Credentials service.
- **http4k-connect-amazon-containercredentials-fake** - [New module] Fake for the above.
- **http4k-connect-amazon-sts-*** - [Unlikely break] Repackaged Credentials to core.

### v3.16.6.0
- **http4k-connect-*** - Upgrade dependencies.

### v3.16.5.0
- **http4k-connect-*** - Upgrade dependencies.

### v3.16.4.0
- **http4k-connect-amazon-kms*** - EncryptionAlgorithms in GetPublicKey is optional (but not according to the AWS docs... )

### v3.16.3.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-amazon-sts** - WebIdentityToken provider refreshes token from disc on re-auth.

### v3.16.2.0
- **http4k-connect-*** - Upgrade dependencies.

### v3.16.1.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-amazon-*** - Add missing exception message in the case of remote failure.

### v3.16.0.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-storage-jdbc*** - [Possible break] Due to upgrade of Exposed and H2SQL - API differences. Need to fix as required.
- **http4k-connect-amazon.dynamodb** - Support defaulted() for falling back to another column.

### v3.15.2.1
- **http4k-connect-amazon-*** - AssumeRoleWithWebIdentity response has optional fields which are not documented. Grrrr.

### v3.15.2.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-amazon-*** - Fix #105 - WebIdentityProvider to STS does not set host name when refreshing token. 

### v3.15.1.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-amazon.dynamodb** - [Fix] ConsumedCapacity is not a long

### v3.15.0.0
- **http4k-connect-*** - [Breaking] Upgrade dependencies. The http4k upgrade must be done in lockstep with this version as there has been a breaking change in http4k.

### v3.14.0.0
- **http4k-connect-*** - [Break] Upgrade of Forkhandles to v2.0.0.0 means some unfortunately exposed constructor methods have gone away. Simple to fix - deprecation warnings will take care of it.
- **http4k-connect-*** - Upgrade dependencies and Kotlin to 1.6.10.

### v3.13.1.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-storage-redis-*** - Vary lifetime of items.

### v3.13.0.0
- **http4k-connect-*** - Upgrade dependencies and Kotlin to 1.6.0.
- **http4k-connect-*** - [Break] Changes to Kotshi mean that JsonFactories are now interfaces instead of abstract classes.

### v3.12.2.0
- **http4k-connect-*** - Upgrade dependencies and Gradle.

### v3.12.1.1
- **http4k-connect-*** - Fix Base64 decoding of ByteArrays(roundtripping).

### v3.12.1.0
- **http4k-connect-amazon-dynamodb*** - Add defaulted() to Attribute Lenses

### v3.12.0.0
- **http4k-connect-amazon-dynamodb*** - [Breaking] Fix TransactionGetItems to not blow up if item missing. Item is now nullable in response.

### v3.11.2.0
- **http4k-connect-*** - Upgrade dependencies

### v3.11.1.0
- **http4k-connect-*** - Upgrade dependencies
- **http4k-amazon-ses*** - [New module] client and Fake for SES. @H/T ToastShaman

### v3.11.0.1
- **http4k-connect-*** - Upgrade dependencies
- **http4k-connect-github** - [Break] Replace Secret with GitHubToken in rest of API.

### v3.10.0.1
- **http4k-connect-amazon-sqs-fake** - [Fix] Calculated attribute MD5 was incorrect. 

### v3.10.0.0
- **http4k-connect-amazon-sqs** - [Breaking] Parsing of message attributes in ReceiveMessage is implemented. SQS docs are wrong... 

### v3.9.0.0
- **http4k-connect-*** - Upgrade dependencies
- **http4k-connect-github** - [Break] Replace Secret with GitHubToken in filters.

### v3.8.3.0
- **http4k-connect-*** - Upgrade dependencies
- **http4k-connect-amazon-sqs-*** - Support ListQueues.

### v3.8.2.0
- **http4k-connect-*** - Upgrade dependencies
- **http4k-connect-github** - Fixing up to make 404s possible in the GitHub action.

### v3.8.1.1
- **http4k-connect-amazon-sqs-fake** - Make MD5 of SQS messages pad right to 32 chars.

### v3.8.1.0
- **http4k-connect-*** - Upgrade dependencies, including Kotlin to 1.5.30.
- **http4k-connect-amazon-sts*** - Added convenience functions for Credential Providers.

### v3.8.0.0
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-amazon-sts*** - Support STS (refreshing) Credential providers including by WebIdentityToken.

### v3.7.0.0
- **http4k-connect-amazon-sqs*** - [Breaking] Change to use QueueUrl universally. This ia much more consistent and aligns with the behaviour of the standard AWS SDK. You will need to update your configurations to pass in the urls instead of the standard queue names/ARNs

### v3.6.4.0
- **http4k-connect-amazon-sqs*** - Support GetQueueAttributes

### v3.6.3.2
- **http4k-connect-*** - Upgrade dependencies.
- **http4k-connect-amazon-sqs*** - Fix ReceiveMessage to correctly return requested number of messages.

### v3.6.3.1
- **http4k-connect-amazon-sqs*** - Fix ReceiveMessage to correctly handle multiple messages.

### v3.6.3.0
- **http4k-connect-*** - Upgrade dependencies, including http4k to 4.11.0.1.
- **http4k-connect-amazon-sqs*** - Support for WaitTimeSeconds when receiving messages.

### v3.6.2.0
- **http4k-connect-amazon-s3*** - Add `HeadBucket` and `HeadKey`
- **http4k-connect-*** - Upgrade dependencies, including http4k to 4.10.1.0.

### v3.6.1.0
- **http4k-connect-*** - Upgrade dependencies, including http4k to 4.10.0.1.

### v3.6.0.0
- **http4k-connect-amazon-s3-fake*** - Fix FakeS3 CopyKey command. H/T @ToastShaman
- **http4k-connect-*** - Upgrade dependencies, including http4k to 4.9.10.0.

### v3.5.1.0
- **http4k-connect-*** - Upgrade dependencies, including http4k to 4.9.9.0.
- **http4k-connect-github** : Fix token usage and add `authScheme` parameter.

### v3.5.0.0
- **http4k-connect-*** - Upgrade dependencies, including http4k to 4.9.8.0.
- **http4k-connect-*-fake** [Breaking] Replaced usage of `ChaosFake` with the `ChaoticHttpHandler` from http4k. Nothing massive, but you may need to update some imports as they have moved

### v3.4.2.0
- **http4k-connect-*** - Upgrade dependencies, including http4k to 4.9.7.0.
- **http4k-connect-amazon-s3-fake** - Fix #56 - S3(Fake): preserve encoding in bucketGetKey - H/T @tkint 

### v3.4.1.0
- **http4k-connect-google-analytics** - [New module] Added support for GA events.

### v3.4.0.0
- **http4k-connect-amazon-*** - Region is now not reliant on default AWS format. This helps with on-prem installations with non-standard region format.
- **http4k-connect-google-analytics** - [Breaking] Moved Tracking ID out of pageView and into client as is global.

### v3.3.3.0
- **http4k-connect-*** - Upgrade dependencies, including http4k to 4.9.5.0.

### v3.3.2.0
- **http4k-connect-*** - Upgrade dependencies, including http4k to 4.9.3.1.

### v3.3.1.0
- **http4k-connect-*** - Upgrade dependencies, including http4k to 4.9.1.0.

### v3.3.0.0
- **http4k-connect-*** - Upgrade dependencies, including http4k to 4.9.0.2.
- **http4k-connect-amazon-lambda** : Introduction of `invokeStreamFunction()` action to allow for calling functions without.

### v3.2.0.1
- **http4k-connect-amazon-s3** : Fix S3 not returning LastModified value correctly in `ListObjectsV2`

### v3.2.0.0
- **http4k-connect-*** - Upgrade dependencies
- **http4k-connect-amazon-dynamodb** : [Slight break] `BatchGetItem` and `BatchWriteItem` actions had incorrect key names for response classes.

### v3.1.1.0
- **http4k-connect-amazon-cognito** - [New module] Base actions for user client and pool creation are implemented, no fake as yet.

### v3.1.0.1
- **http4k-connect-amazon-dynamodb** : Removed non-nullable field on ConsumedCapacity.

### v3.1.0.0
- **http4k-connect-amazon-s3*** : Add support for path-based bucket operations (ie. buckets with `.` in the name)
- **http4k-connect-amazon-s3*** : [Rename break] Renamed `*Key` actions to match S3 API (now `*Object`)
- **http4k-connect-amazon-s3*** : [Slight break] Add headers to `PutObject`.

### v3.0.3.0
- **http4k-connect-*** : Add convenience functions for getting AWS environmental variables from an http4k Environment object.

### v3.0.2.0
- **http4k-connect-*** : Upgrade http4k.

### v3.0.1.0
- **http4k-connect-*** : Add Moshi serializers for enums, making them compatible with GraalVM

### v3.0.0.0
- **http4k-connect-*** : Major repackage of all model classes. Model package has been normalised to `org.http4k.connect.amazon.<system>.model`. All non-top level message objects have been moved from the `org.http4k.connect.amazon.<system>.action` package into `org.http4k.connect.amazon.<system>.model`. This is probably very annoying, and apologies in advance - hence the major version uptick. We are not proud of ourselves, but it needed to be done for our future plans... Also imports of generated client methods may need to be altered as some of them were in teh wrong place.

### v2.23.0.0
- **http4k-connect-amazon-dynamodb** : [Slight break] Repackaging work of item types to reuse them for Dynamo event marshalling.

### v2.22.1.0
- **http4k-connect-amazon-dynamodb** : Support Dynamo Events in marshalling layer.

### v2.22.0.1
- **http4k-connect-amazon-dynamodb** : Fix incorrectly specified data type for OffsetDateTime attributes.

### v2.22.0.0
- **http4k-connect-amazon-dynamodb** : [Breaking] Change `value()` method on `Attribute` to be typed. This only affects you if you are using `values4k` value classes for column mappings.

### v2.21.1.1
- **http4k-connect-amazon-dynamodb** : Fix long value stored as a string.

### v2.21.1.0
- **http4k-connect-*** : Add default values to all nullable response message fields. This is better for when stubbing/mocking out the responses.

### v2.21.0.0
- **http4k-connect-*** : [Breaking] Repackaged Pagination classes (not just Amazon anymore).
- **http4k-connect-*** : [Breaking] Added pagination of results to relevant actions using `xyzPaginated()` actions. Removed usage of `Listing` classes. This is a more convenient API to use and is consistent throughout all modules.

### v2.21.1.1
- **http4k-connect-amazon-dynamodb** : Fix bug with Long data type. @H/T @ToastShaman for the tip off.

### v2.20.1.0
- **http4k-connect-amazon-dynamodb** : Added pagination of results

### v2.20.0.0
- **http4k-connect-amazon-dynamodb** : More making API nicer and typesafe.

### v2.19.0.0
- **http4k-connect-amazon-*** : [Breaking] Changed generated helper functions to not interfere with the names of the parameters. Simple rename will work here.
- **http4k-connect-*** : Friendlify JavaDocs.

### v2.18.1.0
- **http4k-connect-amazon-cloudfront** : [New module]
- **http4k-connect-amazon-cloudfront-fake* : [New module]

### v2.18.0.0
- **http4k-connect-amazon-dynamodb** : Further tweaking of the Item and Key mapping typealiases to make API easier to use.

### v2.17.0.0
- **http4k-connect-amazon-dynamodb** : Reworked DynamoDb API to be typesafe, tightened up types in responses, added Scan.

### v2.16.0.0
- **http4k-connect-amazon-dynamodb** : [New module] New client module. No fake as yet.
- **http4k-connect-amazon-*** : [Break] Rename `Base64Blob.encoded()` -> `Base64Blob.encode()` for clarity.

### v2.15.4.0
- **http4k-connect-github** : Add infra for main GitHub client. No custom actions implemented yet.

### v2.15.3.0
- **http4k-connect-github** : [New module] Containing only basic callback infrastructure and Filters for checking requests.

### v2.15.2.0
- **http4k-connect-*** : upgrade http4k. This should Fix #17 (Enable custom domain in S3).

### v2.15.1.0
- **http4k-connect-*** : upgrade http4k, Kotlin.

### v2.15.0.1
- Switch to Maven Central publishing as first options

### v2.15.0.0
- **http4k-connect-google-analytics** : [Break] Harmonised interface with other clients. TrackingId now moved 
to individual requests

### v2.14.2.0
- **http4k-connect-*** : upgrade http4k, kotlin, others

### v2.14.1.0
- **http4k-connect-*** : upgrade http4k
- **http4k-connect-kapt-generator** : Un-hardcode result type as per Action interface. 

### v2.14.0.0
- **http4k-connect-*** : [Breaking] Changed Result type on Action to be generic to support other programming models. This will only affect users who are implementing their own clients. To fix, change: 
```kotlin
interface MyClient<R> : Action<R>
// to 
interface MyClient<R> : Action<Result<R, RemoteFailure>>
```

### v2.13.0.1
- **http4k-connect-amazon-s3-fake* : Send response XML as well as status code on errors.

### v2.13.0.0
- **http4k-connect-*** : Rejig of dependencies to be consistent.

### v2.12.0.0
- **http4k-connect-storage-core** : New module, containing storage abstractions which can be used without the fakes.

### v2.11.0.0
- **http4k-connect-amazon-sns** : [New module]
- **http4k-connect-amazon-sns-fake* : [New module]
- **http4k-connect-** : Make all action classes Data classes so they are test friendly
- **http4k-connect-amazon-sqs** : [Breaking] Tags is now a `List<Tag>` instead of a `Map<String, String>`.

### v2.10.0.0
- **http4k-connect-amazon-** : Add convenience functions to create clients from the system environment. 
- **http4k-connect-amazon-** : Removed unused Payload type for various clients.
- **http4k-connect-*** : Upgrade values4k and http4k

### v2.9.2.0
- **http4k-connect-amazon-** : Add convenience methods for constructing AWS clients

### v2.9.1.0
- **http4k-connect-amazon-** : Expose Moshi to client API users for JSON-based systems

### v2.9.0.0
- **http4k-connect-amazon-sqs** : Fixed SQS MessageAttributes as API is not as advertised...
- **http4k-connect-amazon-fake** : Extracting out endpoints for easier extension.

### v2.8.0.0
- **http4k-connect-*** : Upgrade to http4k 4.X.X.X.

### v2.7.1.0
- **http4k-connect-amazon-systemsmanager** : Refined model.
- **http4k-connect-amazon-*** : Fixed handling of ARNs.

### v2.7.0.0
- **http4k-connect-amazon-*** : Refined ARN model.
- **http4k-connect-amazon-s3** : Fix Delete Bucket action.

### v2.6.0.0
- **http4k-connect-amazon-*** : API improvements for all AWS services.
- **http4k-connect-*** : `defaultPort()` -> `defaultPort`

### v2.5.1.0
- **http4k-connect-amazon-lambda* : Expose AutoMarshalling in extension function.

### v2.5.0.0
- **http4k-connect-amazon-lambda* : Expose `AutoMarshalling` for invoking functions.

### v2.4.0.0
- **http4k-connect-** : Remove need for AWSCredentialScope - just use Region instead since each service already knows the scope required.

### v2.3.2.0
- **http4k-connect-amazon-sqs* : [New module] Client and fake.
- **http4k-connect-amazon-sqs-fake* : [New module] See README for limitations of FakeSQS.
- **http4k-connect-amazon-sts* : Added STSCredentialsProvider to refresh credentials when required.

### v2.3.1.1
- **http4k-connect-** : Fix #11 thread safety of DocumentBuilderFactory.

### v2.3.1.0
- **http4k-connect-amazon-lambda* : [New module] Support for invoking AWS Lambda functions.
- **http4k-connect-amazon-lambda-fake* : [New module] Includes FakeLambda runtime to run/deploy named HttpHandlers into.

### v2.3.0.0
- **http4k-connect-** : Use Kotshi generated adapters instead of Kotlin Reflection, allowing removal of large Kotlin Reflection JAR. Note that the Kotlin-reflect dependency must be explicitly excluded due to transitivity in your projects.

### v2.2.2.0
- **http4k-connect-** : Generate and ship extension functions for all actions. Rename `S3.Bucket` to `S3Bucket`.

### v2.2.1.0
- **http4k-connect-** : Ship Javadoc.

### v2.2.0.0
- **http4k-connect-** : Repackage all action classes.

### v2.1.0.0
- **http4k-connect-** : Repackage all action classes.

### v2.0.2.1
- **http4k-connect-** : Switch all interfaces to use new `invoke()` mechanism.

### v1.1.0.1
- **http4k-connect-** : Upgrade http4k and Values4k.

### v1.0.1.0
- **http4k-connect-amazon-kms-fake** : Simplify signing.

### v1.0.0.0
- **http4k-connect-amazon-kms** : [New module] New client module.
- **http4k-connect-amazon-kms-fake** : [New module] New client fake module.
- **http4k-connect-amazon-s3** : [New module] New client module.
- **http4k-connect-amazon-s3-fake** : [New module] New client fake module.
- **http4k-connect-amazon-secretsmanager** : [New module] New client module.
- **http4k-connect-amazon-secretsmanager-fake** : [New module] New client fake module.
- **http4k-connect-amazon-systemsmanager** : [New module] New client module.
- **http4k-connect-amazon-systemsmanager-fake** : [New module] New client fake module.
- **http4k-connect-google-analytics** : [New module] New client module.
- **http4k-connect-storage-http** : [New module] New storage module.
- **http4k-connect-storage-jdbc** : [New module] New storage module.
- **http4k-connect-storage-redis** : [New module] New storage module.
- **http4k-connect-storage-s3** : [New module] New storage module.

### v0.20.0.0
- Initial release.
