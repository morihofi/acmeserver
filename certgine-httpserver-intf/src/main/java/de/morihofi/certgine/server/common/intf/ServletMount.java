package de.morihofi.certgine.server.common.intf;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ServletMount {
    String servletMountPoint();
    boolean protect() default false;
}
