package atunstall.server.core.api.logging;

import atunstall.server.core.api.Unique;
import atunstall.server.core.api.Version;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * An interface used for logging.
 * The root logger is always the logger obtained through dependency injection.
 */
@Unique
@Version(major = 1, minor = 0)
public interface Logger {
    /**
     * Returns the full name of this logger.
     * The root logger's full name is the empty string {@code ""}.
     * If this logger's parent is the root logger, then the full name is equal to {@code name}, otherwise it is equal to {@code parent.getName() + "/" + name}, where {@code parent} is the logger that created this logger using {@link #getChild(String)} and name is the argument to that method.
     * @return The full name of this logger.
     */
    String getFullName();

    /**
     * Returns the child logger with the specified name.
     * This child logger will be created if it does not exist.
     * @param name The name of the logger to create.
     * @return The child logger.
     */
    Logger getChild(String name);

    /**
     * Adds the given listener to this logger.
     * This logger and all its children's messages will be passed to the listener if their level isn't filtered out.
     * @param listener The listener to add to this logger.
     * @param levelFilter The level filter to assign to this listener. The message will only be passed on to the listener if this filter returns true for its level.
     */
    void addListener(Consumer<LogMessage> listener, Predicate<Level> levelFilter);

    /**
     * Removes the given listener from this logger.
     * This method fails silently if the given listener hadn't been added.
     * @param listener The listener to remove.
     */
    void removeListener(Consumer<LogMessage> listener);

    /**
     * Logs the given message at the given level.
     * @param level The level at which to log the message.
     * @param message The message to log.
     */
    void log(Level level, String message);

    /**
     * Logs the given message and throwable at the given level.
     * @param level The level at which to log the message and throwable.
     * @param message The message to log.
     * @param throwable The throwable to log.
     */
    void log(Level level, String message, Throwable throwable);
}
