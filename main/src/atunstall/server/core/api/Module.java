package atunstall.server.core.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated type is a module.
 * This module may also be used as the implementation of any of its {@link Version}-annotated superinterfaces.
 * The annotated type must only have one constructor and that constructors' parameters must all be {@link Version}-annotated interfaces.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Module {
    // Empty
}
