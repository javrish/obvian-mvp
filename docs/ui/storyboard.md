# UI Storyboard: Petri Net DAG System

## Overview

This storyboard outlines the 5-screen user journey for the Petri Net DAG proof-of-concept system, showing the complete flow from natural language input to validated, simulated workflows with comprehensive tracing.

## Screen 1: Input & Parsing

### Layout
```
┌─────────────────────────────────────────────────────────────────┐
│ Obvian Petri Net DAG System                    [POC Warning] ⚠️ │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│ Enter your workflow description:                                │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ Every time I push code: run tests; if pass deploy to       │ │
│ │ staging; if fail alert Slack                               │ │
│ │                                                             │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│ Template Examples:                                              │
│ • DevOps: "Every time I push code: run tests; if pass..."      │
│ • Football: "Warm-up, then pass and shoot in parallel..."      │
│                                                                 │
│                           [Parse Workflow]                      │
│                                                                 │
│ ✅ Parsing successful! Found DevOps CI/CD pattern              │
│ • Identified: run_tests → branch → deploy_staging | alert      │
│ • Structure: Sequential with XOR choice                        │
│                                                                 │
│                           [Build Petri Net]                     │
└─────────────────────────────────────────────────────────────────┘
```

### Interactions
- **Text Input**: Real-time parsing feedback as user types
- **Template Selection**: Click examples to populate input field
- **Parse Button**: Triggers template matching and intent extraction
- **Success Feedback**: Green checkmark with parsed structure summary
- **Error Handling**: Red warning with suggestions for unsupported patterns

### Copy & Messaging
- **Header**: "Obvian Petri Net DAG System" with prominent POC warning
- **Prompt**: "Enter your workflow description:"
- **Success**: "✅ Parsing successful! Found [template type] pattern"
- **Error**: "❌ Pattern not recognized. Try: [suggestion]"

## Screen 2: Dual Graph Visualization

### Layout
```
┌─────────────────────────────────────────────────────────────────┐
│ Workflow: Deploy On Green                      [Validate] [⚙️]   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│ Petri Net View                    │ DAG View                    │
│ ┌───────────────────────────────┐ │ ┌─────────────────────────┐ │
│ │     ●p_code                   │ │ │   t_run_tests           │ │
│ │      │                       │ │ │      │                  │ │
│ │   t_run_tests                 │ │ │   ┌──┴──┐               │ │
│ │      │                       │ │ │   │     │               │ │
│ │   ●p_testing                  │ │ │ t_pass t_fail           │ │
│ │    ┌─┴─┐                     │ │ │   │     │               │ │
│ │ t_pass t_fail                 │ │ │ t_deploy t_notify       │ │
│ │    │     │                   │ │ │   │     │               │ │
│ │ ●p_pass ●p_fail               │ │ │   └──┬──┘               │ │
│ │    │     │                   │ │ │      │                  │ │
│ │ t_deploy t_notify             │ │ │   t_finish              │ │
│ │    │     │                   │ │ └─────────────────────────┘ │
│ │ ●p_deployed ●p_alerted        │ │                           │
│ │    └──┬──┘                   │ │ Legend:                    │
│ │    t_finish                   │ │ ● Places (states)          │
│ │       │                      │ │ □ Transitions (actions)     │
│ │    ●p_done                    │ │ → Flow direction           │
│ └───────────────────────────────┘ │ 🔗 Cross-highlighted       │
│                                                                 │
│ Source of Truth: Petri Net │ Simplified View: DAG              │
└─────────────────────────────────────────────────────────────────┘
```

### Interactions
- **Cross-Highlighting**: Hover on t_run_tests in DAG highlights corresponding transition in Petri net
- **Zoom/Pan**: Mouse wheel and drag for navigation in both views
- **Node Selection**: Click to select and show details panel
- **View Toggle**: Switch between synchronized and independent view modes
- **Validate Button**: Triggers formal verification of the workflow

### Visual Elements
- **Places**: Circles with token indicators (filled = has token)
- **Transitions**: Rectangles with action names
- **Arcs**: Arrows showing token flow direction
- **Highlighting**: Blue glow for selected elements, yellow for cross-highlighted
- **Synchronization**: Both views update simultaneously during interactions

## Screen 3: Validation Results

