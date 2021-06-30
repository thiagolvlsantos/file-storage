package io.github.thiagolvlsantos.git.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import io.github.thiagolvlsantos.git.commons.file.FileUtils;
import io.github.thiagolvlsantos.git.storage.objects.Project;
import io.github.thiagolvlsantos.git.storage.objects.ProjectStorage;
import io.github.thiagolvlsantos.git.storage.objects.SubProject;

@SpringBootTest
class GitStorageApplicationTests {

	@Test
	void testBasicFeatures(@Autowired ApplicationContext context) {
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

			// exists by key
			assertThat(storage.exists(dir, Project.class, project1.getName())).isTrue();
			// exists by example
			assertThat(storage.exists(dir, Project.class, project1)).isTrue();

			// read by key
			project1 = storage.read(dir, Project.class, project1.getName());
			assertThat(project1.getName()).isEqualTo("projectA");

			// read by example
			project1 = storage.read(dir, Project.class, project1);
			assertThat(project1.getName()).isEqualTo("projectA");

			// search by name
			List<Project> list = storage.search(dir, Project.class, "{\"name\":{\"$eq\": \"projectB\"}}");
			assertThat(list).hasSize(1);
			assertThat(list.get(0).getName()).isEqualTo("projectB");

			// delete by key
			storage.delete(dir, Project.class, project1.getName());
			assertThat(storage.exists(dir, Project.class, project1.getName())).isFalse();

			// delete by example
			storage.delete(dir, Project.class, project2);
			assertThat(storage.exists(dir, Project.class, project2)).isFalse();
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testBasicFeaturesTyped(@Autowired ApplicationContext context) {
		IGitStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		String name2 = "projectB";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, project1);
			assertThat(project1.getId()).isNotNull();

			// write other
			Project project2 = Project.builder().name(name2).build();
			project2 = storage.write(dir, project2);
			assertThat(project2.getId()).isNotNull();

			// rewrite first
			Long id = project1.getId();
			LocalDateTime created = project1.getCreated();
			Long revision = project1.getRevision();
			LocalDateTime changed = project1.getChanged();

			project1 = storage.write(dir, project1);
			assertThat(project1.getId()).isEqualTo(id);
			assertThat(project1.getCreated()).isEqualTo(created);
			assertThat(project1.getRevision()).isEqualTo(revision + 1);
			assertThat(project1.getChanged()).isAfter(changed);

			// list all
			assertThat(storage.all(dir)).containsSequence(project1, project2);
			// count all
			assertThat(storage.count(dir)).isEqualTo(2L);

			// exists by key
			assertThat(storage.exists(dir, project1.getName())).isTrue();
			// exists by example
			assertThat(storage.exists(dir, project1)).isTrue();

			// read by key
			project1 = storage.read(dir, project1.getName());
			assertThat(project1.getName()).isEqualTo("projectA");

			// read by example
			project1 = storage.read(dir, project1);
			assertThat(project1.getName()).isEqualTo("projectA");

			// search by name
			List<Project> list = storage.search(dir, "{\"name\":{\"$eq\": \"projectB\"}}");
			assertThat(list).hasSize(1);
			assertThat(list.get(0).getName()).isEqualTo("projectB");

			// delete by key
			storage.delete(dir, project1.getName());
			assertThat(storage.exists(dir, Project.class, project1.getName())).isFalse();

			// delete by example
			storage.delete(dir, project2);
			assertThat(storage.exists(dir, Project.class, project2)).isFalse();
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testInheritance(@Autowired ApplicationContext context) {
		IGitStorage storage = context.getBean(IGitStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());

		try {
			String name1 = "subProjectA";
			String language = "pt";
			SubProject sub = SubProject.builder().name(name1).language(language).build();

			sub = storage.write(dir, sub);
			assertThat(storage.exists(dir, sub)).isTrue();

			sub = storage.read(dir, sub);
			assertThat(sub.getLanguage()).isEqualTo(language);

			sub = storage.delete(dir, sub);
			assertThat(storage.exists(dir, sub)).isFalse();
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}