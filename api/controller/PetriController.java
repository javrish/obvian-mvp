package api.controller;

import api.dto.*;
import core.PromptParser;
import core.DAG;
import core.TaskNode;
import core.petri.*;
import core.petri.grammar.AutomationGrammar;
import core.petri.projection.PetriToDagProjector;
import core.petri.simulation.PetriTokenSimulator;
import core.petri.simulation.SimulationConfig;
import core.petri.simulation.SimulationResult;
import core.petri.simulation.TraceEvent;
import core.petri.validation.PetriNetValidator;
// Temporarily disabled: import core.petri.execution.P3NetExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Simplified REST controller for Petri net workflow processing endpoints.
 * Testing version without circuit breaker complexity.
 */
@RestController
@RequestMapping("/api/v1/petri")
@Tag(name = "Petri Net Workflows", description = "Petri net workflow processing endpoints")
public class PetriController {

    private static final Logger logger = LoggerFactory.getLogger(PetriController.class);
    private static final String SCHEMA_VERSION = "1.0";

    private final PromptParser promptParser;
    private final AutomationGrammar automationGrammar;
    private final PetriNetValidator petriNetValidator;
    private final PetriTokenSimulator petriTokenSimulator;
    private final PetriToDagProjector petriToDagProjector;
    // Temporarily disabled: private final P3NetExecutionService p3NetExecutionService;

    public PetriController(AutomationGrammar automationGrammar,
                          PetriNetValidator petriNetValidator,
                          PetriTokenSimulator petriTokenSimulator,
                          PetriToDagProjector petriToDagProjector) {
        this.promptParser = new PromptParser();
        this.automationGrammar = automationGrammar;
        this.petriNetValidator = petriNetValidator;
        this.petriTokenSimulator = petriTokenSimulator;
        this.petriToDagProjector = petriToDagProjector;
    }

