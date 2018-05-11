package atunstall.server.core.impl.logging;

import atunstall.server.core.api.logging.Level;
import atunstall.server.core.api.logging.LogMessage;

import java.util.Optional;

public class LogMessageImpl implements LogMessage {
    private final LoggerImpl logger;
    private final Level level;
    private final String message;
    private final Throwable throwable;

    LogMessageImpl(LoggerImpl logger, Level level, String message) {
        this(logger, level, message, null);
    }

    LogMessageImpl(LoggerImpl logger, Level level, String message, Throwable throwable) {
        this.logger = logger;
        this.level = level;
        this.message = message;
        this.throwable = throwable;
    }

    @Override
    public LoggerImpl getLogger() {
        return logger;
    }

    @Override
    public Level getLevel() {
        return level;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public Optional<Throwable> getThrowable() {
        return Optional.ofNullable(throwable);
    }
}
