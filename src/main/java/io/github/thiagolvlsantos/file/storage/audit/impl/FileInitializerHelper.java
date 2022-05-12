package io.github.thiagolvlsantos.file.storage.audit.impl;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import io.github.thiagolvlsantos.file.storage.audit.IFileInitializer;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FileInitializerHelper {

	public static IFileInitializer initializer(ApplicationContext context) {
		IFileInitializer audit;
		try {
			audit = context.getBean(IFileInitializer.class);
		} catch (NoSuchBeanDefinitionException e) {
			audit = IFileInitializer.INSTANCE;
		}
		return audit;
	}
}
