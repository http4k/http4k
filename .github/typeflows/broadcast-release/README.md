# Broadcast Release (broadcast-release.yml)

```mermaid
%%{init: {"flowchart": {"curve": "basis"}}}%%
flowchart TD
    schedule(["⏰ schedule<br/>0 * * * *"])
    workflowdispatch(["👤 workflow_dispatch"])
    subgraph broadcastreleaseyml["Broadcast Release"]
        broadcastreleaseyml_checknewversion["check-new-version<br/>🐧 ubuntu-latest<br/>🔐 if: github.repository == 'http4k\/http4k'<br/>📤 Outputs: requires-broadcast, version"]
        broadcastreleaseyml_broadcastrelease["broadcast-release<br/>🐧 ubuntu-latest<br/>🔐 if: needs.check-new-version.outputs.requires-broadcast == 'true'"]
        broadcastreleaseyml_checknewversion --> broadcastreleaseyml_broadcastrelease
    end
    schedule --> broadcastreleaseyml_checknewversion
    workflowdispatch --> broadcastreleaseyml_checknewversion
```

## Job: check-new-version

| Job | OS | Dependencies | Config |
|-----|----|--------------|---------| 
| `check-new-version` | 🐧 ubuntu-latest | - | 🔐 if 📤 2 outputs |

### Steps

```mermaid
%%{init: {"flowchart": {"curve": "basis"}}}%%
flowchart TD
    step1["Step 1: Checkout"]
    style step1 fill:#f8f9fa,stroke:#495057
    action1["🎬 actions<br/>checkout"]
    style action1 fill:#e1f5fe,stroke:#0277bd
    step1 -.-> action1
    step2["Step 2: Configure AWS Credentials"]
    style step2 fill:#f8f9fa,stroke:#495057
    action2["🎬 aws-actions<br/>configure-aws-credentials<br/><br/>📝 Inputs:<br/>• aws-access-key-id: ${{ secrets.S3_ACCESS_KEY_ID }...<br/>• aws-secret-access-key: ${{ secrets.S3_SECRET_ACCESS_K...<br/>• aws-region: us-east-1"]
    style action2 fill:#e1f5fe,stroke:#0277bd
    step2 -.-> action2
    step1 --> step2
    step3["Step 3: Check new version<br/>💻 bash"]
    style step3 fill:#f3e5f5,stroke:#7b1fa2
    step2 --> step3
```

**Step Types Legend:**
- 🔘 **Step Nodes** (Gray): Workflow step execution
- 🔵 **Action Blocks** (Blue): External GitHub Actions
- 🔷 **Action Blocks** (Light Blue): Local repository actions
- 🟣 **Script Nodes** (Purple): Run commands/scripts
- **Solid arrows** (→): Step execution flow
- **Dotted arrows** (-.->): Action usage with inputs




## Job: broadcast-release

| Job | OS | Dependencies | Config |
|-----|----|--------------|---------| 
| `broadcast-release` | 🐧 ubuntu-latest | `check-new-version` | 🔐 if |

### Steps

```mermaid
%%{init: {"flowchart": {"curve": "basis"}}}%%
flowchart TD
    step1["Step 1: Checkout"]
    style step1 fill:#f8f9fa,stroke:#495057
    action1["🎬 actions<br/>checkout"]
    style action1 fill:#e1f5fe,stroke:#0277bd
    step1 -.-> action1
    step2["Step 2: Grab tag name"]
    style step2 fill:#f8f9fa,stroke:#495057
    action2["🎬 olegtarasov<br/>get-tag"]
    style action2 fill:#e1f5fe,stroke:#0277bd
    step2 -.-> action2
    step1 --> step2
    step3["Step 3: Repository Dispatch<br/>💻 bash"]
    style step3 fill:#f3e5f5,stroke:#7b1fa2
    step2 --> step3
```

**Step Types Legend:**
- 🔘 **Step Nodes** (Gray): Workflow step execution
- 🔵 **Action Blocks** (Blue): External GitHub Actions
- 🔷 **Action Blocks** (Light Blue): Local repository actions
- 🟣 **Script Nodes** (Purple): Run commands/scripts
- **Solid arrows** (→): Step execution flow
- **Dotted arrows** (-.->): Action usage with inputs