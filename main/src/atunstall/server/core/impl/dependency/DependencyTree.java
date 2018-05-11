package atunstall.server.core.impl.dependency;

import atunstall.server.core.api.Version;
import atunstall.server.core.api.logging.Level;
import atunstall.server.core.api.logging.Logger;
import atunstall.server.core.impl.container.ComponentContainer;
import atunstall.server.core.impl.container.ModuleContainer;
import atunstall.server.core.impl.container.VersionContainer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DependencyTree {
    final Logger logger;
    private final Map<Class<?>, ModuleNode> moduleNodes;
    private final Map<Class<?>, VersionNode> versionNodes;

    public DependencyTree(Logger logger, Stream<ComponentContainer> components) {
        this.logger = logger;
        logger.log(Level.INFO, "Building the dependency tree");
        moduleNodes = new HashMap<>();
        versionNodes = new HashMap<>();
        components.filter(c -> c instanceof ModuleContainer || c instanceof VersionContainer).forEach(this::toNode);
        if (moduleNodes.values().stream().anyMatch(this::checkDependencies)) {
            logger.log(Level.ERROR, "Shutting down due to mismatching dependency versions");
            System.exit(1);
        }
        Set<Node> visited = new HashSet<>();
        moduleNodes.values().forEach(node -> {
            List<Node> parents = new ArrayList<>();
            Deque<Iterator<? extends Node>> iterators = new ArrayDeque<>();
            parents.add(node);
            visited.add(node);
            iterators.add(node.getDependencies().keySet().iterator());
            boolean removeParent = false;
            while (iterators.size() > 0) {
                Iterator<? extends Node> iterator = iterators.peekLast();
                if (iterator.hasNext()) {
                    Node next = iterator.next();
                    if (visited.add(node)) {
                        parents.add(next);
                        iterators.add((next instanceof ModuleNode ? ((ModuleNode) next).getDependencies().keySet() : ((VersionNode) next).getImplementations()).iterator());
                    } else if (parents.contains(next)) {
                        if (next instanceof ModuleNode && ((VersionNode) parents.get(parents.size())).getImplementations().size() > 1) {
                            iterator.remove();
                        } else {
                            removeParent = true;
                        }
                    }
                } else {
                    parents.remove(parents.size() - 1);
                    iterators.removeLast();
                    if (removeParent && ((VersionNode) parents.get(parents.size())).getImplementations().size() > 1) {
                        iterators.peekLast().remove();
                        removeParent = false;
                    }
                }
            }
            if (removeParent) {
                throw new IllegalStateException("cyclic dependency with no alternatives");
            }
        });
        versionNodes.values().forEach(node -> node.setPreferredImplementation(node.getImplementations().stream().findAny().orElse(null)));
    }

    public void instantiateModules() {
        Optional<ModuleNode> node;
        while ((node = moduleNodes.values().stream().filter(n -> !n.getLastInstance().isPresent()).findAny()).isPresent()) {
            node.get().createInstance();
        }
    }

    public Optional<ModuleNode> getModuleNode(Class<?> type) {
        return Optional.ofNullable(moduleNodes.get(type));
    }

    Optional<VersionNode> getVersionNode(Class<?> type) {
        return Optional.ofNullable(versionNodes.get(type));
    }

    Collection<ModuleNode> getModuleNodes() {
        return moduleNodes.values();
    }

    private Node toNode(ComponentContainer componentContainer) {
        logger.log(Level.DEBUG, "Creating node for " + componentContainer.getComponent().getName());
        if (componentContainer instanceof VersionContainer) {
            VersionNode result;
            versionNodes.put(componentContainer.getComponent(), result = new VersionNode(this, (VersionContainer) componentContainer));
            return result;
        } else if (componentContainer instanceof ModuleContainer) {
            ModuleNode result;
            moduleNodes.put(componentContainer.getComponent(), result = new ModuleNode(this, (ModuleContainer) componentContainer));
            return result;
        }
        throw new IllegalArgumentException("component must be either a versioned interface or a module");
    }

    private boolean checkDependencies(ModuleNode node) {
        Map<VersionNode, Version> outdated = node.getDependencies().entrySet().stream().filter(e -> compareVersions(e.getKey(), e.getValue())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        switch (outdated.size()) {
            case 0:
                return false;
            default:
                outdated.forEach((versionNode, version) -> {
                    logger.log(Level.ERROR, String.format("Mismatching dependencies for module: %s", node.getModuleContainer().getComponent().getName()));
                    logger.log(Level.ERROR, String.format("\tDependency: %s\tAvailable: %s\tNeeded: %s", versionNode.getVersionContainer().getComponent().getName(), toString(versionNode.getVersionContainer().getVersion()), toString(version)));
                });
        }
        return true;
    }

    private boolean compareVersions(VersionNode dependency, Version moduleVersion) {
        Version version = dependency.getVersionContainer().getVersion();
        return moduleVersion.major() != version.major() || moduleVersion.minor() > version.minor();
    }

    private String toString(Version version) {
        return String.format("%d.%d", version.major(), version.minor());
    }
}
