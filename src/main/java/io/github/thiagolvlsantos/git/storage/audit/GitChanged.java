package io.github.thiagolvlsantos.git.storage.audit;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.github.thiagolvlsantos.git.storage.audit.impl.GitInitializerDefault;

@Retention(RUNTIME)
@Target(FIELD)
public @interface GitChanged {

	Class<? extends IGitInitializer> value() default GitInitializerDefault.class;

}