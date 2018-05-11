package atunstall.server.core.ap;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedSourceVersion(SourceVersion.RELEASE_9)
@SupportedAnnotationTypes(ModuleProcessor.ANNOTATION)
public class ModuleProcessor extends AbstractProcessor {
    @SuppressWarnings("WeakerAccess")
    static final String ANNOTATION = "atunstall.server.core.api.Module";

    private final Set<TypeElement> modules = new HashSet<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        annotations.forEach(annotation -> roundEnv.getElementsAnnotatedWith(annotation).forEach(element -> {
            AnnotationMirror mirror = getAnnotation(element, annotation.asType()).orElseThrow(IllegalStateException::new);
            if (!ElementKind.CLASS.equals(element.getKind())) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Element is not a class", element, mirror);
            } else {
                modules.add((TypeElement) element);
            }
            Set<Element> constructors = ((TypeElement) element).getEnclosedElements().stream().filter(e -> ElementKind.CONSTRUCTOR.equals(e.getKind())).filter(e -> e.getModifiers().contains(Modifier.PUBLIC)).collect(Collectors.toSet());
            if (constructors.size() == 1) {
                ExecutableElement constructor = (ExecutableElement) constructors.iterator().next();
                constructor.getParameters().stream().filter(e -> !getAnnotation(e, processingEnv.getElementUtils().getTypeElement(VersionProcessor.ANNOTATION).asType()).isPresent())
                        .forEach(e -> processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Parameter must be annotated with " + VersionProcessor.ANNOTATION, e));
            } else {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Element must have only one public constructor", element, mirror);
            }
        }));
        if (roundEnv.processingOver()) {
            saveServices();
        }
        return true;
    }

    private void saveServices() {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Storing " + modules.size() + " module classes in modules.txt");
        try {
            FileObject file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "modules.txt", modules.toArray(new TypeElement[0]));
            try (Writer writer = file.openWriter()) {
                for (TypeElement element : modules) {
                    writer.append(processingEnv.getElementUtils().getBinaryName(element)).append('\n');
                }
            }
        } catch (IOException e) {
            System.err.println("Error whilst trying to create modules.txt");
            e.printStackTrace();
        }
    }

    private Optional<? extends AnnotationMirror> getAnnotation(AnnotatedConstruct annotated, TypeMirror annotation) {
        return annotated.getAnnotationMirrors().stream().filter(m -> processingEnv.getTypeUtils().isSameType(m.getAnnotationType(), annotation)).findAny();
    }
}
