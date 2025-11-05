# Demo Script: 60-Second Petri Net DAG Demonstration

## Overview

This script provides a complete 60-second demonstration of the Petri Net DAG system, showcasing the full flow from natural language input to validated, simulated workflows. The demo includes both DevOps and Football scenarios with intentional validation failures to demonstrate error handling.

## Primary Demo Script (DevOps CI/CD)

### Setup (5 seconds)
**[Screen: Application loads with clean interface]**

**Narrator**: "Welcome to Obvian's Petri Net DAG system. We're going to transform a natural language workflow description into a formally verified, executable model in under 60 seconds."

### Input & Parsing (10 seconds)
**[Screen: Prompt input field is active]**

**Action**: Type in input field
**Text**: `"Every time I push code: run tests; if pass deploy to staging; if fail alert Slack"`

**Narrator**: "I'll start with a common DevOps workflow. As I type, the system recognizes this as a CI/CD pattern with conditional branching."

**[Screen: Green checkmark appears with parsing success message]**
**Display**: "✅ Parsing successful! Found DevOps CI/CD pattern"

### Petri Net Construction (10 seconds)
**[Screen: Click "Build Petri Net" button]**

**Narrator**: "The system converts this into a formal Petri net with places representing states and transitions representing actions."

**[Screen: Dual view appears showing Petri net on left, DAG on right]**
**Visual**: 
- Petri net shows: p_code → t_run_tests → p_testing → [t_pass, t_fail] → [p_pass, p_fail] → [t_deploy, t_notify] → p_done
- DAG shows simplified flow with same transitions

**Narrator**: "Notice how the Petri net captures the complete formal model while the DAG provides a simplified view for stakeholders."

### Formal Validation (10 seconds)
**[Screen: Click "Validate" button]**

**Narrator**: "Now we perform formal verification to ensure this workflow is mathematically sound."

**[Screen: Validation results appear with green checkmarks]**
**Display**: 
- "✅ Deadlock Detection: PASS"
- "✅ Reachability Analysis: PASS - Terminal state reachable via both paths"
- "✅ Liveness Check: PASS"
- "✅ Boundedness Check: PASS"

**Narrator**: "All checks pass. The workflow is guaranteed to complete successfully regardless of test outcomes."

### Simulation & Tracing (15 seconds)
**[Screen: Click "Start Simulation"]**

**Narrator**: "Let's simulate execution with complete observability."

**[Screen: Token animation begins]**
**Visual Sequence**:
1. Blue token appears at p_code
2. Token moves through t_run_tests to p_testing
3. Token moves through t_pass to p_pass (deterministic choice)
4. Token moves through t_deploy to p_deployed
5. Token moves through t_finish to p_done

**[Screen: Trace panel shows real-time events]**
**Display**:
- "Step 1: t_run_tests - p_code → p_testing"
- "Step 2: t_pass - p_testing → p_pass (deterministic)"
- "Step 3: t_deploy - p_pass → p_deployed"
- "Step 4: t_finish - p_deployed → p_done"

**Narrator**: "Every step is traced with complete causality. The deterministic mode ensures reproducible execution for debugging and compliance."

### Export & Conclusion (10 seconds)
**[Screen: Simulation completes, export panel appears]**

**Narrator**: "The system generates complete artifacts: JSON models, validation reports, execution traces, and Mermaid diagrams for documentation."

**[Screen: Show export options]**
**Display**: Multiple download buttons for different artifact types

**Narrator**: "From natural language to formally verified, traceable workflows in 60 seconds. This is how Obvian provides a horizontal trust layer for reliable automation."

**[Screen: Final summary with key benefits]**
**Display**: "✅ Parsed ✅ Validated ✅ Simulated ✅ Traced ✅ Exported"

---

## Alternative Demo Script (Football Training)

### Setup (5 seconds)
**[Screen: Application loads, previous demo cleared]**

**Narrator**: "Let's try a different domain - sports training - to show the system's versatility."

### Input & Parsing (10 seconds)
**[Screen: Type in input field]**
**Text**: `"Warm-up, then pass and shoot in parallel, then cooldown"`

**Narrator**: "This football drill has parallel activities that must synchronize before completion."

**[Screen: Parsing success with different pattern recognition]**
**Display**: "✅ Parsing successful! Found parallel execution with synchronization"

### Petri Net Construction (10 seconds)
**[Screen: Build Petri net]**

**Narrator**: "The system creates an AND-split for parallel activities and an AND-join for synchronization."

**[Screen: Dual view shows different structure]**
**Visual**:
- Petri net: p_start → t_warmup → p_warmed_up → [t_pass, t_shoot] → [p_passing, p_shooting] → t_cooldown → p_complete
- DAG: Linear view with parallel branches converging

### Validation with Error (15 seconds)
**[Screen: Modify the workflow to remove synchronization]**
**Action**: Edit to create intentional error

**Narrator**: "Let me introduce an error to show the validation capabilities."

