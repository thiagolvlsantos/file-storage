package io.github.thiagolvlsantos.file.storage.objects;

import org.springframework.stereotype.Component;

import io.github.thiagolvlsantos.file.storage.impl.AFileStorageTypedImpl;

@Component
public class ProjectStorage extends AFileStorageTypedImpl<Project> {

	public ProjectStorage() {
		super(Project.class);
	}
}