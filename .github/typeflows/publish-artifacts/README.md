# Publish Artifacts (publish-artifacts.yml)

```mermaid
%%{init: {"flowchart": {"curve": "basis"}}}%%
flowchart TD
    push(["📤 push<br/>tags(only: 1)"])
    subgraph publishartifactsyml["Publish Artifacts"]
        publishartifactsyml_metadata[["🔧 Workflow Config<br/>🔐 custom permissions"]]
        publishartifactsyml_build["build<br/>🐧 ubuntu-latest<br/>🔐 if: github.repository == 'http4k\/http4k'"]
        publishartifactsyml_attest["attest<br/>🐧 ubuntu-latest<br/>🔐 if: github.repository == 'http4k\/http4k'"]
        publishartifactsyml_build --> publishartifactsyml_attest
    end
    push --> publishartifactsyml_build
```

## Job: build

| Job | OS | Dependencies | Config |
|-----|----|--------------|---------| 
| `build` | 🐧 ubuntu-latest | - | 🔐 if 🔐 perms |

### Steps

```mermaid
%%{init: {"flowchart": {"curve": "basis"}}}%%
flowchart TD
    step1["Step 1: Checkout"]
    style step1 fill:#f8f9fa,stroke:#495057
    action1["🎬 actions<br/>checkout<br/><br/>📝 Inputs:<br/>• ref: ${{ github.ref_name }}"]
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
    step4["Step 4: Build artifacts<br/>💻 bash"]
    style step4 fill:#f3e5f5,stroke:#7b1fa2
    step3 --> step4
    step5["Step 5: Generate SBOMs<br/>💻 bash"]
    style step5 fill:#f3e5f5,stroke:#7b1fa2
    step4 --> step5
    step6["Step 6: Generate license reports<br/>💻 bash"]
    style step6 fill:#f3e5f5,stroke:#7b1fa2
    step5 --> step6
    step7["Step 7: Build publish manifest<br/>💻 bash"]
    style step7 fill:#f3e5f5,stroke:#7b1fa2
    step6 --> step7
    step8["Step 8: Configure AWS credentials (read)"]
    style step8 fill:#f8f9fa,stroke:#495057
    action8["🎬 aws-actions<br/>configure-aws-credentials<br/><br/>📝 Inputs:<br/>• aws-access-key-id: ${{ secrets.LTS_PUBLISHING_USE...<br/>• aws-secret-access-key: ${{ secrets.LTS_PUBLISHING_PAS...<br/>• aws-region: us-east-1"]
    style action8 fill:#e1f5fe,stroke:#0277bd
    step8 -.-> action8
    step7 --> step8
    step9["Step 9: Pre-seed maven-metadata for merge<br/>💻 bash"]
    style step9 fill:#f3e5f5,stroke:#7b1fa2
    step8 --> step9
    step10["Step 10: Build S3 Maven layout<br/>💻 bash"]
    style step10 fill:#f3e5f5,stroke:#7b1fa2
    step9 --> step10
    step11["Step 11: Publish to Maven Central<br/>💻 bash"]
    style step11 fill:#f3e5f5,stroke:#7b1fa2
    step10 --> step11
    step12["Step 12: Package build outputs for signing<br/>💻 bash"]
    style step12 fill:#f3e5f5,stroke:#7b1fa2
    step11 --> step12
    step13["Step 13: Upload build outputs"]
    style step13 fill:#f8f9fa,stroke:#495057
    action13["🎬 actions<br/>upload-artifact<br/><br/>📝 Inputs:<br/>• name: build-outputs<br/>• path: build-outputs.tar.gz<br/>• retention-days: 1"]
    style action13 fill:#e1f5fe,stroke:#0277bd
    step13 -.-> action13
    step12 --> step13
```

**Step Types Legend:**
- 🔘 **Step Nodes** (Gray): Workflow step execution
- 🔵 **Action Blocks** (Blue): External GitHub Actions
- 🔷 **Action Blocks** (Light Blue): Local repository actions
- 🟣 **Script Nodes** (Purple): Run commands/scripts
- **Solid arrows** (→): Step execution flow
- **Dotted arrows** (-.->): Action usage with inputs




## Job: attest

| Job | OS | Dependencies | Config |
|-----|----|--------------|---------| 
| `attest` | 🐧 ubuntu-latest | `build` | 🔐 if 🔐 perms |

### Steps

```mermaid
%%{init: {"flowchart": {"curve": "basis"}}}%%
flowchart TD
    step1["Step 1: Checkout"]
    style step1 fill:#f8f9fa,stroke:#495057
    action1["🎬 actions<br/>checkout<br/><br/>📝 Inputs:<br/>• ref: ${{ github.ref_name }}"]
    style action1 fill:#e1f5fe,stroke:#0277bd
    step1 -.-> action1
    step2["Step 2: Install cosign"]
    style step2 fill:#f8f9fa,stroke:#495057
    action2["🎬 sigstore<br/>cosign-installer"]
    style action2 fill:#e1f5fe,stroke:#0277bd
    step2 -.-> action2
    step1 --> step2
    step3["Step 3: Configure AWS credentials (write)"]
    style step3 fill:#f8f9fa,stroke:#495057
    action3["🎬 aws-actions<br/>configure-aws-credentials<br/><br/>📝 Inputs:<br/>• aws-access-key-id: ${{ secrets.LTS_PUBLISHING_USE...<br/>• aws-secret-access-key: ${{ secrets.LTS_PUBLISHING_PAS...<br/>• aws-region: us-east-1"]
    style action3 fill:#e1f5fe,stroke:#0277bd
    step3 -.-> action3
    step2 --> step3
    step4["Step 4: Download build outputs"]
    style step4 fill:#f8f9fa,stroke:#495057
    action4["🎬 actions<br/>download-artifact<br/><br/>📝 Inputs:<br/>• name: build-outputs"]
    style action4 fill:#e1f5fe,stroke:#0277bd
    step4 -.-> action4
    step3 --> step4
    step5["Step 5: Restore build outputs<br/>💻 bash"]
    style step5 fill:#f3e5f5,stroke:#7b1fa2
    step4 --> step5
    step6["Step 6: Sign artifacts and generate provenance<br/>💻 bash"]
    style step6 fill:#f3e5f5,stroke:#7b1fa2
    step5 --> step6
    step7["Step 7: Publish to http4k Maven (S3)<br/>💻 bash"]
    style step7 fill:#f3e5f5,stroke:#7b1fa2
    step6 --> step7
    step8["Step 8: Notify LTS Slack<br/>💻 bash"]
    style step8 fill:#f3e5f5,stroke:#7b1fa2
    step7 --> step8
```

**Step Types Legend:**
- 🔘 **Step Nodes** (Gray): Workflow step execution
- 🔵 **Action Blocks** (Blue): External GitHub Actions
- 🔷 **Action Blocks** (Light Blue): Local repository actions
- 🟣 **Script Nodes** (Purple): Run commands/scripts
- **Solid arrows** (→): Step execution flow
- **Dotted arrows** (-.->): Action usage with inputs