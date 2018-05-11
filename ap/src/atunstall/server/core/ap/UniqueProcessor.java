package atunstall.server.core.ap;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.Optional;
import java.util.Set;

@SupportedSourceVersion(SourceVersion.RELEASE_9)
@SupportedAnnotationTypes(UniqueProcessor.ANNOTATION)
public class UniqueProcessor extends AbstractProcessor {
    static final String ANNOTATION = "atunstall.server.core.api.Unique";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        annotations.forEach(annotation -> roundEnv.getElementsAnnotatedWith(annotation).forEach(element -> {
            AnnotationMirror mirror = getAnnotation(element, annotation.asType()).orElseThrow(IllegalStateException::new);
            if (!getAnnotation(element, processingEnv.getElementUtils().getTypeElement(VersionProcessor.ANNOTATION).asType()).isPresent()) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Type must be annotated with " + VersionProcessor.ANNOTATION, element, mirror);
            }
        }));
        return true;
    }

    private Optional<? extends AnnotationMirror> getAnnotation(AnnotatedConstruct annotated, TypeMirror annotation) {
        return annotated.getAnnotationMirrors().stream().filter(m -> processingEnv.getTypeUtils().isSameType(m.getAnnotationType(), annotation)).findAny();
    }
}
