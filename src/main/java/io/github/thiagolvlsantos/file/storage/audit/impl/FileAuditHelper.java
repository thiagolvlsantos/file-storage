package io.github.thiagolvlsantos.file.storage.audit.impl;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import io.github.thiagolvlsantos.file.storage.audit.IFileAudit;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class FileAuditHelper {

	public static IFileAudit audit(ApplicationContext context) {
		IFileAudit audit;
		try {
			audit = context.getBean(IFileAudit.class);
		} catch (NoSuchBeanDefinitionException e) {
			// log.error(e.getMessage(), e);
			audit = IFileAudit.INSTANCE;
		}
		return audit;
	}
}
