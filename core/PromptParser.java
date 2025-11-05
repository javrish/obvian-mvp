package core;

import memory.MemoryStore;
import memory.ExecutionMemoryEntry;
import memory.FileMemoryEntry;
import plugins.PluginRegistry;
import plugins.Plugin;
// Plugin implementations temporarily disabled for basic compilation
// import plugins.email.EmailPlugin;
// import plugins.file.FilePlugin;
// import plugins.slack.SlackPlugin;
// import plugins.reminder.ReminderPlugin;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses natural language prompts to extract intent, entities, and parameters
 * for DAG generation and execution. Supports both single-step and multi-step
 * compound prompts with sequential actions.
 */
public class PromptParser {
    
    /**
     * Create a PromptParser without memory support (backward compatibility)
     */
    public PromptParser() {
        this.memoryStore = null;
    }
    
    /**
     * Create a PromptParser with memory support
     * @param memoryStore The memory store for contextual references
     */
    public PromptParser(MemoryStore memoryStore) {
        this.memoryStore = memoryStore;
    }
    
    // Pattern definitions for common prompt structures
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "(?i)(?:send\\s+(?:an?\\s+)?(?:email\\s+)?to|email\\s+(?:it\\s+to|the\\s+file\\s+to|to))\\s+([\\w._%+-]+@[\\w.-]+\\.[A-Za-z]{2,})(?:\\s+saying[:\\s]+(.+))?",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern FILE_CREATE_PATTERN = Pattern.compile(
        "(?i)create\\s+(?:a\\s+)?(?:file\\s+)?(?:called\\s+|named\\s+)?([\\w.-]+)(?:\\s+with\\s+content[:\\s]+(.+))?",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern REMINDER_PATTERN = Pattern.compile(
        "(?i)(?:set\\s+(?:a\\s+)?reminder|remind\\s+me)\\s+(?:to\\s+)?(.+?)(?:\\s+(?:at|on|in)\\s+(.+))?",
        Pattern.CASE_INSENSITIVE
    );
    
    // Conjunction patterns for detecting compound prompts
    private static final Pattern CONJUNCTION_PATTERN = Pattern.compile(
        "\\b(?:then|and then|after that|next|afterwards|followed by|,\\s*then|;\\s*then)\\b",
        Pattern.CASE_INSENSITIVE
    );

    // Pattern for enumerated lists: "N steps: item1, item2, item3"
    private static final Pattern ENUMERATED_STEPS_PATTERN = Pattern.compile(
        "(?i)(?:create|build|make|setup|set\\s+up)?\\s*(?:a\\s+)?(?:workflow|task|process|job)?\\s*with\\s+(\\d+)\\s+steps?[:\\s]+(.+)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // Memory reference patterns
    private static final Pattern LAST_FILE_PATTERN = Pattern.compile(
        "(?i)(?:the\\s+)?(?:last|latest|most\\s+recent)\\s+file(?:\\s+(?:I\\s+)?created)?",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern LAST_EXECUTION_PATTERN = Pattern.compile(
        "(?i)(?:the\\s+)?(?:last|latest|previous|most\\s+recent)\\s+(?:task|execution|result|action)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern SPECIFIC_FILE_PATTERN = Pattern.compile(
        "(?i)(?:the\\s+)?file\\s+(?:called\\s+|named\\s+)?([\\w.-]+)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern EMAIL_WITH_FILE_PATTERN = Pattern.compile(
        "(?i)email\\s+(?:the\\s+)?(?:(?:last|latest|most\\s+recent)\\s+)?file(?:\\s+(?:I\\s+)?created)?\\s+to\\s+([\\w._%+-]+@[\\w.-]+\\.[A-Za-z]{2,})",
        Pattern.CASE_INSENSITIVE
    );
    
    // Add a new pattern for memory-aware email prompts: e.g., 'Email the last file I created to john@example.com' or 'Send the previous reminder to John'
    private static final Pattern EMAIL_MEMORY_REF_PATTERN = Pattern.compile(
        "(?i)(email|send)\\s+(?:the\\s+)?((?:last|latest|most\\s+recent)\\s+file(?:\\s+(?:i\\s+)?created)?|previous\\s+reminder|recent\\s+message|latest\\s+message|last\\s+message)\\s+to\\s+([\\w._%+-]+@[\\w.-]+\\.[A-Za-z]{2,}|[A-Za-z ]+)",
        Pattern.CASE_INSENSITIVE
    );
    
    private final MemoryStore memoryStore;
    
    /**
     * Represents a parsed intent with action and parameters
     */
    public static class ParsedIntent {
        private final String action;
        private final Map<String, Object> parameters;
        private final String originalPrompt;
        private final int sequenceOrder;
        
        public ParsedIntent(String action, Map<String, Object> parameters, String originalPrompt) {
            this(action, parameters, originalPrompt, 0);
        }
        
        public ParsedIntent(String action, Map<String, Object> parameters, String originalPrompt, int sequenceOrder) {
            this.action = action;
            this.parameters = parameters != null ? parameters : new HashMap<>();
            this.originalPrompt = originalPrompt;
            this.sequenceOrder = sequenceOrder;
        }
        
        public String getAction() { return action; }
        public Map<String, Object> getParameters() { return parameters; }
        public String getOriginalPrompt() { return originalPrompt; }
        public int getSequenceOrder() { return sequenceOrder; }
        
        @Override
        public String toString() {
            return String.format("ParsedIntent{action='%s', parameters=%s, sequenceOrder=%d}", 
                action, parameters, sequenceOrder);
        }
    }
    
    /**
     * Represents the result of parsing a compound prompt with multiple intents
     */
    public static class CompoundParseResult {
        private final List<ParsedIntent> intents;
        private final boolean isCompound;
        private final String originalPrompt;
        
        public CompoundParseResult(List<ParsedIntent> intents, boolean isCompound, String originalPrompt) {
            this.intents = intents != null ? intents : new ArrayList<>();
            this.isCompound = isCompound;
            this.originalPrompt = originalPrompt;
        }
        
        public List<ParsedIntent> getIntents() { return intents; }
        public boolean isCompound() { return isCompound; }
        public String getOriginalPrompt() { return originalPrompt; }
        public int getIntentCount() { return intents.size(); }
        
        /**
         * Get the first intent (for backward compatibility)
         */
        public ParsedIntent getPrimaryIntent() {
            return intents.isEmpty() ? null : intents.get(0);
        }
        
        @Override
        public String toString() {
            return String.format("CompoundParseResult{isCompound=%s, intentCount=%d, intents=%s}", 
                isCompound, intents.size(), intents);
        }
    }
    
    /**
     * Parse a natural language prompt into structured intent (backward compatibility)
     * @param prompt The natural language prompt to parse
     * @return ParsedIntent containing action and parameters
     * @throws IllegalArgumentException if prompt is null or empty
     */
    public ParsedIntent parsePrompt(String prompt) {
        CompoundParseResult result = parseCompoundPrompt(prompt);
        return result.getPrimaryIntent();
    }
    
    /**
     * Parse a natural language prompt that may contain multiple sequential actions
     * @param prompt The natural language prompt to parse
     * @return CompoundParseResult containing all parsed intents
     * @throws IllegalArgumentException if prompt is null or empty
     */
    public CompoundParseResult parseCompoundPrompt(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be null or empty");
        }
        
        String cleanPrompt = prompt.trim();
        
        // Check if this is a compound prompt
        if (isCompoundPrompt(cleanPrompt)) {
            return parseMultipleIntents(cleanPrompt);
        } else {
            // Single intent - use existing logic
            ParsedIntent singleIntent = parseSingleIntent(cleanPrompt);
            return new CompoundParseResult(Arrays.asList(singleIntent), false, cleanPrompt);
        }
    }
    
    /**
     * Check if a prompt contains multiple sequential actions
     */
    public boolean isCompoundPrompt(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return false;
        }
        // Check for conjunction-based compound prompts
        if (CONJUNCTION_PATTERN.matcher(prompt).find()) {
            return true;
        }
        // Check for enumerated step lists
        if (ENUMERATED_STEPS_PATTERN.matcher(prompt).find()) {
            return true;
        }
        return false;
    }
    
    /**
     * Parse multiple intents from a compound prompt
     */
    private CompoundParseResult parseMultipleIntents(String prompt) {
        List<ParsedIntent> intents = new ArrayList<>();

        // Check if this is an enumerated step list first
        Matcher enumeratedMatcher = ENUMERATED_STEPS_PATTERN.matcher(prompt);
        if (enumeratedMatcher.find()) {
            String stepsText = enumeratedMatcher.group(2);
            if (stepsText != null && !stepsText.trim().isEmpty()) {
                // Split by commas, semicolons, or "and"
                String[] stepItems = stepsText.split("[,;]|\\s+and\\s+");

                for (int i = 0; i < stepItems.length; i++) {
                    String stepItem = stepItems[i].trim();
                    if (!stepItem.isEmpty()) {
                        ParsedIntent intent = parseSingleIntent(stepItem, i);
                        intents.add(intent);
                    }
                }

                if (!intents.isEmpty()) {
                    return new CompoundParseResult(intents, true, prompt);
                }
            }
        }

        // Fall back to conjunction-based parsing
        String[] segments = CONJUNCTION_PATTERN.split(prompt);

        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i].trim();

            // Clean up trailing punctuation that might be left from splitting
            segment = segment.replaceAll("[,;]\\s*$", "").trim();

            if (!segment.isEmpty()) {
                ParsedIntent intent = parseSingleIntent(segment, i);
                intents.add(intent);
            }
        }

