package io.github.thiagolvlsantos.git.storage.objects;

import org.springframework.stereotype.Component;

import io.github.thiagolvlsantos.git.storage.impl.AGitStorageTypedImpl;

@Component
public class ProjectStorage extends AGitStorageTypedImpl<Project> {

	public ProjectStorage() {
		super(Project.class);
	}
}