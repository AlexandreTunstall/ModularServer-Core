package atunstall.server.core.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Stores an API version number.
 * When used on a type declaration, this annotation indicates the version of that type and that that type can be injected into a module's constructor.
 * When used on a parameter, this annotation indicates the version of the parameter's type the injected value must be compatible with.
 */
@Documented
@Target({ElementType.TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Version {
    /**
     * The major component of the version number.
     * This component changes every time a non backward-compatible API change is made.
     */
    int major();

    /**
     * The minor component of the version number.
     * This component changes every time the API changes in a backward-compatible way and resets to 0 when the major number changes.
     */
    int minor();
}
