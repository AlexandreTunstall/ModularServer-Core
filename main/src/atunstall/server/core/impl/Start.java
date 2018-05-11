package atunstall.server.core.impl;

import atunstall.server.core.api.logging.Level;
import atunstall.server.core.api.logging.LogMessage;
import atunstall.server.core.api.logging.Logger;
import atunstall.server.core.impl.container.ComponentContainer;
import atunstall.server.core.impl.dependency.DependencyTree;
import atunstall.server.core.impl.logging.RootLogger;

import java.io.*;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

class Start implements Consumer<LogMessage> {
    private static final Path COMPONENTS_ROOT = Paths.get("components");
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private static final String SERVICES_FILE = "services.txt";
    private static final String MODULES_FILE = "modules.txt";

    private final Logger logger;

    private Start(Logger logger) {
        this.logger = logger;
    }

    public static void main(String[] args) {
        RootLogger rootLogger = new RootLogger();
        Start instance = new Start(rootLogger.getChild("Core"));
        rootLogger.addListener(instance, level -> true);
        ComponentClassLoader cl = instance.new ComponentClassLoader(Start.class.getClassLoader());
        DependencyTree dependencies = new DependencyTree(instance.logger, Stream.concat(instance.loadJars(cl), instance.loadClasspath()).distinct().map(c -> ComponentContainer.toComponent(c, cl)).filter(Optional::isPresent).map(Optional::get));
        dependencies.getModuleNode(RootLogger.class).orElseThrow(IllegalStateException::new).setInstance(rootLogger);
        long start = System.nanoTime();
        dependencies.instantiateModules();
        long end = System.nanoTime();
        instance.logger.log(Level.INFO, String.format("Took %.3f milliseconds", ((double) end - start) / 1000000D));
        AtomicLong count = new AtomicLong();
        do {
            count.set(0L);
            Stream<Thread> threads = Thread.getAllStackTraces().keySet().stream().filter(t -> Thread.currentThread().getThreadGroup().parentOf(t.getThreadGroup())).filter(t -> !t.isDaemon() && t != Thread.currentThread())/*.filter(t -> Thread.State.RUNNABLE.equals(t.getState()) || Thread.State.TIMED_WAITING.equals(t.getState()))*/.peek(t -> count.incrementAndGet());
            instance.logger.log(Level.DEBUG, "Waiting for " + count.longValue() + " threads");
            threads.forEach(t -> {
                try {
                    t.join();
                } catch (InterruptedException ignored) {}
            });
        } while (count.longValue() > 0L);
        rootLogger.terminate();
    }

    @Override
    public void accept(LogMessage message) {
        PrintStream target;
        switch (message.getLevel()) {
            case ERROR:
                target = System.err;
                break;
            default:
                target = System.out;
                break;
        }
        target.printf("[%s] %s | %s\n", message.getLogger().getFullName(), message.getLevel().toString(), message.getMessage());
        message.getThrowable().ifPresent(Throwable::printStackTrace);
    }

    private Stream<String> loadClasspath() {
        logger.log(Level.INFO, "Scanning classpath for components");
        return Stream.of(SERVICES_FILE, MODULES_FILE).flatMap(this::getResources).flatMap(this::parse);
    }

    private Stream<String> loadJars(ComponentClassLoader cl) {
        logger.log(Level.INFO, "Scanning \"" + Start.COMPONENTS_ROOT.toString() + "\" directory for JARs");
        try {
            Files.createDirectories(Start.COMPONENTS_ROOT);
            Set<Path> files = Files.walk(Start.COMPONENTS_ROOT).collect(Collectors.toSet());
            ModuleFinder finder = ModuleFinder.of(files.toArray(new Path[0]));
            Configuration configuration = ModuleLayer.boot().configuration().resolveAndBind(finder, ModuleFinder.ofSystem(), finder.findAll().stream().map(ModuleReference::descriptor).map(ModuleDescriptor::name).collect(Collectors.toList()));
            ModuleLayer.defineModules(configuration, List.of(ModuleLayer.boot()), s -> cl);
            return files.stream().filter(Files::isRegularFile).flatMap(c -> loadJar(c, cl)).collect(Collectors.toSet()).stream();
        } catch (IOException e) {
            logger.log(Level.ERROR, "Error while scanning " + Start.COMPONENTS_ROOT, e);
        }
        return Stream.empty();
    }

    private Stream<String> loadJar(Path path, ComponentClassLoader cl) {
        logger.log(Level.INFO, "Loading JAR at " + path.toString());
        try (FileSystem fs = FileSystems.newFileSystem(path, null)) {
            Set<String> components = StreamSupport.stream(fs.getRootDirectories().spliterator(), false).flatMap(p -> Stream.of(p.resolve(SERVICES_FILE), p.resolve(MODULES_FILE))).filter(Files::isRegularFile).flatMap(this::parse).collect(Collectors.toSet());
            cl.addURL(path.toUri().toURL());
            return components.stream();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error while loading " + path, e);
        }
        return Stream.empty();
    }

    private Stream<String> parse(Path path) {
        try {
            return Files.lines(path);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error while reading " + path, e);
        }
        return Stream.empty();
    }

    private Stream<String> parse(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, UTF_8))) {
            return reader.lines().collect(Collectors.toSet()).stream();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error while scanning classpath", e);
        }
        return Stream.empty();
    }

    private Stream<InputStream> getResources(String resource) {
        return ModuleLayer.boot().modules().stream().map(m -> getResourceAsStream(m, resource)).filter(Objects::nonNull);
    }

    private InputStream getResourceAsStream(Module module, String resource) {
        try {
            return module.getResourceAsStream(resource);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error while scanning classpath", e);
        }
        return null;
    }

    private class ComponentClassLoader extends URLClassLoader {
        ComponentClassLoader(ClassLoader parent) {
            super(new URL[0], parent);
        }

        @Override
        public void addURL(URL url) {
            super.addURL(url);
        }
    }
}
