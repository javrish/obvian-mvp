package api.entity;

/**
 * Enum representing user roles in the system.
 */
public enum Role {
    ADMIN("Administrator with full system access"),
    USER("Regular user with standard permissions"),
    DEVELOPER("Developer with API and plugin creation access"),
    VIEWER("Read-only access to resources"),
    OPERATOR("Can execute workflows but cannot modify"),
    MANAGER("Can manage team resources and view analytics");

    private final String description;

    Role(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean hasAdminAccess() {
        return this == ADMIN;
    }

    public boolean canExecute() {
        return this == ADMIN || this == USER || this == DEVELOPER || this == OPERATOR || this == MANAGER;
    }

    public boolean canModify() {
        return this == ADMIN || this == USER || this == DEVELOPER || this == MANAGER;
    }

    public boolean canView() {
        return true; // All roles can view
    }

    public boolean canManageTeam() {
        return this == ADMIN || this == MANAGER;
    }

    public boolean canCreatePlugins() {
        return this == ADMIN || this == DEVELOPER;
    }
}