    /**
     * Parse natural language into PetriIntentSpec
     * POST /api/v1/petri/parse
     */
    @PostMapping("/parse")
    @Operation(summary = "Parse natural language into Petri intent specification",
               description = "Converts natural language workflow descriptions into structured intent specifications")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully parsed natural language"),
        @ApiResponse(responseCode = "400", description = "Invalid input text"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> parseNaturalLanguage(
            @Parameter(description = "Natural language parsing request", required = true)
            @Valid @RequestBody PetriParseRequest request) {

        logger.info("Parsing natural language: {}", request.getText().substring(0, Math.min(request.getText().length(), 100)));

        try {
            // Parse natural language using real PromptParser
            PromptParser.CompoundParseResult parseResult = promptParser.parseCompoundPrompt(request.getText());
            List<PromptParser.ParsedIntent> intents = parseResult.getIntents();

            // Convert parsed intents to PetriIntentSpec
            PetriIntentSpec.Builder intentBuilder = PetriIntentSpec.builder()
                    .name("ParsedWorkflow")
                    .description("Workflow parsed from: " + request.getText())
                    .originalPrompt(request.getText())
                    .templateId(request.getTemplateHint() != null ? request.getTemplateHint() : "generic-workflow");

            // Convert parsed intents to intent steps
            int stepCounter = 1;
            String previousStepId = null;

            for (PromptParser.ParsedIntent intent : intents) {
                String stepId = "step" + stepCounter++;
                String description = getDescriptionForAction(intent.getAction(), intent.getParameters());

                PetriIntentSpec.IntentStep step = new PetriIntentSpec.IntentStep(
                    stepId,
                    PetriIntentSpec.StepType.ACTION,
                    description,
                    previousStepId != null ? Arrays.asList(previousStepId) : new ArrayList<>(),
                    new HashMap<>(),
                    null,
                    new HashMap<>(intent.getParameters()),
                    null, // loopCondition
                    new HashMap<>(), // errorHandling
                    new ArrayList<>(), // compensation
                    null, // timeout
                    new HashMap<>(), // retryPolicy
                    new HashMap<>() // resourceConstraints
                );

                intentBuilder.addStep(step);
                previousStepId = stepId;
            }

            PetriIntentSpec intentSpec = intentBuilder.build();

            // Convert to response format
            Map<String, Object> intentMap = convertIntentSpecToMap(intentSpec);

            Map<String, Object> response = new HashMap<>();
            response.put("schemaVersion", SCHEMA_VERSION);
            response.put("intent", intentMap);
            response.put("templateUsed", intentSpec.getTemplateId());
            response.put("confidence", calculateConfidence(parseResult));
            response.put("success", true);

            logger.info("Successfully parsed input into {} intents", intents.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error during parsing", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("schemaVersion", SCHEMA_VERSION);
            errorResponse.put("error", Map.of(
                "code", "PARSE_ERROR",
                "message", "Failed to parse natural language: " + e.getMessage()
            ));
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Build Petri net from intent specification
     * POST /api/v1/petri/build
     */
    @PostMapping("/build")
    @Operation(summary = "Build Petri net from intent specification",
               description = "Converts intent specification into executable Petri net structure")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully built Petri net"),
        @ApiResponse(responseCode = "400", description = "Invalid intent specification"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> buildPetriNet(
            @Parameter(description = "Build request with intent specification", required = true)
            @Valid @RequestBody Map<String, Object> request) {

        logger.info("Building Petri net from intent specification");

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> intentMap = (Map<String, Object>) request.get("intent");

            if (intentMap == null) {
                throw new IllegalArgumentException("Intent specification is required");
            }

            // Convert intent map back to PetriIntentSpec
            PetriIntentSpec intentSpec = convertMapToIntentSpec(intentMap);

            // Use AutomationGrammar to transform intent to Petri net
            PetriNet petriNet = automationGrammar.transform(intentSpec);

            Map<String, Object> response = new HashMap<>();
            response.put("schemaVersion", SCHEMA_VERSION);
            response.put("success", true);
            response.put("petriNet", convertPetriNetToMap(petriNet));

            logger.info("Successfully built Petri net: {} places, {} transitions",
                    petriNet.getPlaces().size(), petriNet.getTransitions().size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error building Petri net", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("schemaVersion", SCHEMA_VERSION);
            errorResponse.put("error", Map.of(
                "code", "BUILD_ERROR",
                "message", "Failed to build Petri net: " + e.getMessage()
            ));
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Project Petri net to DAG representation
     * POST /api/v1/petri/dag
     */
    @PostMapping("/dag")
    @Operation(summary = "Project Petri net to DAG",
               description = "Converts Petri net structure into DAG representation for execution")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully projected to DAG"),
        @ApiResponse(responseCode = "400", description = "Invalid Petri net"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> projectToDAG(
            @Parameter(description = "DAG projection request with Petri net", required = true)
            @Valid @RequestBody Map<String, Object> request) {

        logger.info("Projecting Petri net to DAG using formal projection algorithm");

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> petriNetMap = (Map<String, Object>) request.get("petriNet");

            if (petriNetMap == null) {
                throw new IllegalArgumentException("Petri net is required");
            }

            // Reconstruct PetriNet object from map (needed for formal projector)
            PetriNet petriNet = reconstructPetriNetFromMap(petriNetMap);

            // Use formal PetriToDagProjector with transitive reduction algorithm
            DAG dag = petriToDagProjector.projectToDAG(petriNet);

            // Convert DAG to response format
            Map<String, Object> dagMap = convertDagToMap(dag);

            Map<String, Object> response = new HashMap<>();
            response.put("schemaVersion", SCHEMA_VERSION);
            response.put("success", true);
            response.put("dag", dagMap);

            // Add projection metadata
            Map<String, Object> projectionInfo = new HashMap<>();
            projectionInfo.put("algorithm", "transitive-reduction");
            projectionInfo.put("derivedFrom", dag.getDerivedFromPetriNetId());
            projectionInfo.put("nodeCount", dag.getNodes().size());
            response.put("projectionInfo", projectionInfo);

            logger.info("Successfully projected Petri net to DAG using formal algorithm: {} nodes",
                       dag.getNodes().size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error projecting to DAG", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("schemaVersion", SCHEMA_VERSION);
            errorResponse.put("error", Map.of(
                "code", "DAG_PROJECTION_ERROR",
                "message", "Failed to project to DAG: " + e.getMessage()
            ));
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Validate Petri net structure using formal verification
     * POST /api/v1/petri/validate
     */
    @PostMapping("/validate")
    @Operation(summary = "Validate Petri net using formal verification",
               description = "Performs deadlock detection, reachability analysis, liveness checking, and boundedness verification")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Validation completed (check result for pass/fail status)"),
        @ApiResponse(responseCode = "400", description = "Invalid Petri net structure"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> validatePetriNet(
            @Parameter(description = "Validation request with Petri net structure", required = true)
            @Valid @RequestBody Map<String, Object> request) {

        logger.info("Validating Petri net structure");

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> petriNetMap = (Map<String, Object>) request.get("petriNet");

            if (petriNetMap == null) {
                throw new IllegalArgumentException("Petri net is required");
            }

            // Convert map to PetriNet object
            PetriNet petriNet = reconstructPetriNetFromMap(petriNetMap);

            // Extract validation config if provided, otherwise use defaults
            @SuppressWarnings("unchecked")
            Map<String, Object> configMap = (Map<String, Object>) request.get("config");
            core.petri.validation.PetriNetValidationResult.ValidationConfig config =
                    configMap != null ? convertMapToValidationConfig(configMap) :
                    core.petri.validation.PetriNetValidationResult.ValidationConfig.defaultConfig();

            // Perform validation
            core.petri.validation.PetriNetValidationResult validationResult =
                    petriNetValidator.validate(petriNet, config);

            // Convert validation result to response map
            Map<String, Object> response = new HashMap<>();
            response.put("schemaVersion", SCHEMA_VERSION);
            response.put("success", validationResult.isValid());
            response.put("validationResult", convertValidationResultToMap(validationResult));

            logger.info("Validation completed for Petri net: {} - Status: {}",
                    petriNet.getName(), validationResult.getPetriStatus());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid validation request", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("schemaVersion", SCHEMA_VERSION);
            errorResponse.put("error", Map.of(
                "code", "VALIDATION_ERROR",
                "message", "Invalid request: " + e.getMessage()
            ));
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            logger.error("Error during Petri net validation", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("schemaVersion", SCHEMA_VERSION);
            errorResponse.put("error", Map.of(
                "code", "VALIDATION_ERROR",
                "message", "Failed to validate Petri net: " + e.getMessage()
            ));
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Simulate token flow through Petri net
     * POST /api/v1/petri/simulate
     */
    @PostMapping("/simulate")
    @Operation(summary = "Simulate token flow through Petri net",
               description = "Executes step-by-step simulation with comprehensive trace logging and deadlock detection")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Simulation completed"),
        @ApiResponse(responseCode = "400", description = "Invalid Petri net or simulation config"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> simulatePetriNet(
            @Parameter(description = "Simulation request with Petri net and config", required = true)
            @Valid @RequestBody Map<String, Object> request) {

        logger.info("Starting Petri net token simulation");

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> petriNetMap = (Map<String, Object>) request.get("petriNet");

            if (petriNetMap == null) {
                throw new IllegalArgumentException("Petri net is required");
            }

            // Convert map to PetriNet object
            PetriNet petriNet = reconstructPetriNetFromMap(petriNetMap);

            // Extract simulation config if provided, otherwise use defaults
            @SuppressWarnings("unchecked")
            Map<String, Object> configMap = (Map<String, Object>) request.get("config");
            SimulationConfig config = configMap != null ?
                    convertMapToSimulationConfig(configMap) :
                    SimulationConfig.defaultDeterministic();

            // Execute simulation
            SimulationResult simulationResult = petriTokenSimulator.simulate(petriNet, config);

            // Convert simulation result to response map
            Map<String, Object> response = new HashMap<>();
            response.put("schemaVersion", SCHEMA_VERSION);
            response.put("success", simulationResult.isSuccess());
            response.put("simulationResult", convertSimulationResultToMap(simulationResult));

            logger.info("Simulation completed - Status: {}, Steps: {}",
                    simulationResult.getStatus(), simulationResult.getStepsExecuted());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid simulation request", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("schemaVersion", SCHEMA_VERSION);
            errorResponse.put("error", Map.of(
                "code", "SIMULATION_ERROR",
                "message", "Invalid request: " + e.getMessage()
            ));
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            logger.error("Error during Petri net simulation", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("schemaVersion", SCHEMA_VERSION);
            errorResponse.put("error", Map.of(
                "code", "SIMULATION_ERROR",
                "message", "Failed to simulate Petri net: " + e.getMessage()
            ));
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Health check for Petri net service
     * GET /api/v1/petri/health
     */
    @GetMapping("/health")
    @Operation(summary = "Health check for Petri net service",
               description = "Returns health status of all Petri net processing components")
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "healthy");
        health.put("service", "PetriNetService");
        health.put("timestamp", System.currentTimeMillis());
        health.put("schemaVersion", SCHEMA_VERSION);

        Map<String, String> components = new HashMap<>();

        // Check PromptParser
        try {
            promptParser.parsePrompt("test");
            components.put("promptParser", "healthy");
        } catch (Exception e) {
            components.put("promptParser", "unhealthy: " + e.getMessage());
        }

        // Check AutomationGrammar
        try {
            if (automationGrammar != null) {
                components.put("automationGrammar", "healthy");
            } else {
                components.put("automationGrammar", "not_initialized");
            }
        } catch (Exception e) {
            components.put("automationGrammar", "unhealthy: " + e.getMessage());
        }

        // Check PetriNetValidator
        try {
            if (petriNetValidator != null) {
                components.put("petriNetValidator", "healthy");
            } else {
                components.put("petriNetValidator", "not_initialized");
            }
        } catch (Exception e) {
            components.put("petriNetValidator", "unhealthy: " + e.getMessage());
        }

        // Check PetriTokenSimulator
        try {
            if (petriTokenSimulator != null) {
                components.put("petriTokenSimulator", "healthy");
            } else {
                components.put("petriTokenSimulator", "not_initialized");
            }
        } catch (Exception e) {
            components.put("petriTokenSimulator", "unhealthy: " + e.getMessage());
        }

        // Check PetriToDagProjector
        try {
            if (petriToDagProjector != null) {
                components.put("petriToDagProjector", "healthy");
            } else {
                components.put("petriToDagProjector", "not_initialized");
            }
        } catch (Exception e) {
            components.put("petriToDagProjector", "unhealthy: " + e.getMessage());
        }

        health.put("components", components);

        // Overall health status
        boolean allHealthy = components.values().stream()
                .allMatch(status -> "healthy".equals(status) || "not_yet_implemented".equals(status));
        if (!allHealthy) {
            health.put("status", "degraded");
        }

        return ResponseEntity.ok(health);
    }

    // Helper methods

    /**
     * Convert intent map from frontend to PetriIntentSpec
     */
    private PetriIntentSpec convertMapToIntentSpec(Map<String, Object> intentMap) {
        String modelType = (String) intentMap.getOrDefault("modelType", "PetriIntentSpec");
        String name = (String) intentMap.getOrDefault("name", "Generated Workflow");
        String description = (String) intentMap.getOrDefault("description", "Workflow from intent");
        String schemaVersion = (String) intentMap.getOrDefault("schemaVersion", "1.0");
        String originalPrompt = (String) intentMap.getOrDefault("originalPrompt", "");
        String templateId = (String) intentMap.getOrDefault("templateId", "generic-workflow");

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) intentMap.getOrDefault("metadata", new HashMap<>());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> stepMaps = (List<Map<String, Object>>) intentMap.get("steps");

        if (stepMaps == null || stepMaps.isEmpty()) {
            throw new IllegalArgumentException("Intent must contain at least one step");
        }

        PetriIntentSpec.Builder builder = PetriIntentSpec.builder()
                .name(name)
                .description(description)
                .originalPrompt(originalPrompt)
                .templateId(templateId);

        // Add metadata
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            builder.addMetadata(entry.getKey(), entry.getValue());
        }

        // Convert step maps to IntentSteps
        for (Map<String, Object> stepMap : stepMaps) {
            String stepId = (String) stepMap.get("id");
            String stepTypeStr = (String) stepMap.get("type");
            String stepDescription = (String) stepMap.get("description");

            @SuppressWarnings("unchecked")
            List<String> dependencies = (List<String>) stepMap.getOrDefault("dependencies", new ArrayList<>());

            @SuppressWarnings("unchecked")
            Map<String, Object> conditions = (Map<String, Object>) stepMap.getOrDefault("conditions", new HashMap<>());

            @SuppressWarnings("unchecked")
            Map<String, Object> stepMetadata = (Map<String, Object>) stepMap.getOrDefault("metadata", new HashMap<>());

            String when = (String) stepMap.get("when");

            PetriIntentSpec.StepType stepType;
            try {
                stepType = PetriIntentSpec.StepType.valueOf(stepTypeStr);
            } catch (Exception e) {
                stepType = PetriIntentSpec.StepType.ACTION; // Default to ACTION
            }

            PetriIntentSpec.IntentStep step = new PetriIntentSpec.IntentStep(
                stepId,
                stepType,
                stepDescription,
                dependencies,
                conditions,
                when,
                stepMetadata,
                null, // loopCondition
                new HashMap<>(), // errorHandling
                new ArrayList<>(), // compensation
                null, // timeout
                new HashMap<>(), // retryPolicy
                new HashMap<>() // resourceConstraints
            );

            builder.addStep(step);
        }

        return builder.build();
    }

    /**
     * Convert parsed action and parameters to human-readable description
     */
    private String getDescriptionForAction(String action, Map<String, Object> parameters) {
        switch (action) {
            case "send_email":
                return "Send email to " + parameters.getOrDefault("recipient", "unknown");
            case "create_file":
                return "Create file " + parameters.getOrDefault("filename", "untitled");
            case "set_reminder":
                return "Set reminder: " + parameters.getOrDefault("task", "reminder");
            case "generic":
                return parameters.getOrDefault("message", "Generic action").toString();
            default:
                return "Execute " + action;
        }
    }

    /**
     * Convert PetriIntentSpec to Map for JSON response
     */
    private Map<String, Object> convertIntentSpecToMap(PetriIntentSpec intentSpec) {
        Map<String, Object> map = new HashMap<>();
        map.put("modelType", intentSpec.getModelType());
        map.put("name", intentSpec.getName());
        map.put("description", intentSpec.getDescription());
        map.put("schemaVersion", intentSpec.getSchemaVersion());
        map.put("originalPrompt", intentSpec.getOriginalPrompt());
        map.put("templateId", intentSpec.getTemplateId());
        map.put("metadata", intentSpec.getMetadata());

        // Convert steps to map format
        List<Map<String, Object>> stepMaps = new ArrayList<>();
        for (PetriIntentSpec.IntentStep step : intentSpec.getSteps()) {
            Map<String, Object> stepMap = new HashMap<>();
            stepMap.put("id", step.getId());
            stepMap.put("type", step.getType().name());
            stepMap.put("description", step.getDescription());
            stepMap.put("dependencies", step.getDependencies());
            stepMap.put("conditions", step.getConditions());
            stepMap.put("metadata", step.getMetadata());
            if (step.getWhen() != null) {
                stepMap.put("when", step.getWhen());
            }
            stepMaps.add(stepMap);
        }
        map.put("steps", stepMaps);

        return map;
    }

    /**
     * Calculate confidence score based on parse result quality
     */
    private double calculateConfidence(PromptParser.CompoundParseResult parseResult) {
        if (parseResult.getIntents().isEmpty()) {
            return 0.0;
        }

        double confidence = 0.7; // Base confidence

        // Higher confidence for recognized patterns
        for (PromptParser.ParsedIntent intent : parseResult.getIntents()) {
            if (!"generic".equals(intent.getAction())) {
                confidence += 0.1;
            }
        }

        return Math.min(confidence, 1.0);
    }

    /**
     * Convert PetriNet to Map for JSON response
     */
    private Map<String, Object> convertPetriNetToMap(PetriNet petriNet) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", petriNet.getName());
        map.put("description", petriNet.getDescription());

        // Convert places
        List<Map<String, Object>> placeMaps = new ArrayList<>();
        for (Place place : petriNet.getPlaces()) {
            Map<String, Object> placeMap = new HashMap<>();
            placeMap.put("id", place.getId());
            placeMap.put("name", place.getName());
            placeMap.put("capacity", place.getCapacity());
            placeMaps.add(placeMap);
        }
        map.put("places", placeMaps);

        // Convert transitions
        List<Map<String, Object>> transitionMaps = new ArrayList<>();
        for (Transition transition : petriNet.getTransitions()) {
            Map<String, Object> transitionMap = new HashMap<>();
            transitionMap.put("id", transition.getId());
            transitionMap.put("name", transition.getName());
            transitionMap.put("description", transition.getDescription());
            transitionMap.put("metadata", transition.getMetadata());
            transitionMaps.add(transitionMap);
        }
        map.put("transitions", transitionMaps);

        // Convert arcs
        List<Map<String, Object>> arcMaps = new ArrayList<>();
        for (Arc arc : petriNet.getArcs()) {
            Map<String, Object> arcMap = new HashMap<>();
            arcMap.put("from", arc.getFrom());
            arcMap.put("to", arc.getTo());
            arcMap.put("weight", arc.getWeight());
            arcMaps.add(arcMap);
        }
        map.put("arcs", arcMaps);

        // Initial marking - extract tokens map for proper JSON serialization
        if (petriNet.getInitialMarking() != null) {
            map.put("initialMarking", petriNet.getInitialMarking().getTokens());
        }

        // Add user-friendly workflow summary (non-technical transitions only)
        List<Map<String, Object>> workflowSteps = new ArrayList<>();
        for (Transition transition : petriNet.getTransitions()) {
            Map<String, Object> metadata = transition.getMetadata();
            boolean isDependencyConnector = metadata != null &&
                    Boolean.TRUE.equals(metadata.get("isDependencyConnector"));

            if (!isDependencyConnector) {
                Map<String, Object> step = new HashMap<>();
                step.put("name", transition.getName());
                step.put("description", transition.getDescription());
                step.put("stepType", metadata != null ? metadata.get("stepType") : "ACTION");
                workflowSteps.add(step);
            }
        }
        map.put("workflowSummary", workflowSteps);

        return map;
    }

    /**
     * Convert PetriNet to DAG representation
     */
    private Map<String, Object> convertPetriNetToDAG(Map<String, Object> petriNetMap) {
        Map<String, Object> dag = new HashMap<>();

        // Extract transitions as DAG nodes, EXCLUDING intermediate dependency connectors
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> allTransitions = (List<Map<String, Object>>) petriNetMap.get("transitions");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> arcs = (List<Map<String, Object>>) petriNetMap.get("arcs");

        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> mainTransitions = new ArrayList<>();

        if (allTransitions != null) {
            for (Map<String, Object> transition : allTransitions) {
                @SuppressWarnings("unchecked")
                Map<String, Object> metadata = (Map<String, Object>) transition.get("metadata");

                // Filter out intermediate dependency connectors
                boolean isDependencyConnector = metadata != null &&
                        Boolean.TRUE.equals(metadata.get("isDependencyConnector"));

                if (!isDependencyConnector) {
                    Map<String, Object> node = new HashMap<>();
                    node.put("id", transition.get("id"));
                    node.put("name", transition.get("name"));
                    node.put("type", "ACTION");
                    nodes.add(node);
                    mainTransitions.add(transition);
                }
            }
        }
        dag.put("nodes", nodes);

        // Build edges based on workflow dependencies (through intermediate places)
        List<Map<String, Object>> edges = new ArrayList<>();
        if (mainTransitions.size() > 1 && arcs != null) {
            // Map transition IDs for quick lookup
            Map<String, Integer> transitionOrder = new HashMap<>();
            for (int i = 0; i < mainTransitions.size(); i++) {
                transitionOrder.put((String) mainTransitions.get(i).get("id"), i);
            }

            // For sequential workflows, connect transitions in order
            for (int i = 0; i < mainTransitions.size() - 1; i++) {
                Map<String, Object> edge = new HashMap<>();
                edge.put("from", mainTransitions.get(i).get("id"));
                edge.put("to", mainTransitions.get(i + 1).get("id"));
                edges.add(edge);
            }
        }
        dag.put("edges", edges);

        return dag;
    }

    /**
     * Reconstruct PetriNet object from map (used by /dag endpoint already)
     */
    private PetriNet reconstructPetriNetFromMap(Map<String, Object> petriNetMap) {
        String name = (String) petriNetMap.getOrDefault("name", "Unnamed Petri Net");
        String description = (String) petriNetMap.getOrDefault("description", "");

        // Extract places
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> placeMaps = (List<Map<String, Object>>) petriNetMap.get("places");
        List<Place> places = new ArrayList<>();
        if (placeMaps != null) {
            for (Map<String, Object> placeMap : placeMaps) {
                String id = (String) placeMap.get("id");
                String placeName = (String) placeMap.get("name");
                Integer capacity = (Integer) placeMap.getOrDefault("capacity", Integer.MAX_VALUE);
                places.add(new Place(id, placeName, capacity));
            }
        }

        // Extract transitions
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> transitionMaps = (List<Map<String, Object>>) petriNetMap.get("transitions");
        List<Transition> transitions = new ArrayList<>();
        if (transitionMaps != null) {
            for (Map<String, Object> transitionMap : transitionMaps) {
                String id = (String) transitionMap.get("id");
                String transitionName = (String) transitionMap.get("name");
                String transitionDesc = (String) transitionMap.getOrDefault("description", "");
                String action = (String) transitionMap.get("action");
                String guard = (String) transitionMap.get("guard");
                @SuppressWarnings("unchecked")
                Map<String, Object> metadata = (Map<String, Object>) transitionMap.getOrDefault("metadata", new HashMap<>());
                Long timeoutMs = transitionMap.get("timeoutMs") != null ? ((Number) transitionMap.get("timeoutMs")).longValue() : null;
                Long delayMs = transitionMap.get("delayMs") != null ? ((Number) transitionMap.get("delayMs")).longValue() : null;
                @SuppressWarnings("unchecked")
                Map<String, Object> retryPolicy = (Map<String, Object>) transitionMap.get("retryPolicy");
                @SuppressWarnings("unchecked")
                Map<String, Object> inhibitorConditions = (Map<String, Object>) transitionMap.get("inhibitorConditions");
                transitions.add(new Transition(id, transitionName, transitionDesc, action, guard, metadata, timeoutMs, delayMs, retryPolicy, inhibitorConditions));
            }
        }

        // Extract arcs
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> arcMaps = (List<Map<String, Object>>) petriNetMap.get("arcs");
        List<Arc> arcs = new ArrayList<>();
        if (arcMaps != null) {
            for (Map<String, Object> arcMap : arcMaps) {
                String from = (String) arcMap.get("from");
                String to = (String) arcMap.get("to");
                Integer weight = (Integer) arcMap.getOrDefault("weight", 1);
                arcs.add(new Arc(from, to, weight));
            }
        }

        // Extract initial marking
        Object initialMarkingObj = petriNetMap.get("initialMarking");
        Marking initialMarking = null;
        if (initialMarkingObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> rawMarkingMap = (Map<String, Object>) initialMarkingObj;
            Map<String, Integer> markingMap = new HashMap<>();
            for (Map.Entry<String, Object> entry : rawMarkingMap.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Integer) {
                    markingMap.put(entry.getKey(), (Integer) value);
                } else if (value instanceof Number) {
                    markingMap.put(entry.getKey(), ((Number) value).intValue());
                } else {
                    markingMap.put(entry.getKey(), 0);
                }
            }
            initialMarking = new Marking(markingMap);
        } else {
            initialMarking = new Marking();
        }

        return new PetriNet(null, name, description, places, transitions, arcs,
                           initialMarking, "1.0", new HashMap<>(), null);
    }

    /**
     * Convert DAG object to map for JSON response
     * Enhanced version for formal PetriToDagProjector output with cross-highlighting metadata
     */
    private Map<String, Object> convertDagToMap(DAG dag) {
        Map<String, Object> dagMap = new HashMap<>();

        // Basic DAG metadata
        dagMap.put("id", dag.getId());
        dagMap.put("name", dag.getName());
        dagMap.put("derivedFromPetriNetId", dag.getDerivedFromPetriNetId());
        dagMap.put("metadata", dag.getMetadata());

        // Convert TaskNodes to node representation
        List<Map<String, Object>> nodeMaps = new ArrayList<>();
        for (TaskNode node : dag.getNodes()) {
            Map<String, Object> nodeMap = new HashMap<>();
            nodeMap.put("id", node.getId());
            nodeMap.put("action", node.getAction());
            nodeMap.put("type", "ACTION");

            // Include input parameters from projection
            if (node.getInputParams() != null && !node.getInputParams().isEmpty()) {
                nodeMap.put("inputParams", node.getInputParams());
            }

            // Include metadata with cross-highlighting information
            if (node.getMetadata() != null && !node.getMetadata().isEmpty()) {
                nodeMap.put("metadata", node.getMetadata());
            }

            // Dependencies for edge construction
            nodeMap.put("dependencies", node.getDependencyIds() != null ?
                       node.getDependencyIds() : new ArrayList<>());
            nodeMaps.add(nodeMap);
        }
        dagMap.put("nodes", nodeMaps);

        // Extract edges from node dependencies with cross-highlighting metadata
        List<Map<String, Object>> edgeMaps = new ArrayList<>();
        for (TaskNode node : dag.getNodes()) {
            if (node.getDependencyIds() != null) {
                for (String depId : node.getDependencyIds()) {
                    Map<String, Object> edgeMap = new HashMap<>();
                    edgeMap.put("from", depId);
                    edgeMap.put("to", node.getId());

                    // Include cross-highlighting metadata (places this edge represents)
                    Map<String, Object> nodeMetadata = node.getMetadata();
                    if (nodeMetadata != null && nodeMetadata.containsKey("incomingEdges")) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> incomingEdges =
                            (List<Map<String, Object>>) nodeMetadata.get("incomingEdges");
                        for (Map<String, Object> edgeInfo : incomingEdges) {
                            if (depId.equals(edgeInfo.get("from"))) {
                                edgeMap.put("places", edgeInfo.get("places"));
                                break;
                            }
                        }
                    }

                    edgeMaps.add(edgeMap);
                }
            }
        }
        dagMap.put("edges", edgeMaps);

        // Root node information
        if (dag.getRootNode() != null) {
            dagMap.put("rootNodeId", dag.getRootNode().getId());
        }

        return dagMap;
    }

    /**
     * Convert map to ValidationConfig
     */
    private core.petri.validation.PetriNetValidationResult.ValidationConfig convertMapToValidationConfig(
            Map<String, Object> configMap) {

        // Extract kBound with default (0 triggers constructor's default of 200)
        int kBound = 0;
        if (configMap.get("kBound") instanceof Number) {
            kBound = ((Number) configMap.get("kBound")).intValue();
        }

        // Extract maxTimeMs with default (0 triggers constructor's default of 30000ms)
        long maxTimeMs = 0L;
        if (configMap.get("maxTimeMs") != null && configMap.get("maxTimeMs") instanceof Number) {
            maxTimeMs = ((Number) configMap.get("maxTimeMs")).longValue();
        }

        // Extract enabled checks (null triggers constructor's default of all checks)
        @SuppressWarnings("unchecked")
        List<String> enabledCheckStrings = (List<String>) configMap.get("enabledChecks");
        Set<core.petri.validation.PetriNetValidationResult.CheckType> enabledChecks = null;

        if (enabledCheckStrings != null && !enabledCheckStrings.isEmpty()) {
            enabledChecks = new HashSet<>();
            for (String checkStr : enabledCheckStrings) {
                try {
                    enabledChecks.add(core.petri.validation.PetriNetValidationResult.CheckType.valueOf(checkStr));
                } catch (IllegalArgumentException e) {
                    logger.warn("Unknown check type: {}", checkStr);
                }
            }
        }

        return new core.petri.validation.PetriNetValidationResult.ValidationConfig(
                kBound, maxTimeMs, enabledChecks);
    }

    /**
     * Convert ValidationResult to map for JSON response
     */
    private Map<String, Object> convertValidationResultToMap(
            core.petri.validation.PetriNetValidationResult result) {

        Map<String, Object> map = new HashMap<>();
        map.put("petriNetId", result.getPetriNetId());
        map.put("petriStatus", result.getPetriStatus().name());
        map.put("isValid", result.isValid());
        map.put("statesExplored", result.getStatesExplored());

        // Convert check results from Map
        Map<core.petri.validation.PetriNetValidationResult.CheckType,
            core.petri.validation.PetriNetValidationResult.CheckResult> checks = result.getChecks();
        List<Map<String, Object>> checkMaps = new ArrayList<>();
        for (Map.Entry<core.petri.validation.PetriNetValidationResult.CheckType,
                       core.petri.validation.PetriNetValidationResult.CheckResult> entry : checks.entrySet()) {
            core.petri.validation.PetriNetValidationResult.CheckResult check = entry.getValue();
            Map<String, Object> checkMap = new HashMap<>();
            checkMap.put("type", check.getType().name());
            checkMap.put("status", check.getStatus().name());
            checkMap.put("message", check.getMessage());
            checkMap.put("details", check.getDetails());
            checkMap.put("executionTimeMs", check.getExecutionTimeMs());
            checkMaps.add(checkMap);
        }
        map.put("checkResults", checkMaps);

        // Convert counter-example if present (single, not list)
        if (result.getCounterExample() != null) {
            core.petri.validation.PetriNetValidationResult.CounterExample ce = result.getCounterExample();
            Map<String, Object> ceMap = new HashMap<>();
            ceMap.put("description", ce.getDescription());
            ceMap.put("failingMarking", ce.getFailingMarking());
            ceMap.put("enabledTransitions", ce.getEnabledTransitions());
            ceMap.put("pathToFailure", ce.getPathToFailure());
            map.put("counterExample", ceMap);
        }

        // Add hints if present
        if (result.getHints() != null && !result.getHints().isEmpty()) {
            map.put("hints", result.getHints());
        }

        // Add suggestions
        if (result.getSuggestions() != null) {
            map.put("suggestions", result.getSuggestions());
        }

        return map;
    }

    /**
     * Convert map to SimulationConfig
     */
    private SimulationConfig convertMapToSimulationConfig(Map<String, Object> configMap) {
        Long seed = configMap.get("seed") != null ?
                ((Number) configMap.get("seed")).longValue() : null;

        String modeStr = (String) configMap.get("mode");
        SimulationConfig.SimulationMode mode = null;
        if (modeStr != null) {
            try {
                mode = SimulationConfig.SimulationMode.valueOf(modeStr);
            } catch (IllegalArgumentException e) {
                logger.warn("Unknown simulation mode: {}, using DETERMINISTIC", modeStr);
                mode = SimulationConfig.SimulationMode.DETERMINISTIC;
            }
        }

        Integer maxSteps = (Integer) configMap.get("maxSteps");
        Integer stepDelayMs = (Integer) configMap.get("stepDelayMs");
        Boolean enableTracing = (Boolean) configMap.get("enableTracing");
        Boolean enableAnimation = (Boolean) configMap.get("enableAnimation");
        Boolean pauseOnDeadlock = (Boolean) configMap.get("pauseOnDeadlock");
        Boolean verbose = (Boolean) configMap.get("verbose");

        return new SimulationConfig(seed, mode, maxSteps, stepDelayMs,
                                   enableTracing, enableAnimation, pauseOnDeadlock, verbose);
    }

    /**
     * Convert SimulationResult to map for JSON response
     */
    private Map<String, Object> convertSimulationResultToMap(SimulationResult result) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", result.isSuccess());
        map.put("status", result.getStatus().name());
        map.put("message", result.getMessage());
        map.put("stepsExecuted", result.getStepsExecuted());
        map.put("initialMarking", result.getInitialMarking());
        map.put("finalMarking", result.getFinalMarking());

        if (result.getSimulationStartTime() != null) {
            map.put("simulationStartTime", result.getSimulationStartTime().toString());
        }
        if (result.getSimulationEndTime() != null) {
            map.put("simulationEndTime", result.getSimulationEndTime().toString());
        }

        // Convert trace events
        if (result.getTrace() != null) {
            List<Map<String, Object>> traceMaps = new ArrayList<>();
            for (core.petri.simulation.TraceEvent event : result.getTrace()) {
                Map<String, Object> eventMap = new HashMap<>();
                eventMap.put("timestamp", event.getTimestamp().toString());
                eventMap.put("type", event.getType().name());
                eventMap.put("transitionId", event.getTransitionId());
                eventMap.put("transition", event.getTransition());
                eventMap.put("markingBefore", event.getMarkingBefore());
                eventMap.put("markingAfter", event.getMarkingAfter());
                eventMap.put("description", event.getDescription());
                if (event.getSequenceNumber() != null) {
                    eventMap.put("sequenceNumber", event.getSequenceNumber());
                }
                traceMaps.add(eventMap);
            }
            map.put("trace", traceMaps);
        }

        // Add diagnostics
        if (result.getDiagnostics() != null) {
            map.put("diagnostics", result.getDiagnostics());
        }

        return map;
    }

    /**
     * Execute complete P3Net workflow with formal verification and plugin execution
     * POST /api/v1/petri/execute
     */
    @PostMapping("/execute")
    @Operation(summary = "Execute complete P3Net workflow",
               description = "End-to-end execution: parse → build → validate → project → execute with plugins")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully executed workflow"),
        @ApiResponse(responseCode = "400", description = "Invalid workflow specification"),
        @ApiResponse(responseCode = "500", description = "Execution failed")
    })
    public ResponseEntity<?> executeWorkflow(
            @Parameter(description = "P3Net execution request", required = true)
            @Valid @RequestBody PetriExecuteRequest request) {

        // TODO: Re-enable when P3NetExecutionService is completed
        logger.warn("P3Net plugin execution endpoint not yet implemented - WIP");

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", "Plugin execution not yet implemented. Use /build-dag endpoint for visualization only.");
        response.put("message", "This endpoint requires P3NetExecutionService which is currently disabled (WIP)");

        return ResponseEntity.status(501).body(response);
    }
}