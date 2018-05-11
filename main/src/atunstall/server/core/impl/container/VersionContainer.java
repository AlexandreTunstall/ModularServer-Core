package atunstall.server.core.impl.container;

import atunstall.server.core.api.Version;

public class VersionContainer extends ComponentContainer {
    private final Version version;
    private final boolean unique;

    VersionContainer(Class<?> component, Version version, boolean unique) {
        super(component);
        this.version = version;
        this.unique = unique;
    }

    public Version getVersion() {
        return version;
    }

    public boolean isUnique() {
        return unique;
    }
}
