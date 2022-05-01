package io.github.thiagolvlsantos.file.storage.audit;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.github.thiagolvlsantos.file.storage.audit.impl.FileAuditDefault;

@Retention(RUNTIME)
@Target(FIELD)
public @interface FileCreatedBy {

	Class<? extends IFileAudit> value() default FileAuditDefault.class;

}