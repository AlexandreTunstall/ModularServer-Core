package atunstall.server.core.ap;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@SupportedSourceVersion(SourceVersion.RELEASE_9)
@SupportedAnnotationTypes(VersionProcessor.ANNOTATION)
public class VersionProcessor extends AbstractProcessor {
    static final String ANNOTATION = "atunstall.server.core.api.Version";
    private static final String SUPPLIER = "java.util.function.Supplier";
    private static final String MAJOR_NAME = "major";
    private static final String MINOR_NAME = "minor";

    private final Set<TypeElement> services = new HashSet<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        annotations.forEach(annotation -> roundEnv.getElementsAnnotatedWith(annotation).forEach(element -> {
            AnnotationMirror mirror = getAnnotation(element, annotation.asType()).orElseThrow(IllegalStateException::new);
            switch (element.getKind()) {
                case INTERFACE:
                    services.add((TypeElement) element);
                    break;
                case PARAMETER:
                    if (!ElementKind.CONSTRUCTOR.equals(element.getEnclosingElement().getKind())) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Enclosing element is not a constructor", element, mirror);
                    }
                    Optional<? extends AnnotationMirror> interfaceMirror;
                    if (!TypeKind.DECLARED.equals(element.asType().getKind())) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Parameter is not a declared type", element, mirror);
                        break;
                    }
                    TypeElement type = (TypeElement) processingEnv.getTypeUtils().asElement(element.asType());
                    if (SUPPLIER.contentEquals(type.getQualifiedName())) {
                        type = (TypeElement) processingEnv.getTypeUtils().asElement(((DeclaredType) element.asType()).getTypeArguments().get(0));
                        getAnnotation(type, processingEnv.getElementUtils().getTypeElement(UniqueProcessor.ANNOTATION).asType()).ifPresent(m -> processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Parameter's type is annotated with Unique"));
                    }
                    interfaceMirror = getAnnotation(type, annotation.asType());
                    if (!interfaceMirror.isPresent()) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Parameter's type is not annotated with Version", element, mirror);
                    }
                    interfaceMirror.map(processingEnv.getElementUtils()::getElementValuesWithDefaults).ifPresent(values -> {
                        Map<? extends ExecutableElement, ? extends AnnotationValue> paramValues = processingEnv.getElementUtils().getElementValuesWithDefaults(mirror);
                        values.forEach((k, v) -> {
                            if ((MAJOR_NAME.contentEquals(k.getSimpleName()) || MINOR_NAME.contentEquals(k.getSimpleName())) && !Objects.equals(paramValues.get(k).getValue(), v.getValue())) {
                                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Version is not equal to interface Version", element, mirror, paramValues.get(k));
                            }
                        });
                    });
                    break;
                default:
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Element is not an interface or a constructor parameter", element, mirror);
                    break;
            }
        }));
        if (roundEnv.processingOver()) {
            saveServices();
        }
        return true;
    }

    private void saveServices() {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Storing " + services.size() + " service classes in services.txt");
        try {
            FileObject file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "services.txt", services.toArray(new TypeElement[0]));
            try (Writer writer = file.openWriter()) {
                for (TypeElement element : services) {
                    writer.append(processingEnv.getElementUtils().getBinaryName(element)).append('\n');
                }
            }
        } catch (IOException e) {
            System.err.println("Error whilst trying to create services.txt");
            e.printStackTrace();
        }
    }

    private Optional<? extends AnnotationMirror> getAnnotation(AnnotatedConstruct annotated, TypeMirror annotation) {
        return annotated.getAnnotationMirrors().stream().filter(m -> processingEnv.getTypeUtils().isSameType(m.getAnnotationType(), annotation)).findAny();
    }
}