### Layout
```
┌─────────────────────────────────────────────────────────────────┐
│ Validation Results                             [Simulate] [⚙️]   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│ ✅ VALIDATION PASSED - Workflow is safe for execution           │
│                                                                 │
│ Detailed Results:                                               │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ ✅ Deadlock Detection: PASS                                 │ │
│ │    No unreachable states found                              │ │
│ │                                                             │ │
│ │ ✅ Reachability Analysis: PASS                              │ │
│ │    Terminal state reachable via both paths                 │ │
│ │    States explored: 7 (bound: 200)                         │ │
│ │                                                             │ │
│ │ ✅ Liveness Check: PASS                                     │ │
│ │    All transitions can eventually fire                      │ │
│ │                                                             │ │
│ │ ✅ Boundedness Check: PASS                                  │ │
│ │    Token counts stay within limits                         │ │
│ │    Max tokens per place: 1                                 │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│ Workflow Properties:                                            │
│ • Places: 7, Transitions: 6, Arcs: 12                         │
│ • Execution Paths: 2 (deploy success, alert failure)          │
│ • Synchronization Points: 1 (final completion)                │
│                                                                 │
│                           [Start Simulation]                    │
└─────────────────────────────────────────────────────────────────┘
```

### Interactions
- **Expandable Sections**: Click to show/hide detailed validation results
- **Error Navigation**: Click on failed checks to highlight problematic areas in graph
- **Simulation Button**: Enabled only after successful validation
- **Export Results**: Download validation report as JSON or PDF

### Status Indicators
- **PASS**: Green checkmark with brief explanation
- **FAIL**: Red X with detailed error description and suggested fixes
- **INCONCLUSIVE**: Yellow warning with explanation (e.g., "Bound reached")

### Error Example (Alternative State)
```
❌ VALIDATION FAILED - Issues found in workflow

Detailed Results:
┌─────────────────────────────────────────────────────────────┐
│ ❌ Deadlock Detection: FAIL                                 │
│    Deadlock found at marking: {p_passing: 1}               │
│    → Cooldown reachable without completing shooting        │
│                                                             │
│ 💡 Suggested Fix:                                           │
│    Add AND-join before t_cooldown to ensure both           │
│    activities complete before proceeding                    │
└─────────────────────────────────────────────────────────────┘
```

## Screen 4: Simulation & Trace

### Layout
```
┌─────────────────────────────────────────────────────────────────┐
│ Simulation: Deploy On Green                    [⏸️] [⏭️] [🔄]    │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│ Petri Net (Live)              │ Trace Events                    │
│ ┌───────────────────────────┐ │ ┌─────────────────────────────┐ │
│ │     ○p_code               │ │ │ Step 1: t_run_tests         │ │
│ │      │                   │ │ │ 10:00:00.123                │ │
│ │   t_run_tests             │ │ │ p_code → p_testing          │ │
│ │      │                   │ │ │ Token: τ1                   │ │
│ │   🔵p_testing              │ │ │                             │ │
│ │    ┌─┴─┐                 │ │ │ Step 2: t_pass              │ │
│ │ t_pass✨t_fail            │ │ │ 10:00:01.245                │ │
│ │    │     │               │ │ │ p_testing → p_pass          │ │
│ │ ○p_pass ○p_fail           │ │ │ Token: τ1 (deterministic)   │ │
│ │    │     │               │ │ │                             │ │
│ │ t_deploy t_notify         │ │ │ Step 3: t_deploy            │ │
│ │    │     │               │ │ │ 10:00:02.367                │ │
│ │ ○p_deployed ○p_alerted    │ │ │ p_pass → p_deployed         │ │
│ │    └──┬──┘               │ │ │ Token: τ1                   │ │
│ │    t_finish               │ │ │                             │ │
│ │       │                  │ │ │ ⏸️ Simulation paused         │ │
│ │    ○p_done                │ │ │                             │ │
│ └───────────────────────────┘ │ └─────────────────────────────┘ │
│                                                                 │
│ Controls:                      │ Export:                        │
│ ⏯️ Play/Pause  ⏭️ Step  🔄 Reset │ 📄 Trace JSON  📊 Mermaid     │
│ Speed: ●────○────○ (2x)        │ 🎯 Full Report  📋 Summary     │
│ Mode: ● Deterministic ○ Interactive                            │
└─────────────────────────────────────────────────────────────────┘
```

### Interactions
- **Token Animation**: Blue tokens move along arcs during simulation
- **Playback Controls**: Play/pause/step through execution
- **Speed Control**: Slider to adjust animation speed (0.5x to 5x)
- **Mode Toggle**: Switch between deterministic and interactive execution
- **Step Navigation**: Click on trace events to jump to that simulation state
- **Export Options**: Download trace data in various formats

### Visual Elements
- **Active Tokens**: Blue circles (🔵) showing current token positions
- **Enabled Transitions**: Glowing effect (✨) for transitions that can fire
- **Fired Transitions**: Brief highlight animation when transition fires
- **Trace Timeline**: Chronological list with timestamps and token movements
- **Progress Indicator**: Shows current step in overall execution

