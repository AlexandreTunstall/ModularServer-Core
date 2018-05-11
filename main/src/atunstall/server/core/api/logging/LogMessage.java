package atunstall.server.core.api.logging;

import java.util.Optional;

/**
 * Stores the details of a logged message.
 */
public interface LogMessage {
    /**
     * Returns the logger with which the message is being logged.
     * @return The logger of the message.
     */
    Logger getLogger();

    /**
     * Returns the level at which the message is being logged.
     * @return The level of the message.
     */
    Level getLevel();

    /**
     * Returns the message that is being logged.
     * @return The message.
     */
    String getMessage();

    /**
     * Returns the throwable that is being logged.
     * @return The throwable if it exists, {@link Optional#empty()} otherwise.
     */
    Optional<Throwable> getThrowable();
}
