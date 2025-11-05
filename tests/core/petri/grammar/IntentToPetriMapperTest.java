package core.petri.grammar;

import core.petri.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for IntentToPetriMapper.
 *
 * Tests cover:
 * - Individual step type mappings
 * - Dependency handling
 * - Naming strategies
 * - Edge cases and error conditions
 *
 * @author Obvian Labs
 * @since Task 2 - AutomationGrammar Implementation
 */
class IntentToPetriMapperTest {

    private IntentToPetriMapper mapper;
    private AutomationGrammar.TransformationContext context;
    private PetriNet.Builder builder;

    @BeforeEach
    void setUp() {
        mapper = new IntentToPetriMapper();

        // Create mock intent spec for context
        PetriIntentSpec intentSpec = PetriIntentSpec.builder()
                .name("Test Intent")
                .description("Test intent for mapping")
                .build();
        context = new AutomationGrammar.TransformationContext(intentSpec);
        builder = PetriNet.builder();
    }

    @Nested
    @DisplayName("ACTION Step Mapping")
    class ActionStepMapping {

        @Test
        @DisplayName("Map simple ACTION step creates place-transition-place pattern")
        void mapSimpleActionStepCreatesPattern() {
            // Given: Simple action step
            PetriIntentSpec.IntentStep actionStep = new PetriIntentSpec.IntentStep(
                    "test_action", PetriIntentSpec.StepType.ACTION, "Test action",
                    new ArrayList<>(), new HashMap<>(), null, new HashMap<>()
            );

            // When: Map the step
            mapper.mapStep(actionStep, context, builder);
            PetriNet petriNet = builder.build();

            // Then: Verify pattern
            assertThat(petriNet.getPlaces()).hasSize(2);
            assertThat(petriNet.getTransitions()).hasSize(1);
            assertThat(petriNet.getArcs()).hasSize(2);

            // Verify places have correct metadata
            List<Place> places = petriNet.getPlaces();
            for (Place place : places) {
                assertThat(place.getMetadata().get("stepId")).isEqualTo("test_action");
                assertThat(place.getMetadata().get("stepType")).isEqualTo("ACTION");
            }

            // Verify transition
            Transition transition = petriNet.getTransitions().get(0);
            assertThat(transition.getDescription()).isEqualTo("Test action");
            assertThat(transition.getMetadata().get("stepId")).isEqualTo("test_action");
        }

        @Test
        @DisplayName("Map ACTION step with guard condition sets guard on transition")
        void mapActionStepWithGuardCondition() {
            // Given: Action step with guard condition
            PetriIntentSpec.IntentStep guardedStep = new PetriIntentSpec.IntentStep(
                    "guarded_action", PetriIntentSpec.StepType.ACTION, "Guarded action",
                    new ArrayList<>(), new HashMap<>(), "condition == true", new HashMap<>()
            );

            // When: Map the step
            mapper.mapStep(guardedStep, context, builder);
            PetriNet petriNet = builder.build();

            // Then: Verify guard is set
            Transition transition = petriNet.getTransitions().get(0);
            assertThat(transition.hasGuard()).isTrue();
            assertThat(transition.getGuard()).isEqualTo("condition == true");
        }

        @Test
        @DisplayName("Map ACTION step with dependencies creates connection arcs")
        void mapActionStepWithDependencies() {
            // Given: Action step with dependency
            // First create the dependency step
            PetriIntentSpec.IntentStep dependency = new PetriIntentSpec.IntentStep(
                    "dep_action", PetriIntentSpec.StepType.ACTION, "Dependency action",
                    new ArrayList<>(), new HashMap<>(), null, new HashMap<>()
            );

            // Map dependency first
            mapper.mapStep(dependency, context, builder);

            // Now create step that depends on it
            PetriIntentSpec.IntentStep dependentStep = new PetriIntentSpec.IntentStep(
                    "dependent_action", PetriIntentSpec.StepType.ACTION, "Dependent action",
                    Arrays.asList("dep_action"), new HashMap<>(), null, new HashMap<>()
            );

            // When: Map the dependent step
            mapper.mapStep(dependentStep, context, builder);
            PetriNet petriNet = builder.build();

            // Then: Verify connection
            assertThat(petriNet.getArcs()).hasSizeGreaterThan(4); // Base 4 + dependency connection

            // Should have connection from dep_action's output to dependent_action's input
            // This would be verified by checking arc connectivity
        }

