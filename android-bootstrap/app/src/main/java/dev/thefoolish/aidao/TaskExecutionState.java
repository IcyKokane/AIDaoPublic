package dev.thefoolish.aidao;

/**
 * Canonical execution states for implementation work owned by AIDao.
 * User-facing task progress must reflect real execution signals rather than manual checkmarks.
 */
public enum TaskExecutionState {
    PLANNED("Planned"),
    IN_PROGRESS("In Progress"),
    VERIFYING("Verifying"),
    COMPLETE("Complete"),
    BLOCKED("Blocked");

    public final String label;

    TaskExecutionState(String label) {
        this.label = label;
    }

    public static TaskExecutionState fromStored(String value) {
        if (value == null || value.trim().isEmpty()) return PLANNED;
        try {
            return valueOf(value.trim());
        } catch (IllegalArgumentException ignored) {
            return PLANNED;
        }
    }
}
