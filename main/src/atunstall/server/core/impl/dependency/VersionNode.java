package atunstall.server.core.impl.dependency;

import atunstall.server.core.impl.container.VersionContainer;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

class VersionNode extends Node {
    private final VersionContainer versionContainer;
    private Set<ModuleNode> implementations;
    private ModuleNode preferredImplementation;
    private Object instance;

    VersionNode(DependencyTree tree, VersionContainer versionContainer) {
        super(tree);
        this.versionContainer = versionContainer;
    }

    VersionContainer getVersionContainer() {
        return versionContainer;
    }

    Set<ModuleNode> getImplementations() {
        if (implementations == null) {
            implementations = tree.getModuleNodes().stream().filter(n -> n.getImplemented().contains(this)).collect(Collectors.toSet());
        }
        return implementations;
    }

    void setPreferredImplementation(ModuleNode preferredImplementation) {
        this.preferredImplementation = preferredImplementation;
    }

    ModuleNode getPreferredImplementation() {
        return preferredImplementation;
    }

    Optional<Object> getInstance() {
        if (versionContainer.isUnique()) {
            if (instance != null) {
                return Optional.of(instance);
            }
            Optional<Object> opt = preferredImplementation.getLastInstance();
            opt.ifPresent(obj -> instance = obj);
            return opt;
        }
        return Optional.empty();
    }
}
