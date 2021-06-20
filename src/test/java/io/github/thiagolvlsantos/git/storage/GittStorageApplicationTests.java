package io.github.thiagolvlsantos.git.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

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
		String name1 = "projectA";
		String name2 = "projectB";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, Project.class, project1);
			assertThat(project1.getId()).isNotNull();

			// write other
			Project project2 = Project.builder().name(name2).build();
			project2 = storage.write(dir, Project.class, project2);
			assertThat(project2.getId()).isNotNull();

			// rewrite first
			Long id = project1.getId();
			LocalDateTime created = project1.getCreated();
			Long revision = project1.getRevision();
			LocalDateTime changed = project1.getChanged();

			project1 = storage.write(dir, Project.class, project1);
			assertThat(project1.getId()).isEqualTo(id);
			assertThat(project1.getCreated()).isEqualTo(created);
			assertThat(project1.getRevision()).isEqualTo(revision + 1);
			assertThat(project1.getChanged()).isAfter(changed);

			// list all
			assertThat(storage.all(dir, Project.class)).containsSequence(project1, project2);
			// count all
			assertThat(storage.count(dir, Project.class)).isEqualTo(2L);

			// exists by id
			assertThat(storage.exists(dir, Project.class, project1.getName())).isTrue();
			// exists by example
			assertThat(storage.exists(dir, Project.class, project1)).isTrue();

			// read by id
			project1 = storage.read(dir, Project.class, project1.getName());
			assertThat(project1.getName()).isEqualTo("projectA");

			// read by example
			project1 = storage.read(dir, Project.class, project1);
			assertThat(project1.getName()).isEqualTo("projectA");

			// delete by id
			storage.delete(dir, Project.class, project1.getName());
			assertThat(storage.exists(dir, Project.class, project1.getName())).isFalse();

			// delete by example
			project1 = storage.write(dir, Project.class, project1);
			assertThat(project1.getId()).isNotNull();

			storage.delete(dir, Project.class, project1);
			assertThat(storage.exists(dir, Project.class, project1)).isFalse();
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}