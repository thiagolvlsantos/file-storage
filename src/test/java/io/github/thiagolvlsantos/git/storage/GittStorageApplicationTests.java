package io.github.thiagolvlsantos.git.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import io.github.thiagolvlsantos.git.commons.file.FileUtils;
import io.github.thiagolvlsantos.git.storage.objects.Project;

@SpringBootTest
class GittStorageApplicationTests {

	@Test
	void contextLoads(@Autowired ApplicationContext context) {
		IGitStorage storage = context.getBean(IGitStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		try {
			// write
			Project project = Project.builder().name("projectA").build();
			if (storage.exists(dir, Project.class, project)) {
				storage.delete(dir, Project.class, project);
			}

			project = storage.write(dir, Project.class, project);
			assertThat(project.getId()).isNotNull();

			// all
			assertThat(storage.all(dir, Project.class)).matches(p -> p.size() == 1);
			// count
			assertThat(storage.count(dir, Project.class)).isEqualTo(1L);

			// exists by id
			assertThat(storage.exists(dir, Project.class, project.getName())).isTrue();
			// exists by example
			assertThat(storage.exists(dir, Project.class, project)).isTrue();

			// read by id
			project = storage.read(dir, Project.class, project.getName());
			assertThat(project.getName()).isEqualTo("projectA");

			// read by example
			project = storage.read(dir, Project.class, project);
			assertThat(project.getName()).isEqualTo("projectA");

			// delete by id
			storage.delete(dir, Project.class, project.getName());
			assertThat(storage.exists(dir, Project.class, project.getName())).isFalse();

			// delete by example
			project = storage.write(dir, Project.class, project);
			assertThat(project.getId()).isNotNull();

			storage.delete(dir, Project.class, project);
			assertThat(storage.exists(dir, Project.class, project)).isFalse();
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}