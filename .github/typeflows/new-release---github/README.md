# New Release - GitHub

```mermaid
%%{init: {"flowchart": {"curve": "basis"}}}%%
flowchart TD
    repositorydispatch(["🔔 repository_dispatch<br/>(http4k-release)"])
    subgraph newreleasegithub["New Release - GitHub"]
        newreleasegithub_metadata[["🔧 Workflow Config<br/>🔐 custom permissions"]]
        newreleasegithub_release["release<br/>🐧 ubuntu-latest"]
    end
    repositorydispatch --> newreleasegithub_release
```

## Job: release

| Job | OS | Dependencies | Config |
|-----|----|--------------|---------| 
| `release` | 🐧 ubuntu-latest | - | 🔐 perms |

### Steps

```mermaid
%%{init: {"flowchart": {"curve": "basis"}}}%%
flowchart TD
    step1["Step 1: Checkout"]
    style step1 fill:#f8f9fa,stroke:#495057
    action1["🎬 actions<br/>checkout"]
    style action1 fill:#e1f5fe,stroke:#0277bd
    step1 -.-> action1
    step2["Step 2: Build release note<br/>💻 bash"]
    style step2 fill:#f3e5f5,stroke:#7b1fa2
    step1 --> step2
    step3["Step 3: Create Release"]
    style step3 fill:#f8f9fa,stroke:#495057
    action3["🎬 actions<br/>create-release<br/><br/>📝 Inputs:<br/>• tag_name: ${{ github.event.client_payloa...<br/>• release_name: ${{ github.event.client_payloa...<br/>• body_path: NOTE.md<br/>• draft: false<br/>• prerelease: false"]
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