package atunstall.server.core.impl.dependency;

import atunstall.server.core.api.Version;
import atunstall.server.core.api.logging.Level;
import atunstall.server.core.impl.container.ModuleContainer;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public class ModuleNode extends Node {
    private final ModuleContainer moduleContainer;
    private Set<VersionNode> implemented;
    private Map<VersionNode, Boolean> dependencies;
    private Map<VersionNode, Version> dependencyVersions;
    private Object instance;

    ModuleNode(DependencyTree tree, ModuleContainer moduleContainer) {
        super(tree);
        this.moduleContainer = moduleContainer;
    }

    public void setInstance(Object instance) {
        this.instance = instance;
    }

    ModuleContainer getModuleContainer() {
        return moduleContainer;
    }

    Set<VersionNode> getImplemented() {
        if (implemented == null) {
            implemented = new HashSet<>();
            Deque<Class<?>> processing = new ArrayDeque<>();
            processing.add(moduleContainer.getComponent());
            while (processing.size() > 0) {
                Class<?> current = processing.pop();
                tree.getVersionNode(current).ifPresent(versionNode -> implemented.add(versionNode));
                Class<?> superclass = current.getSuperclass();
                if (superclass != null) {
                    processing.add(superclass);
                }
                processing.addAll(Arrays.asList(current.getInterfaces()));
            }
        }
        return implemented;
    }

    Map<VersionNode, Version> getDependencies() {
        if (dependencies == null) {
            dependencies = new LinkedHashMap<>();
            dependencyVersions = new LinkedHashMap<>();
            Annotation[][] annotations = moduleContainer.getComponent().getConstructors()[0].getParameterAnnotations();
            Type[] parameters = moduleContainer.getComponent().getConstructors()[0].getGenericParameterTypes();
            for (int index = 0; index < parameters.length; index++) {
                Class<?> raw;
                boolean supplier = false;
                if (parameters[index] instanceof ParameterizedType) {
                    raw = (Class<?>) ((ParameterizedType) parameters[index]).getRawType();
                    if (supplier = raw == Supplier.class) {
                        parameters[index] = ((ParameterizedType) parameters[index]).getActualTypeArguments()[0];
                        if (parameters[index] instanceof ParameterizedType) {
                            raw = (Class<?>) ((ParameterizedType) parameters[index]).getRawType();
                        } else {
                            raw = (Class<?>) parameters[index];
                        }
                    }
                } else {
                    raw = (Class<?>) parameters[index];
                }
                Version version = null;
                for (Annotation annotation : annotations[index]) {
                    if (annotation instanceof Version) {
                        version = (Version) annotation;
                        break;
                    }
                }
                VersionNode node = tree.getVersionNode(raw).orElseThrow(IllegalStateException::new);
                dependencies.put(node, supplier);
                dependencyVersions.put(node, version);
            }
        }
        return dependencyVersions;
    }

    Object createInstance() {
        Deque<ModuleNode> nodes = new ArrayDeque<>();
        Deque<Object[]> arguments = new ArrayDeque<>();
        Deque<Integer> indexes = new ArrayDeque<>();
        Deque<Iterator<Map.Entry<VersionNode, Boolean>>> iterators = new ArrayDeque<>();
        Deque<Boolean> suppliers = new ArrayDeque<>();
        nodes.add(this);
        arguments.add(new Object[getDependencies().size()]);
        indexes.add(0);
        iterators.add(dependencies.entrySet().iterator());
        while (nodes.size() > 0) {
            Object[] args = arguments.peekLast();
            int index = indexes.peekLast();
            Iterator<Map.Entry<VersionNode, Boolean>> iterator = iterators.peekLast();
            if (index < args.length) {
                Map.Entry<VersionNode, Boolean> dependency = iterator.next();
                Optional<Object> opt = dependency.getKey().getInstance();
                if (opt.isPresent()) {
                    args[index] = dependency.getValue() ? (Supplier<?>) dependency.getKey().getPreferredImplementation()::createInstance : opt.get();
                    index = indexes.removeLast();
                    indexes.add(index + 1);
                } else {
                    ModuleNode implementation = dependency.getKey().getPreferredImplementation();
                    nodes.add(implementation);
                    arguments.add(new Object[implementation.getDependencies().size()]);
                    indexes.add(0);
                    iterators.add(implementation.dependencies.entrySet().iterator());
                    suppliers.add(dependency.getValue());
                }
            } else {
                arguments.removeLast();
                indexes.removeLast();
                iterators.removeLast();
                ModuleNode last = nodes.removeLast();
                Object instance = last.createInstance(args);
                if (nodes.size() > 0) {
                    arguments.peekLast()[index = indexes.removeLast()] = suppliers.removeLast() ? (Supplier<?>) last::createInstance : instance;
                    indexes.add(index + 1);
                }
            }
        }
        return getLastInstance().orElseThrow(IllegalStateException::new);
    }

    Optional<Object> getLastInstance() {
        return Optional.ofNullable(instance);
    }

    private Object createInstance(Object[] args) {
        tree.logger.log(Level.DEBUG, "Creating an instance of " + moduleContainer.getComponent().getName());
        try {
            return instance = moduleContainer.getComponent().getConstructors()[0].newInstance(args);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }
}
