package atunstall.server.core.impl.container;

import atunstall.server.core.api.Module;
import atunstall.server.core.api.Unique;
import atunstall.server.core.api.Version;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ComponentContainer {
    private static final Map<Class<?>, WeakReference<ComponentContainer>> CACHE = new HashMap<>();

    private final Class<?> component;

    ComponentContainer(Class<?> component) {
        this.component = component;
    }

    public static Optional<ComponentContainer> toComponent(String className, ClassLoader cl) {
        try {
            return toComponent(Class.forName(className, true, cl));
        } catch (ClassNotFoundException e) {
            NoClassDefFoundError error = new NoClassDefFoundError("could not find component class");
            error.addSuppressed(e);
            throw error;
        }
    }

    private static Optional<ComponentContainer> toComponent(Class<?> type) {
        ComponentContainer candidate = null;
        if (CACHE.containsKey(type) && (candidate = CACHE.get(type).get()) != null) {
            return Optional.of(candidate);
        }
        Version version = type.getAnnotation(Version.class);
        if (version != null) {
            candidate = new VersionContainer(type, version, type.getAnnotation(Unique.class) != null);
        }
        Module module = type.getAnnotation(Module.class);
        if (module != null) {
            candidate = new ModuleContainer(type, module);
        }
        if (candidate != null) {
            CACHE.put(type, new WeakReference<>(candidate));
            return Optional.of(candidate);
        }
        return Optional.empty();
    }

    public Class<?> getComponent() {
        return component;
    }
}
