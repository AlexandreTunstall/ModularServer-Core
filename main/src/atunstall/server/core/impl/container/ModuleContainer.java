package atunstall.server.core.impl.container;

import atunstall.server.core.api.Module;

public class ModuleContainer extends ComponentContainer {
    private final Module module;

    ModuleContainer(Class<?> component, Module module) {
        super(component);
        this.module = module;
    }

    public Module getModule() {
        return module;
    }
}