        // If we didn't find any valid intents, treat as single generic intent
        if (intents.isEmpty()) {
            intents.add(new ParsedIntent("generic", Map.of("message", prompt), prompt, 0));
        }

        return new CompoundParseResult(intents, true, prompt);
    }
    
    /**
     * Parse a single intent from a prompt segment
     */
    private ParsedIntent parseSingleIntent(String prompt) {
        return parseSingleIntent(prompt, 0);
    }
    
    /**
     * Try to detect memory reference in the prompt and return a canonical memoryRef string if found.
     * Returns null if no memory reference is detected.
     */
    private String detectMemoryReference(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) return null;
        String lower = prompt.toLowerCase();
        if (LAST_FILE_PATTERN.matcher(prompt).find()) {
            return "last_file";
        }
        if (LAST_EXECUTION_PATTERN.matcher(prompt).find()) {
            return "last_execution";
        }
        if (lower.contains("previous reminder")) {
            return "previous_reminder";
        }
        if (lower.contains("recent message") || lower.contains("latest message") || lower.contains("last message")) {
            return "recent_message";
        }
        // Add more patterns as needed
        return null;
    }
    
    /**
     * Parse a single intent from a prompt segment with sequence order
     */
    private ParsedIntent parseSingleIntent(String prompt, int sequenceOrder) {
        String cleanPrompt = prompt.trim();
        // Try email pattern
        ParsedIntent emailIntent = tryParseEmail(cleanPrompt, sequenceOrder);
        if (emailIntent != null) {
            // Check for memory reference in email context
            String memRef = detectMemoryReference(cleanPrompt);
            if (memRef != null) {
                emailIntent.getParameters().put("memoryRef", memRef);
            }
            return emailIntent;
        }
        // Try file creation pattern
        ParsedIntent fileIntent = tryParseFileCreation(cleanPrompt, sequenceOrder);
        if (fileIntent != null) {
            String memRef = detectMemoryReference(cleanPrompt);
            if (memRef != null) {
                fileIntent.getParameters().put("memoryRef", memRef);
            }
            return fileIntent;
        }
        // Try reminder pattern
        ParsedIntent reminderIntent = tryParseReminder(cleanPrompt, sequenceOrder);
        if (reminderIntent != null) {
            String memRef = detectMemoryReference(cleanPrompt);
            if (memRef != null) {
                reminderIntent.getParameters().put("memoryRef", memRef);
            }
            return reminderIntent;
        }
        // Fallback: generic action
        Map<String, Object> params = new HashMap<>();
        params.put("message", cleanPrompt);
        String memRef = detectMemoryReference(cleanPrompt);
        if (memRef != null) {
            params.put("memoryRef", memRef);
        }
        return new ParsedIntent("generic", params, cleanPrompt, sequenceOrder);
    }
    
    /**
     * Try to parse email sending intent
     */
    private ParsedIntent tryParseEmail(String prompt) {
        return tryParseEmail(prompt, 0);
    }
    
    /**
     * Try to parse email sending intent with sequence order
     */
    private ParsedIntent tryParseEmail(String prompt, int sequenceOrder) {
        // First, try the standard email pattern
        Matcher matcher = EMAIL_PATTERN.matcher(prompt);
        if (matcher.find()) {
            String recipient = matcher.group(1);
            String message = matcher.group(2);
            Map<String, Object> params = new HashMap<>();
            params.put("recipient", recipient);
            params.put("subject", "Message from Obvian");
            if (message != null && !message.trim().isEmpty()) {
                String cleanMessage = message.trim();
                if ((cleanMessage.startsWith("'") && cleanMessage.endsWith("'")) ||
                    (cleanMessage.startsWith("\"") && cleanMessage.endsWith("\""))) {
                    cleanMessage = cleanMessage.substring(1, cleanMessage.length() - 1);
                }
                params.put("body", cleanMessage);
            } else {
                params.put("body", "Hello from Obvian!");
            }
            return new ParsedIntent("send_email", params, prompt, sequenceOrder);
        }
        // Next, try the memory-aware email pattern
        Matcher memEmailMatcher = EMAIL_MEMORY_REF_PATTERN.matcher(prompt);
        if (memEmailMatcher.find()) {
            String memoryPhrase = memEmailMatcher.group(2);
            String recipient = memEmailMatcher.group(3).trim();
            // If recipient is not an email, treat as a name (e.g., 'John')
            if (!recipient.contains("@")) {
                // Optionally, you could add logic to resolve name to email if MemoryStore is available
                // For now, just use the name as recipient
                recipient = recipient;
            }
            Map<String, Object> params = new HashMap<>();
            params.put("recipient", recipient);
            params.put("subject", "Message from Obvian");
            params.put("body", "Hello from Obvian!");
            // Canonicalize memoryRef
            String memRef = detectMemoryReference(memoryPhrase);
            if (memRef != null) {
                params.put("memoryRef", memRef);
            }
            return new ParsedIntent("send_email", params, prompt, sequenceOrder);
        }
        return null;
    }
    
    /**
     * Try to parse file creation intent
     */
    private ParsedIntent tryParseFileCreation(String prompt) {
        return tryParseFileCreation(prompt, 0);
    }
    
    /**
     * Try to parse file creation intent with sequence order
     */
    private ParsedIntent tryParseFileCreation(String prompt, int sequenceOrder) {
        Matcher matcher = FILE_CREATE_PATTERN.matcher(prompt);
        if (matcher.find()) {
            String filename = matcher.group(1);
            String content = matcher.group(2);
            
            Map<String, Object> params = new HashMap<>();
            params.put("filename", filename);
            
            if (content != null && !content.trim().isEmpty()) {
                // Remove surrounding quotes if present
                String cleanContent = content.trim();
                if ((cleanContent.startsWith("'") && cleanContent.endsWith("'")) ||
                    (cleanContent.startsWith("\"") && cleanContent.endsWith("\""))) {
                    cleanContent = cleanContent.substring(1, cleanContent.length() - 1);
                }
                params.put("content", cleanContent);
            } else {
                params.put("content", "");
            }
            
            return new ParsedIntent("create_file", params, prompt, sequenceOrder);
        }
        return null;
    }
    
    /**
     * Try to parse reminder intent
     */
    private ParsedIntent tryParseReminder(String prompt) {
        return tryParseReminder(prompt, 0);
    }
    
    /**
     * Try to parse reminder intent with sequence order
     */
    private ParsedIntent tryParseReminder(String prompt, int sequenceOrder) {
        // Try pattern with time first
        Pattern reminderWithTime = Pattern.compile(
            "(?i)(?:set\\s+(?:a\\s+)?reminder|remind\\s+me)\\s+(?:to\\s+)?(.+?)\\s+(?:at|on|in)\\s+(.+)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcherWithTime = reminderWithTime.matcher(prompt);
        if (matcherWithTime.find()) {
            String task = matcherWithTime.group(1);
            String time = matcherWithTime.group(2);
            
            Map<String, Object> params = new HashMap<>();
            params.put("task", task != null ? task.trim() : "");
            params.put("time", time != null ? time.trim() : "now");
            
            return new ParsedIntent("set_reminder", params, prompt, sequenceOrder);
        }
        
        // Try pattern without time
        Pattern reminderWithoutTime = Pattern.compile(
            "(?i)(?:set\\s+(?:a\\s+)?reminder|remind\\s+me)\\s+(?:to\\s+)?(.+)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcherWithoutTime = reminderWithoutTime.matcher(prompt);
        if (matcherWithoutTime.find()) {
            String task = matcherWithoutTime.group(1);
            
            Map<String, Object> params = new HashMap<>();
            params.put("task", task != null ? task.trim() : "");
            params.put("time", "now");
            
            return new ParsedIntent("set_reminder", params, prompt, sequenceOrder);
        }
        
        return null;
    }
    
    /**
     * Extract entities from a prompt (emails, filenames, etc.)
     * @param prompt The prompt to analyze
     * @return Map of entity types to lists of found entities
     */
    public Map<String, List<String>> extractEntities(String prompt) {
        Map<String, List<String>> entities = new HashMap<>();
        
        if (prompt == null || prompt.trim().isEmpty()) {
            return entities;
        }
        
        // Extract email addresses
        Pattern emailPattern = Pattern.compile("[\\w._%+-]+@[\\w.-]+\\.[A-Za-z]{2,}");
        Matcher emailMatcher = emailPattern.matcher(prompt);
        List<String> emails = new ArrayList<>();
        while (emailMatcher.find()) {
            emails.add(emailMatcher.group());
        }
        if (!emails.isEmpty()) {
            entities.put("emails", emails);
        }
        
        // Extract potential filenames (words with extensions)
        Pattern filenamePattern = Pattern.compile("\\b[\\w.-]+\\.[a-zA-Z]{2,4}\\b");
        Matcher filenameMatcher = filenamePattern.matcher(prompt);
        List<String> filenames = new ArrayList<>();
        while (filenameMatcher.find()) {
            filenames.add(filenameMatcher.group());
        }
        if (!filenames.isEmpty()) {
            entities.put("filenames", filenames);
        }
        
        return entities;
    }
    
    /**
     * Check if a prompt matches a known pattern
     * @param prompt The prompt to check
     * @return true if the prompt matches a known pattern, false otherwise
     */
    public boolean isKnownPattern(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return false;
        }
        
        return EMAIL_PATTERN.matcher(prompt).find() ||
               FILE_CREATE_PATTERN.matcher(prompt).find() ||
               REMINDER_PATTERN.matcher(prompt).find();
    }
    

    
    /**
     * Get the conjunction words found in a prompt
     * @param prompt The prompt to analyze
     * @return List of conjunction words found
     */
    public List<String> getConjunctionWords(String prompt) {
        List<String> conjunctions = new ArrayList<>();
        if (prompt == null || prompt.trim().isEmpty()) {
            return conjunctions;
        }
        
        Matcher matcher = CONJUNCTION_PATTERN.matcher(prompt);
        while (matcher.find()) {
            conjunctions.add(matcher.group().trim());
        }
        
        return conjunctions;
    }
    
    /**
     * Get supported action types
     * @return List of supported action types
     */
    public List<String> getSupportedActions() {
        return Arrays.asList("send_email", "create_file", "set_reminder", "generic");
    }
    
    // Plugin-aware parsing capabilities (Phase 26.2c enhancement)
    private PluginRegistry pluginRegistry;
    
    /**
     * Set the plugin registry for dynamic capability discovery
     * @param registry The plugin registry to use
     */
    public void setPluginRegistry(PluginRegistry registry) {
        this.pluginRegistry = registry;
    }
    
    /**
     * Parse prompt with plugin awareness - discovers available plugins and maps intent
     * @param prompt The natural language prompt
     * @return ParsedIntent with plugin-aware action mapping
     */
    public ParsedIntent parseWithPluginAwareness(String prompt) {
        if (pluginRegistry == null) {
            // Fallback to standard parsing if no registry
            return parsePrompt(prompt);
        }
        
        // First try standard patterns
        ParsedIntent standardIntent = parsePrompt(prompt);
        if (!"generic".equals(standardIntent.getAction())) {
            return standardIntent;
        }
        
        // For generic intents, discover plugin capabilities
        Map<String, Double> pluginScores = new HashMap<>();
        
        // Score each plugin based on capability match
        for (Plugin plugin : pluginRegistry.getAllPlugins()) {
            if (plugin != null) {
                double score = calculatePluginRelevance(prompt, plugin);
                if (score > 0.5) { // Threshold for relevance
                    pluginScores.put(plugin.getName(), score);
                }
            }
        }
        
        // Find best matching plugin
        if (!pluginScores.isEmpty()) {
            String bestPlugin = pluginScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
                
            if (bestPlugin != null) {
                Map<String, Object> params = new HashMap<>(standardIntent.getParameters());
                params.put("plugin", bestPlugin);
                params.put("confidence", pluginScores.get(bestPlugin));
                return new ParsedIntent("plugin_action", params, prompt);
            }
        }
        
        return standardIntent;
    }
    
    /**
     * Calculate how relevant a plugin is to the given prompt
     * @param prompt The user prompt
     * @param plugin The plugin to evaluate
     * @return Relevance score between 0.0 and 1.0
     */
    private double calculatePluginRelevance(String prompt, Plugin plugin) {
        // This is a simplified version - in production would use NLP/ML
        String promptLower = prompt.toLowerCase();
        double score = 0.0;
        
        // Check plugin name relevance
        String pluginName = plugin.getName().toLowerCase();
        if (promptLower.contains(pluginName.replace("plugin", ""))) {
            score += 0.5;
        }

        if (pluginName.contains("email") &&
            (promptLower.contains("email") || promptLower.contains("send") || promptLower.contains("notify"))) {
            score += 0.8;
        } else if (pluginName.contains("file") &&
            (promptLower.contains("file") || promptLower.contains("create") || promptLower.contains("write"))) {
            score += 0.8;
        } else if (pluginName.contains("slack") &&
            (promptLower.contains("slack") || promptLower.contains("message") || promptLower.contains("channel"))) {
            score += 0.8;
        }
        // Add more plugin-specific patterns as needed
        
        return Math.min(score, 1.0);
    }
}