# Release API (release-api.yml)

```mermaid
%%{init: {"flowchart": {"curve": "basis"}}}%%
flowchart TD
    repositorydispatch(["🔔 repository_dispatch<br/>(http4k-release)"])
    workflowdispatch(["👤 workflow_dispatch<br/>inputs: version"])
    subgraph releaseapiyml["Release API"]
        releaseapiyml_metadata[["🔧 Workflow Config<br/>🔐 custom permissions"]]
        releaseapiyml_releaseapi["release-api<br/>🐧 ubuntu-latest"]
    end
    repositorydispatch --> releaseapiyml_releaseapi
    workflowdispatch --> releaseapiyml_releaseapi
```

## Job: release-api

| Job | OS | Dependencies | Config |
|-----|----|--------------|---------| 
| `release-api` | 🐧 ubuntu-latest | - | - |

### Steps

```mermaid
%%{init: {"flowchart": {"curve": "basis"}}}%%
flowchart TD
    step1["Step 1: Checkout"]
    style step1 fill:#f8f9fa,stroke:#495057
    action1["🎬 actions<br/>checkout"]
    style action1 fill:#e1f5fe,stroke:#0277bd
    step1 -.-> action1
    step2["Step 2: Validate release version<br/>💻 bash"]
    style step2 fill:#f3e5f5,stroke:#7b1fa2
    step1 --> step2
    step3["Step 3: Setup Java"]
    style step3 fill:#f8f9fa,stroke:#495057
    action3["🎬 actions<br/>setup-java<br/><br/>📝 Inputs:<br/>• java-version: 21<br/>• distribution: adopt"]
    style action3 fill:#e1f5fe,stroke:#0277bd
    step3 -.-> action3
    step2 --> step3
    step4["Step 4: Setup Gradle"]
    style step4 fill:#f8f9fa,stroke:#495057
    action4["🎬 gradle<br/>actions/setup-gradle"]
    style action4 fill:#e1f5fe,stroke:#0277bd
    step4 -.-> action4
    step3 --> step4
    step5["Step 5: Generate API docs<br/>💻 bash"]
    style step5 fill:#f3e5f5,stroke:#7b1fa2
    step4 --> step5
    step6["Step 6: Checkout API repo"]
    style step6 fill:#f8f9fa,stroke:#495057
    action6["🎬 actions<br/>checkout<br/><br/>📝 Inputs:<br/>• repository: http4k/api<br/>• token: ${{ secrets.AUTHOR_TOKEN }}<br/>• path: tmp"]
    style action6 fill:#e1f5fe,stroke:#0277bd
    step6 -.-> action6
    step5 --> step6
    step7["Step 7: Copy docs<br/>💻 bash"]
    style step7 fill:#f3e5f5,stroke:#7b1fa2
    step6 --> step7
    step8["Step 8: Commit API docs"]
    style step8 fill:#f8f9fa,stroke:#495057
    action8["🎬 EndBug<br/>add-and-commit<br/><br/>📝 Inputs:<br/>• cwd: tmp<br/>• message: release API docs"]
    style action8 fill:#e1f5fe,stroke:#0277bd
    step8 -.-> action8
    step7 --> step8
    step9["Step 9: Push API docs"]
    style step9 fill:#f8f9fa,stroke:#495057
    action9["🎬 ad-m<br/>github-push-action<br/><br/>📝 Inputs:<br/>• github_token: ${{ secrets.AUTHOR_TOKEN }}<br/>• directory: tmp<br/>• repository: http4k/api"]
    style action9 fill:#e1f5fe,stroke:#0277bd
    step9 -.-> action9
    step8 --> step9
```

**Step Types Legend:**
- 🔘 **Step Nodes** (Gray): Workflow step execution
- 🔵 **Action Blocks** (Blue): External GitHub Actions
- 🔷 **Action Blocks** (Light Blue): Local repository actions
- 🟣 **Script Nodes** (Purple): Run commands/scripts
- **Solid arrows** (→): Step execution flow
- **Dotted arrows** (-.->): Action usage with inputs