**[Screen: Click validate, red error appears]**
**Display**: 
- "❌ Deadlock Detection: FAIL"
- "Deadlock found: cooldown reachable without completing both activities"
- "💡 Suggested Fix: Add AND-join before t_cooldown"

**Narrator**: "The system caught a critical flaw - players could start cooldown without finishing both drills. The formal verification prevents this error from reaching production."

### Fix & Success (10 seconds)
**[Screen: Restore proper synchronization]**

**Narrator**: "After fixing the synchronization..."

**[Screen: Validation passes, simulation runs]**
**Visual**: Tokens flow through parallel branches, synchronize at AND-join, then proceed to cooldown

**Narrator**: "Perfect. Both activities complete before cooldown begins, ensuring proper training protocol."

### Conclusion (10 seconds)
**Narrator**: "Whether DevOps pipelines or training protocols, Obvian transforms natural language into formally verified, traceable workflows. Mathematical guarantees meet human intuition."

---

## Error Demonstration Script (30 seconds)

### Intentional Parsing Error (10 seconds)
**[Screen: Type unsupported pattern]**
**Text**: `"Do something really complex with multiple undefined steps"`

**[Screen: Red error message appears]**
**Display**: 
- "❌ Pattern not recognized"
- "Try: 'Every time X: do Y; if Z then A'"
- "Or: 'First X, then Y and Z in parallel, then W'"

**Narrator**: "Clear error messages guide users toward supported patterns."

### Validation Failure (10 seconds)
**[Screen: Load pre-built problematic workflow]**

**[Screen: Validation fails with detailed diagnostics]**
**Display**:
- "❌ Reachability Analysis: FAIL"
- "Terminal state unreachable from initial marking"
- "Witness: {p_stuck: 1} with no enabled transitions"

**Narrator**: "Formal verification catches issues that would cause runtime failures."

### Simulation Bounds (10 seconds)
**[Screen: Load workflow with potential infinite loop]**

**[Screen: Simulation hits step limit]**
**Display**: 
- "⚠️ Simulation stopped: Maximum steps (1000) reached"
- "Possible infinite loop detected"
- "Review workflow for missing termination conditions"

**Narrator**: "Safety bounds prevent resource exhaustion while providing diagnostic information."

---

## Technical Demo Script (Advanced Features)

### Cross-Highlighting (15 seconds)
**[Screen: Hover over elements in both views]**

**Narrator**: "The dual visualization maintains perfect synchronization. Hovering over a transition in the DAG highlights the corresponding element in the Petri net."

**Visual**: Elements glow in both views simultaneously

### Interactive Simulation (15 seconds)
**[Screen: Switch to interactive mode]**

**Narrator**: "Interactive mode lets users make choices at decision points."

**[Screen: Choice dialog appears]**
**Display**: 
- "Choose next transition:"
- "● t_pass (Tests passed - deploy path)"
- "○ t_fail (Tests failed - alert path)"

**Visual**: User selects t_fail, simulation follows alert path

### Trace Analysis (15 seconds)
**[Screen: Show detailed trace viewer]**

**Narrator**: "Complete execution traces provide audit trails for compliance and debugging."

**[Screen: Trace events with timestamps and causality]**
**Display**: Detailed JSON trace with correlation IDs and metadata

### Export Integration (15 seconds)
**[Screen: Show various export formats]**

**Narrator**: "Multiple export formats enable integration with existing tools and documentation systems."

**[Screen: Download Mermaid diagram, show in external viewer]**
**Visual**: Generated diagram opens in documentation tool

---

## Presentation Notes

### Timing Guidelines
- **Total Demo**: 60 seconds maximum
- **Setup**: Keep minimal, focus on value demonstration
- **Transitions**: Smooth, no dead time between sections
- **Conclusion**: Strong summary of key benefits

### Visual Emphasis
- **Token Animation**: Ensure clearly visible, appropriate speed
- **Error States**: Use contrasting colors, clear messaging
- **Cross-Highlighting**: Obvious visual connection between views
- **Progress Indicators**: Show system is working during longer operations

### Audience Adaptation
- **Technical Audience**: Emphasize formal verification, algorithm details
- **Business Audience**: Focus on reliability, compliance, documentation
- **Mixed Audience**: Balance technical depth with business value

### Backup Scenarios
- **Network Issues**: Have offline version ready
- **Performance Problems**: Pre-loaded examples to avoid delays
- **Browser Compatibility**: Test on multiple browsers beforehand

### Key Messages
1. **Natural Language Accessibility**: Non-technical users can create formal models
2. **Mathematical Rigor**: Formal verification provides guarantees
3. **Complete Observability**: Every execution step is traced and auditable
4. **Domain Agnostic**: Same engine works across different workflow types
5. **Integration Ready**: Multiple export formats for existing tool chains

### Success Metrics
- **Audience Engagement**: Questions about implementation and use cases
- **Technical Understanding**: Recognition of formal methods value
- **Business Interest**: Inquiries about production deployment
- **Follow-up Actions**: Requests for deeper technical discussions

This demo script provides multiple options for different audiences and time constraints while maintaining focus on the core value proposition of reliable, traceable workflow automation through formal methods.