# Publish Artifacts (publish-artifacts.yml)

```mermaid
%%{init: {"flowchart": {"curve": "basis"}}}%%
flowchart TD
    push(["📤 push<br/>tags(only: 1)"])
    subgraph publishartifactsyml["Publish Artifacts"]
        publishartifactsyml_metadata[["🔧 Workflow Config<br/>🌍 1 env var"]]
        publishartifactsyml_release["release<br/>🐧 ubuntu-latest<br/>🔐 if: github.repository == 'http4k\/http4k'"]
    end
    push --> publishartifactsyml_release
```

## Job: release

| Job | OS | Dependencies | Config |
|-----|----|--------------|---------| 
| `release` | 🐧 ubuntu-latest | - | 🔐 if |

### Steps

```mermaid
%%{init: {"flowchart": {"curve": "basis"}}}%%
flowchart TD
    step1["Step 1: Checkout"]
    style step1 fill:#f8f9fa,stroke:#495057
    action1["🎬 actions<br/>checkout<br/><br/>📝 Inputs:<br/>• ref: ${{ steps.tagName.outputs.tag ..."]
    style action1 fill:#e1f5fe,stroke:#0277bd
    step1 -.-> action1
    step2["Step 2: Grab tag name"]
    style step2 fill:#f8f9fa,stroke:#495057
    action2["🎬 olegtarasov<br/>get-tag"]
    style action2 fill:#e1f5fe,stroke:#0277bd
    step2 -.-> action2
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
    step5["Step 5: Install cosign"]
    style step5 fill:#f8f9fa,stroke:#495057
    action5["🎬 sigstore<br/>cosign-installer"]
    style action5 fill:#e1f5fe,stroke:#0277bd
    step5 -.-> action5
    step4 --> step5
    step6["Step 6: Build artifacts<br/>💻 bash"]
    style step6 fill:#f3e5f5,stroke:#7b1fa2
    step5 --> step6
    step7["Step 7: Generate SBOMs<br/>💻 bash"]
    style step7 fill:#f3e5f5,stroke:#7b1fa2
    step6 --> step7
    step8["Step 8: Generate license reports<br/>💻 bash"]
    style step8 fill:#f3e5f5,stroke:#7b1fa2
    step7 --> step8
    step9["Step 9: Build publish manifest<br/>💻 bash"]
    style step9 fill:#f3e5f5,stroke:#7b1fa2
    step8 --> step9
    step10["Step 10: Sign artifacts and generate provenance<br/>💻 bash"]
    style step10 fill:#f3e5f5,stroke:#7b1fa2
    step9 --> step10
    step11["Step 11: Publish to http4k Maven<br/>💻 bash"]
    style step11 fill:#f3e5f5,stroke:#7b1fa2
    step10 --> step11
    step12["Step 12: Publish to Maven Central<br/>💻 bash"]
    style step12 fill:#f3e5f5,stroke:#7b1fa2
    step11 --> step12
    step13["Step 13: Notify LTS Slack<br/>💻 bash"]
    style step13 fill:#f3e5f5,stroke:#7b1fa2
    step12 --> step13
```

**Step Types Legend:**
- 🔘 **Step Nodes** (Gray): Workflow step execution
- 🔵 **Action Blocks** (Blue): External GitHub Actions
- 🔷 **Action Blocks** (Light Blue): Local repository actions
- 🟣 **Script Nodes** (Purple): Run commands/scripts
- **Solid arrows** (→): Step execution flow
- **Dotted arrows** (-.->): Action usage with inputs