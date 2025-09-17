# Update Dependencies (update-dependencies.yml)

```mermaid
%%{init: {"flowchart": {"curve": "basis"}}}%%
flowchart TD
    schedule(["⏰ schedule<br/>0 8 * * 1"])
    workflowdispatch(["👤 workflow_dispatch"])
    subgraph updatedependenciesyml["Update Dependencies"]
        updatedependenciesyml_updatedependencies["update-dependencies<br/>🐧 ubuntu-latest"]
    end
    schedule --> updatedependenciesyml_updatedependencies
    workflowdispatch --> updatedependenciesyml_updatedependencies
```

## Job: update-dependencies

| Job | OS | Dependencies | Config |
|-----|----|--------------|---------| 
| `update-dependencies` | 🐧 ubuntu-latest | - | 🔐 perms |

### Steps

```mermaid
%%{init: {"flowchart": {"curve": "basis"}}}%%
flowchart TD
    step1["Step 1: Checkout repository"]
    style step1 fill:#f8f9fa,stroke:#495057
    action1["🎬 actions<br/>checkout<br/><br/>📝 Inputs:<br/>• token: ${{ secrets.GITHUB_TOKEN }}"]
    style action1 fill:#e1f5fe,stroke:#0277bd
    step1 -.-> action1
    step2["Step 2: Set up JDK"]
    style step2 fill:#f8f9fa,stroke:#495057
    action2["🎬 actions<br/>setup-java<br/><br/>📝 Inputs:<br/>• java-version: 21<br/>• distribution: temurin"]
    style action2 fill:#e1f5fe,stroke:#0277bd
    step2 -.-> action2
    step1 --> step2
    step3["Step 3: Setup Gradle"]
    style step3 fill:#f8f9fa,stroke:#495057
    action3["🎬 gradle<br/>actions/setup-gradle"]
    style action3 fill:#e1f5fe,stroke:#0277bd
    step3 -.-> action3
    step2 --> step3
    step4["Step 4: Build<br/>💻 bash"]
    style step4 fill:#f3e5f5,stroke:#7b1fa2
    step3 --> step4
    step5["Step 5: Build<br/>🔐 if: steps.verify-changed-files.outputs.changed == 'true'<br/>💻 bash<br/>⏱️ 120m timeout"]
    style step5 fill:#f3e5f5,stroke:#7b1fa2
    step4 --> step5
    step6["Step 6: Check for changes<br/>💻 bash"]
    style step6 fill:#f3e5f5,stroke:#7b1fa2
    step5 --> step6
    step7["Step 7: Create Pull Request<br/>🔐 if: steps.changes.outputs.has_changes"]
    style step7 fill:#f8f9fa,stroke:#495057
    action7["🎬 peter-evans<br/>create-pull-request<br/><br/>📝 Inputs:<br/>• token: ${{ secrets.GITHUB_TOKEN }}<br/>• commit-message: chore: Update dependencies<br/>• title: chore: update dependencies<br/>• body: This PR updates dependencies i...<br/>• branch: update-dependencies<br/>• delete-branch: true"]
    style action7 fill:#e1f5fe,stroke:#0277bd
    step7 -.-> action7
    step6 --> step7
```

**Step Types Legend:**
- 🔘 **Step Nodes** (Gray): Workflow step execution
- 🔵 **Action Blocks** (Blue): External GitHub Actions
- 🔷 **Action Blocks** (Light Blue): Local repository actions
- 🟣 **Script Nodes** (Purple): Run commands/scripts
- **Solid arrows** (→): Step execution flow
- **Dotted arrows** (-.->): Action usage with inputs