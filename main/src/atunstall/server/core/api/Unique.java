package atunstall.server.core.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that only one instance of the implementation of this object may be injected as a dependency.
 * The previously injected value will be reused if the annotated type is injected again.
 * The annotated type must be annotated with {@link Version}
 */
@Documented
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Unique {
    // Empty
}
