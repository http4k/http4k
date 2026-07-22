# OSSF scorecard (ossf-scorecard.yml)

```mermaid
%%{init: {"flowchart": {"curve": "basis"}}}%%
flowchart TD
    branchprotectionrule(["🔔 branchprotectionrule"])
    workflowdispatch(["👤 workflow_dispatch"])
    schedule(["⏰ schedule<br/>0 8 * * 1"])
    push(["📤 push<br/>branches(only: 1)"])
    subgraph ossfscorecardyml["OSSF scorecard"]
        ossfscorecardyml_metadata[["🔧 Workflow Config<br/>🔐 read-all"]]
        ossfscorecardyml_analysis["Scorecard analysis<br/>🐧 ubuntu-latest<br/>🔐 if: github.repository == 'http4k\/http4k'"]
    end
    branchprotectionrule --> ossfscorecardyml_analysis
    workflowdispatch --> ossfscorecardyml_analysis
    schedule --> ossfscorecardyml_analysis
    push --> ossfscorecardyml_analysis
```

## Job: Scorecard analysis

| Job | OS | Dependencies | Config |
|-----|----|--------------|---------| 
| `analysis` | 🐧 ubuntu-latest | - | 🔐 if 🔐 perms |

### Steps

```mermaid
%%{init: {"flowchart": {"curve": "basis"}}}%%
flowchart TD
    step1["Step 1: Checkout"]
    style step1 fill:#f8f9fa,stroke:#495057
    action1["🎬 actions<br/>checkout<br/><br/>📝 Inputs:<br/>• persist-credentials: false"]
    style action1 fill:#e1f5fe,stroke:#0277bd
    step1 -.-> action1
    step2["Step 2: Run analysis"]
    style step2 fill:#f8f9fa,stroke:#495057
    action2["🎬 ossf<br/>scorecard-action<br/><br/>📝 Inputs:<br/>• results_file: results.sarif<br/>• results_format: sarif<br/>• publish_results: true"]
    style action2 fill:#e1f5fe,stroke:#0277bd
    step2 -.-> action2
    step1 --> step2
    step3["Step 3: Upload to code-scanning"]
    style step3 fill:#f8f9fa,stroke:#495057
    action3["🎬 github<br/>codeql-action/upload-sarif<br/><br/>📝 Inputs:<br/>• sarif_file: results.sarif"]
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