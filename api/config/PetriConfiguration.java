package api.config;

import core.petri.grammar.AutomationGrammar;
import core.petri.grammar.IntentToPetriMapper;
import core.petri.grammar.RuleEngine;
import core.petri.validation.PetriNetValidator;
import core.petri.validation.SimplePetriNetValidator;
import core.petri.simulation.PetriTokenSimulator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for Petri Net components.
 *
 * Configures the P3Net (Petri Net Phase 1) components for the Obvian DAG execution engine:
 * - AutomationGrammar: Transforms PetriIntentSpec to PetriNet structures
 * - IntentToPetriMapper: Maps intent steps to Petri net elements
 * - RuleEngine: Applies transformation rules for complex patterns
 * - PetriNetValidator: Validates Petri net structural integrity
 * - PetriTokenSimulator: Simulates token flow through Petri nets
 */
@Configuration
public class PetriConfiguration {

    /**
     * Intent to Petri mapper for step-by-step transformation
     */
    @Bean
    public IntentToPetriMapper intentToPetriMapper() {
        return new IntentToPetriMapper();
    }

    /**
     * Rule engine for applying complex transformation patterns
     */
    @Bean
    public RuleEngine ruleEngine() {
        return new RuleEngine();
    }

    /**
     * AutomationGrammar with proper dependency injection
     */
    @Bean
    public AutomationGrammar automationGrammar(IntentToPetriMapper mapper, RuleEngine ruleEngine) {
        return new AutomationGrammar(mapper, ruleEngine);
    }

    /**
     * Petri Net validator using PetriNetValidator implementation
     */
    @Bean
    public PetriNetValidator petriNetValidator() {
        return new PetriNetValidator();
    }

    /**
     * Petri Token simulator for workflow execution
     */
    @Bean
    public PetriTokenSimulator petriTokenSimulator() {
        return new PetriTokenSimulator();
    }
}