        @Test
        @DisplayName("Map ACTION step with no dependencies gets initial token")
        void mapActionStepWithNoDependenciesGetsInitialToken() {
            // Given: Action step with no dependencies
            PetriIntentSpec.IntentStep startStep = new PetriIntentSpec.IntentStep(
                    "start_action", PetriIntentSpec.StepType.ACTION, "Start action",
                    new ArrayList<>(), new HashMap<>(), null, new HashMap<>()
            );

            // When: Map the step
            mapper.mapStep(startStep, context, builder);
            PetriNet petriNet = builder.build();

            // Then: Verify initial token
            assertThat(petriNet.getInitialMarking().getTotalTokens()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("CHOICE Step Mapping")
    class ChoiceStepMapping {

        @Test
        @DisplayName("Map CHOICE step creates XOR branching structure")
        void mapChoiceStepCreatesXORBranching() {
            // Given: Choice step with multiple paths
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("paths", Arrays.asList("Path A", "Path B", "Path C"));

            PetriIntentSpec.IntentStep choiceStep = new PetriIntentSpec.IntentStep(
                    "choice1", PetriIntentSpec.StepType.CHOICE, "Make choice",
                    new ArrayList<>(), new HashMap<>(), null, metadata
            );

            // When: Map the step
            mapper.mapStep(choiceStep, context, builder);
            PetriNet petriNet = builder.build();

            // Then: Verify XOR structure
            assertThat(petriNet.getPlaces()).hasSize(4); // 1 pre-place + 3 path places
            assertThat(petriNet.getTransitions()).hasSize(3); // 3 choice transitions

            // Verify all transitions are marked as choice
            List<Transition> transitions = petriNet.getTransitions();
            for (Transition t : transitions) {
                assertThat(t.isChoice()).isTrue();
                assertThat(t.getMetadata().get("stepType")).isEqualTo("CHOICE");
            }

            // Verify path metadata
            for (int i = 0; i < 3; i++) {
                final int pathIndex = i;
                boolean foundPathTransition = transitions.stream()
                        .anyMatch(t -> Integer.valueOf(pathIndex).equals(t.getMetadata().get("pathIndex")));
                assertThat(foundPathTransition).isTrue();
            }
        }

        @Test
        @DisplayName("Map CHOICE step with empty paths handles gracefully")
        void mapChoiceStepWithEmptyPaths() {
            // Given: Choice step with no paths
            PetriIntentSpec.IntentStep choiceStep = new PetriIntentSpec.IntentStep(
                    "empty_choice", PetriIntentSpec.StepType.CHOICE, "Empty choice",
                    new ArrayList<>(), new HashMap<>(), null, new HashMap<>()
            );

            // When: Map the step
            mapper.mapStep(choiceStep, context, builder);
            PetriNet petriNet = builder.build();

            // Then: Should still create basic structure
            assertThat(petriNet.getPlaces()).hasSize(1); // Just pre-place
            assertThat(petriNet.getTransitions()).hasSize(0); // No choice transitions
        }
    }

    @Nested
    @DisplayName("PARALLEL Step Mapping")
    class ParallelStepMapping {

        @Test
        @DisplayName("Map PARALLEL step creates AND-split structure")
        void mapParallelStepCreatesANDSplit() {
            // Given: Parallel step
            PetriIntentSpec.IntentStep parallelStep = new PetriIntentSpec.IntentStep(
                    "parallel1", PetriIntentSpec.StepType.PARALLEL, "Parallel execution",
                    new ArrayList<>(), new HashMap<>(), null, new HashMap<>()
            );

            // When: Map the step
            mapper.mapStep(parallelStep, context, builder);
            PetriNet petriNet = builder.build();

            // Then: Verify AND-split structure
            assertThat(petriNet.getPlaces()).hasSize(3); // 1 pre-place + 2 branch places
            assertThat(petriNet.getTransitions()).hasSize(1); // 1 fork transition

            // Verify fork transition
            Transition forkTransition = petriNet.getTransitions().get(0);
            assertThat(forkTransition.isFork()).isTrue();
            assertThat(forkTransition.getMetadata().get("stepType")).isEqualTo("PARALLEL");

            // Verify branch places
            List<Place> places = petriNet.getPlaces();
            long branchPlaces = places.stream()
                    .filter(p -> p.getMetadata().containsKey("branchIndex"))
                    .count();
            assertThat(branchPlaces).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("SYNC Step Mapping")
    class SyncStepMapping {

        @Test
        @DisplayName("Map SYNC step creates AND-join structure")
        void mapSyncStepCreatesANDJoin() {
            // Given: Sync step with dependencies
            PetriIntentSpec.IntentStep syncStep = new PetriIntentSpec.IntentStep(
                    "sync1", PetriIntentSpec.StepType.SYNC, "Synchronize",
                    Arrays.asList("parallel1"), new HashMap<>(), null, new HashMap<>()
            );

            // When: Map the step
            mapper.mapStep(syncStep, context, builder);
            PetriNet petriNet = builder.build();

            // Then: Verify AND-join structure
            assertThat(petriNet.getPlaces()).hasSize(1); // Post-place
            assertThat(petriNet.getTransitions()).hasSize(1); // Join transition

            // Verify join transition
            Transition joinTransition = petriNet.getTransitions().get(0);
            assertThat(joinTransition.isJoin()).isTrue();
            assertThat(joinTransition.getMetadata().get("stepType")).isEqualTo("SYNC");
        }
    }

    @Nested
    @DisplayName("Configuration and Naming")
    class ConfigurationAndNaming {

        @Test
        @DisplayName("Configure mapper with minimal naming strategy")
        void configureMapperWithMinimalNaming() {
            // Given: Minimal naming configuration
            AutomationGrammar.TransformationConfig config = new AutomationGrammar.TransformationConfig();
            config.setNamingStrategy("minimal");
            mapper.configure(config);

            PetriIntentSpec.IntentStep actionStep = new PetriIntentSpec.IntentStep(
                    "test_action", PetriIntentSpec.StepType.ACTION, "Very long description that should be shortened in descriptive mode",
                    new ArrayList<>(), new HashMap<>(), null, new HashMap<>()
            );

            // When: Map the step
            mapper.mapStep(actionStep, context, builder);
            PetriNet petriNet = builder.build();

            // Then: Names should be minimal
            Transition transition = petriNet.getTransitions().get(0);
            // In minimal mode, name should be just the step ID
            assertThat(transition.getName()).isEqualTo("test_action");
        }

        @Test
        @DisplayName("Configure mapper with descriptive naming strategy")
        void configureMapperWithDescriptiveNaming() {
            // Given: Descriptive naming configuration (default)
            AutomationGrammar.TransformationConfig config = new AutomationGrammar.TransformationConfig();
            config.setNamingStrategy("descriptive");
            mapper.configure(config);

            PetriIntentSpec.IntentStep actionStep = new PetriIntentSpec.IntentStep(
                    "test_action", PetriIntentSpec.StepType.ACTION, "Short description",
                    new ArrayList<>(), new HashMap<>(), null, new HashMap<>()
            );

            // When: Map the step
            mapper.mapStep(actionStep, context, builder);
            PetriNet petriNet = builder.build();

            // Then: Names should be descriptive
            Transition transition = petriNet.getTransitions().get(0);
            // In descriptive mode, name should use the description
            assertThat(transition.getName()).isEqualTo("Short description");
        }
    }

    @Nested
    @DisplayName("Context Management")
    class ContextManagement {

        @Test
        @DisplayName("Mapping step updates transformation context")
        void mappingStepUpdatesTransformationContext() {
            // Given: Action step
            PetriIntentSpec.IntentStep actionStep = new PetriIntentSpec.IntentStep(
                    "context_test", PetriIntentSpec.StepType.ACTION, "Context test",
                    new ArrayList<>(), new HashMap<>(), null, new HashMap<>()
            );

            // When: Map the step
            mapper.mapStep(actionStep, context, builder);

            // Then: Context should be updated
            assertThat(context.isStepProcessed("context_test")).isTrue();
            assertThat(context.getStepToPlaceMap()).isNotEmpty();
            assertThat(context.getStepToTransitionMap()).isNotEmpty();

            // Verify specific mappings exist
            assertThat(context.getStepToPlaceMap()).containsKey("context_test_pre");
            assertThat(context.getStepToPlaceMap()).containsKey("context_test_post");
            assertThat(context.getStepToTransitionMap()).containsKey("context_test");
        }

        @Test
        @DisplayName("Context generates unique IDs for multiple steps")
        void contextGeneratesUniqueIDsForMultipleSteps() {
            // Given: Multiple action steps
            PetriIntentSpec.IntentStep step1 = new PetriIntentSpec.IntentStep(
                    "step1", PetriIntentSpec.StepType.ACTION, "Step 1",
                    new ArrayList<>(), new HashMap<>(), null, new HashMap<>()
            );

            PetriIntentSpec.IntentStep step2 = new PetriIntentSpec.IntentStep(
                    "step2", PetriIntentSpec.StepType.ACTION, "Step 2",
                    new ArrayList<>(), new HashMap<>(), null, new HashMap<>()
            );

            // When: Map both steps
            mapper.mapStep(step1, context, builder);
            mapper.mapStep(step2, context, builder);

            PetriNet petriNet = builder.build();

            // Then: All places and transitions should have unique IDs
            Set<String> placeIds = petriNet.getPlaces().stream()
                    .map(Place::getId)
                    .collect(HashSet::new, Set::add, Set::addAll);
            assertThat(placeIds).hasSize(petriNet.getPlaces().size());

            Set<String> transitionIds = petriNet.getTransitions().stream()
                    .map(Transition::getId)
                    .collect(HashSet::new, Set::add, Set::addAll);
            assertThat(transitionIds).hasSize(petriNet.getTransitions().size());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesAndErrorHandling {

        @Test
        @DisplayName("Map step with null type throws exception")
        void mapStepWithNullTypeThrowsException() {
            // Given: Step with null type
            PetriIntentSpec.IntentStep nullTypeStep = new PetriIntentSpec.IntentStep(
                    "null_type", null, "Null type step",
                    new ArrayList<>(), new HashMap<>(), null, new HashMap<>()
            );

            // When/Then: Should throw exception
            assertThatThrownBy(() -> mapper.mapStep(nullTypeStep, context, builder))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unsupported step type: null");
        }

        @Test
        @DisplayName("Map SEQUENCE step handles gracefully")
        void mapSequenceStepHandlesGracefully() {
            // Given: Sequence step (which is more of a meta-step)
            PetriIntentSpec.IntentStep sequenceStep = new PetriIntentSpec.IntentStep(
                    "sequence1", PetriIntentSpec.StepType.SEQUENCE, "Sequential flow",
                    new ArrayList<>(), new HashMap<>(), null, new HashMap<>()
            );

            // When: Map the step (should not throw)
            assertThatCode(() -> mapper.mapStep(sequenceStep, context, builder))
                    .doesNotThrowAnyException();

            // Then: Should create at least a sync place
            PetriNet petriNet = builder.build();
            assertThat(petriNet.getPlaces()).hasSize(1);
        }

        @Test
        @DisplayName("Map step with circular dependency reference handles gracefully")
        void mapStepWithCircularDependencyHandlesGracefully() {
            // Given: Step that references non-existent dependency
            PetriIntentSpec.IntentStep stepWithBadDep = new PetriIntentSpec.IntentStep(
                    "bad_dep_step", PetriIntentSpec.StepType.ACTION, "Bad dependency",
                    Arrays.asList("non_existent_step"), new HashMap<>(), null, new HashMap<>()
            );

            // When: Map the step (should not throw)
            assertThatCode(() -> mapper.mapStep(stepWithBadDep, context, builder))
                    .doesNotThrowAnyException();

            // Then: Should still create basic structure (dependency connection will be skipped)
            PetriNet petriNet = builder.build();
            assertThat(petriNet.getPlaces()).hasSize(2); // Pre and post places
            assertThat(petriNet.getTransitions()).hasSize(1);
        }
    }
}