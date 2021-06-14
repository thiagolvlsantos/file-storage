package com.thiagolvlsantos.git.storage;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.thiagolvlsantos.git.storage.EnableGitStorage.GitStorage;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import({ GitStorage.class })
public @interface EnableGitStorage {

	@Configuration
	@ComponentScan("com.thiagolvlsantos.git.storage")
	public static class GitStorage {
	}
}