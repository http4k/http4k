# New Release - Update other projects (new-release-upgrade-branches.yml)

```mermaid
%%{init: {"flowchart": {"curve": "basis"}}}%%
flowchart TD
    repositorydispatch(["🔔 repository_dispatch<br/>(http4k-release)"])
    subgraph newreleaseupgradebranchesyml["New Release - Update other projects"]
        newreleaseupgradebranchesyml_createupgradebranches["create-upgrade-branches<br/>🐧 ubuntu-latest<br/>📊 Matrix: repo (11 runs)"]
    end
    repositorydispatch --> newreleaseupgradebranchesyml_createupgradebranches
```

## Job: create-upgrade-branches

| Job | OS | Dependencies | Config |
|-----|----|--------------|---------| 
| `create-upgrade-branches` | 🐧 ubuntu-latest | - | 🔄 matrix |

### Steps

```mermaid
%%{init: {"flowchart": {"curve": "basis"}}}%%
flowchart TD
    step1["Step 1: Trigger ${{ matrix.repo }}<br/>💻 bash"]
    style step1 fill:#f3e5f5,stroke:#7b1fa2
```

**Step Types Legend:**
- 🔘 **Step Nodes** (Gray): Workflow step execution
- 🔵 **Action Blocks** (Blue): External GitHub Actions
- 🔷 **Action Blocks** (Light Blue): Local repository actions
- 🟣 **Script Nodes** (Purple): Run commands/scripts
- **Solid arrows** (→): Step execution flow
- **Dotted arrows** (-.->): Action usage with inputs