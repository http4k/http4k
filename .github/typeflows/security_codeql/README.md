# Security - Vulnerability Scanning (CodeQL) (security_codeql.yml)

```mermaid
%%{init: {"flowchart": {"curve": "basis"}}}%%
flowchart TD
    push(["📤 push<br/>branches(only: 1), paths(ignore: 1)"])
    pullrequest(["🔀 pull_request<br/>(*), branches(only: 1), paths(ignore: 1)"])
    schedule(["⏰ schedule<br/>0 12 * * 3"])
    subgraph securitycodeqlyml["Security - Vulnerability Scanning (CodeQL)"]
        securitycodeqlyml_metadata[["🔧 Workflow Config<br/>🔐 custom permissions"]]
        securitycodeqlyml_analyze["Analyze<br/>🐧 ubuntu-latest<br/>⏱️ 360m timeout<br/>🔐 if: github.repository == 'http4k\/http4k'"]
    end
    push --> securitycodeqlyml_analyze
    pullrequest --> securitycodeqlyml_analyze
    schedule --> securitycodeqlyml_analyze
```

## Job: Analyze

| Job | OS | Dependencies | Config |
|-----|----|--------------|---------| 
| `analyze` | 🐧 ubuntu-latest | - | ⏱️ 360m 🔐 if 🔐 perms |

### Steps

```mermaid
%%{init: {"flowchart": {"curve": "basis"}}}%%
flowchart TD
    step1["Step 1: Checkout repository"]
    style step1 fill:#f8f9fa,stroke:#495057
    action1["🎬 actions<br/>checkout"]
    style action1 fill:#e1f5fe,stroke:#0277bd
    step1 -.-> action1
    step2["Step 2: Initialize CodeQL"]
    style step2 fill:#f8f9fa,stroke:#495057
    action2["🎬 github<br/>codeql-action/init<br/><br/>📝 Inputs:<br/>• languages: java<br/>• build-mode: none"]
    style action2 fill:#e1f5fe,stroke:#0277bd
    step2 -.-> action2
    step1 --> step2
    step3["Step 3: Perform CodeQL Analysis"]
    style step3 fill:#f8f9fa,stroke:#495057
    action3["🎬 github<br/>codeql-action/analyze<br/><br/>📝 Inputs:<br/>• category: /language:java"]
    style action3 fill:#e1f5fe,stroke:#0277bd
    step3 -.-> action3
    step2 --> step3
```

**Step Types Legend:**
- 🔘 **Step Nodes** (Gray): Workflow step execution
- 🔵 **Action Blocks** (Blue): External GitHub Actions
- 🔷 **Action Blocks** (Light Blue): Local repository actions
- 🟣 **Script Nodes** (Purple): Run commands/scripts
- **Solid arrows** (→): Step execution flow
- **Dotted arrows** (-.->): Action usage with inputs