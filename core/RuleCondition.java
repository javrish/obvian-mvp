package core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a condition in a business rule that can be evaluated against business data.
 * Supports simple expressions, complex logical operations (AND/OR), and nested conditions.
 * 
 * This class is part of the Natural Language Business Rule Engine (Phase 26.2b)
 * which implements Product Patent 24 - Natural Language Business Rule Processing.
 * 
 * Patent Alignment: Implements condition evaluation logic that enables conversational
 * rule definition with complex business logic expressions.
 * 
 * @author Obvian Labs
 * @since Phase 26.2b
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RuleCondition {
    
    /**
     * Logical operators for combining multiple expressions
     */
    public enum LogicalOperator {
        AND, OR, NOT
    }
    
    /**
     * Comparison operators for individual expressions
     */
    public enum ComparisonOperator {
        EQUALS,
        NOT_EQUALS,
        GREATER_THAN,
        GREATER_THAN_OR_EQUAL,
        LESS_THAN,
        LESS_THAN_OR_EQUAL,
        CONTAINS,
        NOT_CONTAINS,
        STARTS_WITH,
        ENDS_WITH,
        IN,
        NOT_IN,
        IS_NULL,
        IS_NOT_NULL,
        MATCHES_REGEX,
        BETWEEN
    }
    
    /**
     * Individual expression within a condition
     */
    public static class Expression {
        @JsonProperty("field")
        private final String field;
        
        @JsonProperty("operator")
        private final String operator;
        
        @JsonProperty("value")
        private final Object value;
        
        @JsonProperty("secondValue")
        private final Object secondValue; // For BETWEEN operator
        
        @JsonCreator
        public Expression(
                @JsonProperty("field") String field,
                @JsonProperty("operator") String operator,
                @JsonProperty("value") Object value,
                @JsonProperty("secondValue") Object secondValue) {
            this.field = field;
            this.operator = operator;
            this.value = value;
            this.secondValue = secondValue;
        }
        
        public Expression(String field, String operator, Object value) {
            this(field, operator, value, null);
        }
        
        // Getters
        public String getField() { return field; }
        public String getOperator() { return operator; }
        public Object getValue() { return value; }
        public Object getSecondValue() { return secondValue; }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Expression that = (Expression) o;
            return Objects.equals(field, that.field) &&
                   Objects.equals(operator, that.operator) &&
                   Objects.equals(value, that.value) &&
                   Objects.equals(secondValue, that.secondValue);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(field, operator, value, secondValue);
        }
        
        @Override
        public String toString() {
            return "Expression{" +
                    "field='" + field + '\'' +
                    ", operator='" + operator + '\'' +
                    ", value=" + value +
                    (secondValue != null ? ", secondValue=" + secondValue : "") +
                    '}';
        }
    }
    
    @JsonProperty("logicalOperator")
    private final String logicalOperator;
    
    @JsonProperty("expressions")
    private final List<Expression> expressions;
    
    @JsonProperty("nestedConditions")
    private final List<RuleCondition> nestedConditions;
    
    @JsonProperty("negated")
    private final boolean negated;
    
    @JsonCreator
    private RuleCondition(
            @JsonProperty("logicalOperator") String logicalOperator,
            @JsonProperty("expressions") List<Expression> expressions,
            @JsonProperty("nestedConditions") List<RuleCondition> nestedConditions,
            @JsonProperty("negated") boolean negated) {
        this.logicalOperator = logicalOperator != null ? logicalOperator : "AND";
        this.expressions = expressions != null ? new ArrayList<>(expressions) : new ArrayList<>();
        this.nestedConditions = nestedConditions != null ? new ArrayList<>(nestedConditions) : new ArrayList<>();
        this.negated = negated;
    }
    
    // Getters
    public String getLogicalOperator() { return logicalOperator; }
    public List<Expression> getExpressions() { return new ArrayList<>(expressions); }
    public List<RuleCondition> getNestedConditions() { return new ArrayList<>(nestedConditions); }
    public boolean isNegated() { return negated; }
    
    /**
     * Check if this condition has any expressions or nested conditions
     */
    public boolean isEmpty() {
        return expressions.isEmpty() && nestedConditions.isEmpty();
    }
    
    /**
     * Check if this is a simple condition (single expression, no nesting)
     */
    public boolean isSimple() {
        return expressions.size() == 1 && nestedConditions.isEmpty();
    }
    
    /**
     * Check if this is a complex condition (multiple expressions or nested conditions)
     */
    public boolean isComplex() {
        return expressions.size() > 1 || !nestedConditions.isEmpty();
    }
    
    /**
     * Get all field names referenced in this condition (including nested)
     */
    public List<String> getReferencedFields() {
        List<String> fields = new ArrayList<>();
        
        // Add fields from direct expressions
        expressions.forEach(expr -> fields.add(expr.getField()));
        
        // Add fields from nested conditions
        nestedConditions.forEach(nested -> fields.addAll(nested.getReferencedFields()));
        
        return fields;
    }
    
    /**
     * Builder pattern for creating RuleCondition instances
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String logicalOperator = "AND";
        private List<Expression> expressions = new ArrayList<>();
        private List<RuleCondition> nestedConditions = new ArrayList<>();
        private boolean negated = false;
        
        public Builder logicalOperator(String operator) {
            this.logicalOperator = operator;
            return this;
        }
        
        public Builder and() {
            this.logicalOperator = "AND";
            return this;
        }
        
        public Builder or() {
            this.logicalOperator = "OR";
            return this;
        }
        
        public Builder addExpression(String field, String operator, Object value) {
            this.expressions.add(new Expression(field, operator, value));
            return this;
        }
        
        public Builder addExpression(String field, String operator, Object value, Object secondValue) {
            this.expressions.add(new Expression(field, operator, value, secondValue));
            return this;
        }
        
        public Builder addExpression(Expression expression) {
            this.expressions.add(expression);
            return this;
        }
        
        public Builder expressions(List<Expression> expressions) {
            this.expressions = new ArrayList<>(expressions);
            return this;
        }
        
        public Builder addNestedCondition(RuleCondition condition) {
            this.nestedConditions.add(condition);
            return this;
        }
        
        public Builder nestedConditions(List<RuleCondition> conditions) {
            this.nestedConditions = new ArrayList<>(conditions);
            return this;
        }
        
        public Builder negated(boolean negated) {
            this.negated = negated;
            return this;
        }
        
        public Builder negate() {
            this.negated = true;
            return this;
        }
        
        // Convenience methods for common operations
        public Builder equals(String field, Object value) {
            return addExpression(field, "EQUALS", value);
        }
        
        public Builder notEquals(String field, Object value) {
            return addExpression(field, "NOT_EQUALS", value);
        }
        
        public Builder greaterThan(String field, Object value) {
            return addExpression(field, "GREATER_THAN", value);
        }
        
        public Builder greaterThanOrEqual(String field, Object value) {
            return addExpression(field, "GREATER_THAN_OR_EQUAL", value);
        }
        
        public Builder lessThan(String field, Object value) {
            return addExpression(field, "LESS_THAN", value);
        }
        
        public Builder lessThanOrEqual(String field, Object value) {
            return addExpression(field, "LESS_THAN_OR_EQUAL", value);
        }
        
        public Builder contains(String field, Object value) {
            return addExpression(field, "CONTAINS", value);
        }
        
        public Builder notContains(String field, Object value) {
            return addExpression(field, "NOT_CONTAINS", value);
        }
        
        public Builder startsWith(String field, Object value) {
            return addExpression(field, "STARTS_WITH", value);
        }
        
        public Builder endsWith(String field, Object value) {
            return addExpression(field, "ENDS_WITH", value);
        }
        
        public Builder in(String field, Object value) {
            return addExpression(field, "IN", value);
        }
        
        public Builder notIn(String field, Object value) {
            return addExpression(field, "NOT_IN", value);
        }
        
        public Builder isNull(String field) {
            return addExpression(field, "IS_NULL", null);
        }
        
        public Builder isNotNull(String field) {
            return addExpression(field, "IS_NOT_NULL", null);
        }
        
        public Builder matchesRegex(String field, String pattern) {
            return addExpression(field, "MATCHES_REGEX", pattern);
        }
        
        public Builder between(String field, Object lowerValue, Object upperValue) {
            return addExpression(field, "BETWEEN", lowerValue, upperValue);
        }
        
        public RuleCondition build() {
            validate();
            return new RuleCondition(logicalOperator, expressions, nestedConditions, negated);
        }
        
        private void validate() {
            if (expressions.isEmpty() && nestedConditions.isEmpty()) {
                throw new IllegalArgumentException("Condition must have at least one expression or nested condition");
            }
            
            // Validate logical operator
            if (logicalOperator != null && 
                !"AND".equals(logicalOperator) && 
                !"OR".equals(logicalOperator) && 
                !"NOT".equals(logicalOperator)) {
                throw new IllegalArgumentException("Invalid logical operator: " + logicalOperator);
            }
            
            // Validate expressions
            for (Expression expr : expressions) {
                if (expr.getField() == null || expr.getField().trim().isEmpty()) {
                    throw new IllegalArgumentException("Expression field cannot be null or empty");
                }
                if (expr.getOperator() == null || expr.getOperator().trim().isEmpty()) {
                    throw new IllegalArgumentException("Expression operator cannot be null or empty");
                }
                
                // Validate BETWEEN operator has two values
                if ("BETWEEN".equals(expr.getOperator()) && expr.getSecondValue() == null) {
                    throw new IllegalArgumentException("BETWEEN operator requires both value and secondValue");
                }
            }
            
            // NOT operator should only have one expression or nested condition
            if ("NOT".equals(logicalOperator) && 
                (expressions.size() + nestedConditions.size()) > 1) {
                throw new IllegalArgumentException("NOT operator can only be used with a single expression or nested condition");
            }
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuleCondition that = (RuleCondition) o;
        return negated == that.negated &&
               Objects.equals(logicalOperator, that.logicalOperator) &&
               Objects.equals(expressions, that.expressions) &&
               Objects.equals(nestedConditions, that.nestedConditions);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(logicalOperator, expressions, nestedConditions, negated);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (negated) sb.append("NOT (");
        
        boolean first = true;
        for (Expression expr : expressions) {
            if (!first) sb.append(" ").append(logicalOperator).append(" ");
            sb.append(expr.toString());
            first = false;
        }
        
        for (RuleCondition nested : nestedConditions) {
            if (!first) sb.append(" ").append(logicalOperator).append(" ");
            sb.append("(").append(nested.toString()).append(")");
            first = false;
        }
        
        if (negated) sb.append(")");
        return sb.toString();
    }
}