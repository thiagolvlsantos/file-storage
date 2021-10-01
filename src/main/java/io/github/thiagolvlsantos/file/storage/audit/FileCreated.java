package io.github.thiagolvlsantos.file.storage.audit;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.github.thiagolvlsantos.file.storage.audit.impl.GitInitializerDefault;

@Retention(RUNTIME)
@Target(FIELD)
public @interface FileCreated {

	Class<? extends IFileInitializer> value() default GitInitializerDefault.class;
}