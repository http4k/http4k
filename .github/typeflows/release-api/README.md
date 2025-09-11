# Release API (release-api.yml)

```mermaid
%%{init: {"flowchart": {"curve": "basis"}}}%%
flowchart TD
    repositorydispatch(["🔔 repository_dispatch<br/>(http4k-release)"])
    subgraph releaseapiyml["Release API"]
        releaseapiyml_releaseapi["release-api<br/>🐧 ubuntu-latest"]
    end
    repositorydispatch --> releaseapiyml_releaseapi
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
    step2["Step 2: Setup Java"]
    style step2 fill:#f8f9fa,stroke:#495057
    action2["🎬 actions<br/>setup-java<br/><br/>📝 Inputs:<br/>• java-version: 21<br/>• distribution: adopt"]
    style action2 fill:#e1f5fe,stroke:#0277bd
    step2 -.-> action2
    step1 --> step2
    step3["Step 3: Setup Gradle"]
    style step3 fill:#f8f9fa,stroke:#495057
    action3["🎬 gradle<br/>actions/setup-gradle"]
    style action3 fill:#e1f5fe,stroke:#0277bd
    step3 -.-> action3
    step2 --> step3
    step4["Step 4: Generate API docs<br/>💻 bash"]
    style step4 fill:#f3e5f5,stroke:#7b1fa2
    step3 --> step4
    step5["Step 5: Checkout API repo"]
    style step5 fill:#f8f9fa,stroke:#495057
    action5["🎬 actions<br/>checkout<br/><br/>📝 Inputs:<br/>• repository: http4k/api<br/>• token: ${{ secrets.AUTHOR_TOKEN }}<br/>• path: tmp"]
    style action5 fill:#e1f5fe,stroke:#0277bd
    step5 -.-> action5
    step4 --> step5
    step6["Step 6: Copy docs<br/>💻 bash"]
    style step6 fill:#f3e5f5,stroke:#7b1fa2
    step5 --> step6
    step7["Step 7: Commit API docs"]
    style step7 fill:#f8f9fa,stroke:#495057
    action7["🎬 EndBug<br/>add-and-commit<br/><br/>📝 Inputs:<br/>• cwd: tmp<br/>• message: release API docs"]
    style action7 fill:#e1f5fe,stroke:#0277bd
    step7 -.-> action7
    step6 --> step7
    step8["Step 8: Push API docs"]
    style step8 fill:#f8f9fa,stroke:#495057
    action8["🎬 ad-m<br/>github-push-action<br/><br/>📝 Inputs:<br/>• github_token: ${{ secrets.AUTHOR_TOKEN }}<br/>• directory: tmp<br/>• repository: http4k/api"]
    style action8 fill:#e1f5fe,stroke:#0277bd
    step8 -.-> action8
    step7 --> step8
```

**Step Types Legend:**
- 🔘 **Step Nodes** (Gray): Workflow step execution
- 🔵 **Action Blocks** (Blue): External GitHub Actions
- 🔷 **Action Blocks** (Light Blue): Local repository actions
- 🟣 **Script Nodes** (Purple): Run commands/scripts
- **Solid arrows** (→): Step execution flow
- **Dotted arrows** (-.->): Action usage with inputs