### Interactive Mode (Alternative)
```
🔵p_testing
 ┌─┴─┐
t_pass t_fail
 ✨    ✨

Choose next transition:
● t_pass (Tests passed - deploy path)
○ t_fail (Tests failed - alert path)

[Continue] [Cancel]
```

## Screen 5: Complete & Export

### Layout
```
┌─────────────────────────────────────────────────────────────────┐
│ Simulation Complete                            [New Workflow]    │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│ ✅ Execution completed successfully in 4 steps                  │
│                                                                 │
│ Final State:                                                    │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │     ○p_code                                                 │ │
│ │      │                                                     │ │
│ │   t_run_tests                                               │ │
│ │      │                                                     │ │
│ │   ○p_testing                                                │ │
│ │    ┌─┴─┐                                                   │ │
│ │ t_pass t_fail                                               │ │
│ │    │     │                                                 │ │
│ │ ○p_pass ○p_fail                                             │ │
│ │    │     │                                                 │ │
│ │ t_deploy t_notify                                           │ │
│ │    │     │                                                 │ │
│ │ ○p_deployed ○p_alerted                                      │ │
│ │    └──┬──┘                                                 │ │
│ │    t_finish                                                 │ │
│ │       │                                                    │ │
│ │    🔵p_done                                                 │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│ Execution Summary:                                              │
│ • Path taken: Deploy Success (tests passed)                    │
│ • Total steps: 4                                              │
│ • Duration: 2.367 seconds (simulated)                         │
│ • Tokens processed: 1                                         │
│                                                                 │
│ Export Artifacts:                                              │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ 📄 Intent Specification (JSON)        [Download]           │ │
│ │ 🕸️ Petri Net Model (JSON)             [Download]           │ │
│ │ ✅ Validation Report (JSON)            [Download]           │ │
│ │ 📊 DAG Representation (JSON)          [Download]           │ │
│ │ 📝 Execution Trace (ND-JSON)          [Download]           │ │
│ │ 🎨 Mermaid Diagrams (MD)              [Download]           │ │
│ │ 📋 Complete Package (ZIP)             [Download All]       │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│                    [Try Another Workflow]                       │
└─────────────────────────────────────────────────────────────────┘
```

### Interactions
- **Individual Downloads**: Click to download specific artifacts
- **Bulk Download**: Download all artifacts as a ZIP package
- **Preview**: Hover to see artifact preview/summary
- **New Workflow**: Reset application for another workflow design session
- **Share**: Generate shareable link to workflow (future feature)

### Export Formats
- **JSON**: Machine-readable format for integration
- **Mermaid**: Human-readable diagrams for documentation
- **ND-JSON**: Newline-delimited JSON for trace analysis
- **ZIP**: Complete package with all artifacts and README

## Responsive Design Considerations

### Mobile/Tablet Layout
- **Stacked Views**: Petri net above, DAG below on smaller screens
- **Collapsible Panels**: Trace viewer and controls can be minimized
- **Touch Interactions**: Tap to select, pinch to zoom, swipe to pan
- **Simplified Controls**: Larger buttons, reduced complexity

### Accessibility Features
- **Keyboard Navigation**: Full functionality without mouse
- **Screen Reader**: ARIA labels and descriptions for all elements
- **High Contrast**: Alternative color scheme for visual impairments
- **Color-Blind Friendly**: Shape + color encoding for all states

## Error States & Edge Cases

### Parsing Errors
```
❌ Pattern not recognized

The text "do something complex" doesn't match our supported templates.

Try these patterns:
• "Every time X: do Y; if Z then A; if W then B"
• "First X, then Y and Z in parallel, then W"

[Show More Examples] [Contact Support]
```

### Validation Failures
```
❌ VALIDATION FAILED

Deadlock detected at step 3:
• Current state: {p_passing: 1, p_shooting: 0}
• No enabled transitions
• Terminal state not reached

💡 Fix: Add synchronization before cooldown
[Highlight Problem] [Show Fix] [Try Again]
```

### Simulation Errors
```
⚠️ Simulation stopped unexpectedly

Reached maximum steps (1000) without completion.
This may indicate an infinite loop in your workflow.

[View Trace] [Adjust Limits] [Report Issue]
```

## Performance Indicators

### Loading States
- **Parsing**: "Analyzing workflow pattern..."
- **Building**: "Constructing Petri net..."
- **Validating**: "Checking workflow properties... (45/200 states)"
- **Simulating**: "Running simulation... (step 12/∞)"

### Progress Feedback
- **Validation**: Progress bar showing states explored
- **Simulation**: Step counter and estimated completion
- **Export**: File generation progress for large artifacts

This storyboard provides a complete user journey through the Petri Net DAG system, emphasizing clarity, feedback, and educational value while maintaining the formal rigor required for workflow validation.