# Build (build-http4k.yml)

```mermaid
%%{init: {"flowchart": {"curve": "basis"}}}%%
flowchart TD
    push(["📤 push<br/>branches(only: 1), paths(ignore: 1)"])
    pullrequest(["🔀 pull_request<br/>(*), branches(ignore: 1), paths(ignore: 1)"])
    subgraph buildhttp4kyml["Build"]
        buildhttp4kyml_metadata[["🔧 Workflow Config<br/>🔐 custom permissions"]]
        buildhttp4kyml_build["build<br/>🐧 ubuntu-latest<br/>🔑 Uses secrets"]
    end
    push --> buildhttp4kyml_build
    pullrequest --> buildhttp4kyml_build
```

## Job: build

| Job | OS | Dependencies | Config |
|-----|----|--------------|---------| 
| `build` | 🐧 ubuntu-latest | - | 🌍 env 🔐 perms |

### Steps

```mermaid
%%{init: {"flowchart": {"curve": "basis"}}}%%
flowchart TD
    step1["Step 1: Checkout"]
    style step1 fill:#f8f9fa,stroke:#495057
    action1["🎬 actions<br/>checkout<br/><br/>📝 Inputs:<br/>• persist-credentials: false<br/>• fetch-depth: 2"]
    style action1 fill:#e1f5fe,stroke:#0277bd
    step1 -.-> action1
    step2["Step 2: Validate Gradle wrapper"]
    style step2 fill:#f8f9fa,stroke:#495057
    action2["🎬 gradle<br/>actions/wrapper-validation"]
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
    step5["Step 5: Build<br/>💻 bash<br/>⏱️ 120m timeout"]
    style step5 fill:#f3e5f5,stroke:#7b1fa2
    step4 --> step5
    step6["Step 6: Upload coverage to Codecov"]
    style step6 fill:#f8f9fa,stroke:#495057
    action6["🎬 codecov<br/>codecov-action<br/><br/>📝 Inputs:<br/>• token: ${{ secrets.CODECOV_TOKEN }}<br/>• files: build/reports/jacoco/test/jaco..."]
    style action6 fill:#e1f5fe,stroke:#0277bd
    step6 -.-> action6
    step5 --> step6
    step7["Step 7: Buildnote<br/>🔐 if: always()"]
    style step7 fill:#f8f9fa,stroke:#495057
    action7["🎬 buildnote<br/>action"]
    style action7 fill:#e1f5fe,stroke:#0277bd
    step7 -.-> action7
    step6 --> step7
    step8["Step 8: Publish Test Report<br/>🔐 if: always()"]
    style step8 fill:#f8f9fa,stroke:#495057
    action8["🎬 mikepenz<br/>action-junit-report<br/><br/>📝 Inputs:<br/>• report_paths: **/build/test-results/test/TES...<br/>• github_token: ${{ secrets.GITHUB_TOKEN }}<br/>• check_annotations: true<br/>• update_check: true"]
    style action8 fill:#e1f5fe,stroke:#0277bd
    step8 -.-> action8
    step7 --> step8
    step9["Step 9: Release (if required)<br/>🔐 if: github.ref == 'refs/heads/master'<br/>💻 bash"]
    style step9 fill:#f3e5f5,stroke:#7b1fa2
    step8 --> step9
```

**Step Types Legend:**
- 🔘 **Step Nodes** (Gray): Workflow step execution
- 🔵 **Action Blocks** (Blue): External GitHub Actions
- 🔷 **Action Blocks** (Light Blue): Local repository actions
- 🟣 **Script Nodes** (Purple): Run commands/scripts
- **Solid arrows** (→): Step execution flow
- **Dotted arrows** (-.->): Action usage with inputs