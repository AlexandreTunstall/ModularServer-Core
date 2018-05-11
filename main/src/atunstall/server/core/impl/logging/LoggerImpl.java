package atunstall.server.core.impl.logging;

import atunstall.server.core.api.logging.Level;
import atunstall.server.core.api.logging.LogMessage;
import atunstall.server.core.api.logging.Logger;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class LoggerImpl implements Logger {
    RootLogger root;
    private final LoggerImpl parent;
    private final String fullName;
    private final Map<String, WeakReference<Logger>> children;
    private final Map<Consumer<LogMessage>, Predicate<Level>> listeners;

    LoggerImpl(LoggerImpl parent, String fullName) {
        this.root = parent == null ? null : parent.root;
        this.parent = parent;
        this.fullName = fullName;
        children = new HashMap<>();
        listeners = new HashMap<>();
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public Logger getChild(String name) {
        Logger logger = null;
        if (children.containsKey(name)) {
            logger = children.get(name).get();
        }
        if (logger == null) {
            logger = new LoggerImpl(this, parent == null ? name : String.format("%s/%s", fullName, name));
            children.put(name, new WeakReference<>(logger));
        }
        return logger;
    }

    @Override
    public void addListener(Consumer<LogMessage> listener, Predicate<Level> levelFilter) {
        listeners.put(listener, levelFilter);
    }

    @Override
    public void removeListener(Consumer<LogMessage> listener) {
        listeners.remove(listener);
    }

    @Override
    public void log(Level level, String message) {
        root.log(new LogMessageImpl(this, level, message));
    }

    @Override
    public void log(Level level, String message, Throwable throwable) {
        root.log(new LogMessageImpl(this, level, message, throwable));
    }

    LoggerImpl getParent() {
        return parent;
    }

    void handle(LogMessageImpl message) {
        listeners.entrySet().stream().filter(e -> e.getValue().test(message.getLevel())).forEach(e -> e.getKey().accept(message));
    }
}
