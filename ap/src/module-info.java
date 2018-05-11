module atunstall.server.core.ap {
    requires java.compiler;
    provides javax.annotation.processing.Processor with atunstall.server.core.ap.ModuleProcessor, atunstall.server.core.ap.UniqueProcessor, atunstall.server.core.ap.VersionProcessor;
}