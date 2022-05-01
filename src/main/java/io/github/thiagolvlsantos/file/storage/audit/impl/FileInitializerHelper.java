package io.github.thiagolvlsantos.file.storage.audit.impl;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import io.github.thiagolvlsantos.file.storage.audit.IFileInitializer;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class FileInitializerHelper {

	public static IFileInitializer initializer(ApplicationContext context) {
		IFileInitializer audit;
		try {
			audit = context.getBean(IFileInitializer.class);
		} catch (NoSuchBeanDefinitionException e) {
			// log.error(e.getMessage(), e);
			audit = IFileInitializer.INSTANCE;
		}
		return audit;
	}
}
