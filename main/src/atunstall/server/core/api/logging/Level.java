package atunstall.server.core.api.logging;

/**
 * Represents at what level a message should be logged based on the importance of the message.
 */
public enum Level {
    /**
     * Log level used for debugging messages.
     */
    DEBUG,

    /**
     * Log level used for informative messages.
     */
    INFO,

    /**
     * Log level used for warning messages.
     */
    WARNING,

    /**
     * Log level used for error messages.
     */
    ERROR;

    /**
     * Checks if the given level is equal to or more severe than this level.
     * @param level The level whose severity to check.
     * @return False if the given level is less severe than this level, true otherwise.
     */
    public boolean isMoreSevere(Level level) {
        return ordinal() <= level.ordinal();
    }
}
