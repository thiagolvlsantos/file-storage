package io.github.thiagolvlsantos.file.storage;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import io.github.thiagolvlsantos.file.storage.EnableFileStorage.GitStorage;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import({ GitStorage.class })
public @interface EnableFileStorage {

	@Configuration
	@ComponentScan("io.github.thiagolvlsantos.file.storage")
	public static class GitStorage {
	}
}