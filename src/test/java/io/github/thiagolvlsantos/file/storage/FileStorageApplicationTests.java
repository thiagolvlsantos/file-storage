package io.github.thiagolvlsantos.file.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import io.github.thiagolvlsantos.file.storage.exceptions.FileStorageAttributeNotFoundException;
import io.github.thiagolvlsantos.file.storage.exceptions.FileStorageException;
import io.github.thiagolvlsantos.file.storage.exceptions.FileStorageNotFoundException;
import io.github.thiagolvlsantos.file.storage.exceptions.FileStorageResourceNotFoundException;
import io.github.thiagolvlsantos.file.storage.exceptions.FileStorageSecurityException;
import io.github.thiagolvlsantos.file.storage.objects.Outlier;
import io.github.thiagolvlsantos.file.storage.objects.OutlierStorage;
import io.github.thiagolvlsantos.file.storage.objects.Project;
import io.github.thiagolvlsantos.file.storage.objects.ProjectStorage;
import io.github.thiagolvlsantos.file.storage.objects.SubProject;
import io.github.thiagolvlsantos.file.storage.resource.Resource;
import io.github.thiagolvlsantos.file.storage.resource.ResourceContent;
import io.github.thiagolvlsantos.file.storage.resource.ResourceMetadata;
import io.github.thiagolvlsantos.git.commons.file.FileUtils;
import io.github.thiagolvlsantos.json.predicate.IPredicateFactory;
import io.github.thiagolvlsantos.json.predicate.impl.PredicateFactoryJson;

@SpringBootTest
class FileStorageApplicationTests {

	private IPredicateFactory factory = new PredicateFactoryJson();

	@Test
	void testInvalidEntity(@Autowired ApplicationContext context) {
		IFileStorage storage = context.getBean(IFileStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		try {
			final Outlier instance = new Outlier();
			assertThatThrownBy(() -> storage.write(dir, instance))//
					.isExactlyInstanceOf(FileStorageException.class)//
					.hasMessage("Entity is not annotated with @FileEntity.");
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testInvalidEntityTyped(@Autowired ApplicationContext context) {
		IFileStorageTyped<Outlier> storage = context.getBean(OutlierStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		try {
			final Outlier instance = new Outlier();
			assertThatThrownBy(() -> storage.write(dir, instance))//
					.isExactlyInstanceOf(FileStorageException.class)//
					.hasMessage("Entity is not annotated with @FileEntity.");
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testInvalidRevisionReuse(@Autowired ApplicationContext context) {
		IFileStorage storage = context.getBean(IFileStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());

		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).revision(0L).build();
			Project result = storage.write(dir, Project.class, project1);
			assertThat(result.getId()).isNotNull();

			// try update with old revision
			project1.setRevision(0L);
			assertThatThrownBy(() -> storage.write(dir, Project.class, project1))//
					.isExactlyInstanceOf(FileStorageException.class)//
					.hasMessage("Invalid revision. Reload object and try again.");
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testInvalidRevisionReuseTyped(@Autowired ApplicationContext context) {
		IFileStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());

		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).revision(0L).build();
			Project result = storage.write(dir, project1);
			assertThat(result.getId()).isNotNull();

			// try update with old revision
			project1.setRevision(0L);
			assertThatThrownBy(() -> storage.write(dir, project1))//
					.isExactlyInstanceOf(FileStorageException.class)//
					.hasMessage("Invalid revision. Reload object and try again.");
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testBasicFeatures(@Autowired ApplicationContext context) {
		IFileStorage storage = context.getBean(IFileStorage.class);
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
			assertThat(storage.list(dir, Project.class, null, null, FileSorting.builder().property("name").build()))
					.contains(project1, project2);
			// count all
			assertThat(storage.count(dir, Project.class, null, null)).isEqualTo(2L);

			// exists by key
			assertThat(storage.exists(dir, Project.class, FileParams.of(project1.getName()))).isTrue();
			// exists by example
			assertThat(storage.exists(dir, Project.class, project1)).isTrue();

			// read by key
			project1 = storage.read(dir, Project.class, FileParams.of(project1.getName()));
			assertThat(project1.getName()).isEqualTo("projectA");

			// read by example
			project1 = storage.read(dir, Project.class, project1);
			assertThat(project1.getName()).isEqualTo("projectA");

			// search by name
			List<Project> list = storage.list(dir, Project.class,
					new FilePredicate(factory.read("{\"name\":{\"$eq\": \"projectB\"}}".getBytes())), null, null);
			assertThat(list).hasSize(1);
			assertThat(list.get(0).getName()).isEqualTo("projectB");

			// delete by key
			storage.delete(dir, Project.class, FileParams.of(project1.getName()));
			assertThat(storage.exists(dir, Project.class, FileParams.of(project1.getName()))).isFalse();

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
		IFileStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
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
			assertThat(storage.list(dir, null, FilePaging.builder().skip(0).build(),
					FileSorting.builder().property("name").nullsFirst(true).build())).contains(project1, project2);
			// count all, skip 1
			assertThat(storage.count(dir, null, FilePaging.builder().skip(1).max(1).build())).isEqualTo(1L);

			// exists by key
			assertThat(storage.exists(dir, FileParams.of(project1.getName()))).isTrue();
			// exists by example
			assertThat(storage.exists(dir, project1)).isTrue();

			// read by key
			project1 = storage.read(dir, FileParams.of(project1.getName()));
			assertThat(project1.getName()).isEqualTo("projectA");

			// read by example
			project1 = storage.read(dir, project1);
			assertThat(project1.getName()).isEqualTo("projectA");

			// search by name
			List<Project> list = storage.list(dir,
					new FilePredicate(factory.read("{\"name\":{\"$eq\": \"projectB\"}}".getBytes())), null, null);
			assertThat(list).hasSize(1);
			assertThat(list.get(0).getName()).isEqualTo("projectB");

			// delete by key
			storage.delete(dir, FileParams.of(project1.getName()));
			assertThat(storage.exists(dir, FileParams.of(project1.getName()))).isFalse();

			// delete by example
			storage.delete(dir, project2);
			assertThat(storage.exists(dir, project2)).isFalse();
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
		IFileStorage storage = context.getBean(IFileStorage.class);
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

	@Test
	void testUpdate(@Autowired ApplicationContext context) {
		IFileStorage storage = context.getBean(IFileStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, Project.class, project1);
			assertThat(project1.getId()).isNotNull();

			// read value that cannot change
			Long id = project1.getId();
			LocalDateTime created = project1.getCreated();
			Long revision = project1.getRevision();

			Project newVersion = Project.builder().name(name1).build();
			String description = "This is a new description.";
			newVersion.setDescription(description);
			project1 = storage.merge(dir, Project.class, FileParams.of(name1), newVersion);

			// old attributes
			assertThat(project1.getId()).isEqualTo(id);
			assertThat(project1.getName()).isEqualTo(name1);
			assertThat(project1.getCreated()).isEqualTo(created);
			assertThat(project1.getRevision()).isEqualTo(revision + 1);

			// new attributes
			assertThat(project1.getDescription()).isEqualTo(description);

			// update valid attribute
			description = "Final version";
			project1 = storage.setAttribute(dir, Project.class, FileParams.of(name1), "description", description);

			// changed attribute
			assertThat(project1.getDescription()).isEqualTo(description);
			assertThat(project1.getRevision()).isEqualTo(revision + 2);
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testUpdateTyped(@Autowired ApplicationContext context) {
		IFileStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, project1);
			assertThat(project1.getId()).isNotNull();

			// read value that cannot change
			Long id = project1.getId();
			LocalDateTime created = project1.getCreated();
			Long revision = project1.getRevision();

			Project newVersion = Project.builder().name(name1).build();
			String description = "This is a new description.";
			newVersion.setDescription(description);
			project1 = storage.merge(dir, FileParams.of(name1), newVersion);

			// old attributes
			assertThat(project1.getId()).isEqualTo(id);
			assertThat(project1.getName()).isEqualTo(name1);
			assertThat(project1.getCreated()).isEqualTo(created);
			assertThat(project1.getRevision()).isEqualTo(revision + 1);

			// new attributes
			assertThat(project1.getDescription()).isEqualTo(description);

			// update valid attribute
			description = "Final version";
			project1 = storage.setAttribute(dir, FileParams.of(name1), "description", description);

			// changed attribute
			assertThat(project1.getDescription()).isEqualTo(description);
			assertThat(project1.getRevision()).isEqualTo(revision + 2);
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testInvalidMerge(@Autowired ApplicationContext context) {
		IFileStorage storage = context.getBean(IFileStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		try {
			final Project instance = new Project();
			FileParams params = FileParams.of("doNotExist");
			assertThatThrownBy(() -> {
				storage.merge(dir, Project.class, params, instance);
			}).isExactlyInstanceOf(FileStorageNotFoundException.class)//
					.hasMessage("Object '" + Project.class.getSimpleName() + "' with keys '"
							+ FileParams.of("doNotExist") + "' not found.");
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testInvalidMergeTyped(@Autowired ApplicationContext context) {
		IFileStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		try {
			final Project instance = new Project();
			FileParams params = FileParams.of("doNotExist");
			assertThatThrownBy(() -> {
				storage.merge(dir, params, instance);
			}).isExactlyInstanceOf(FileStorageNotFoundException.class)//
					.hasMessage("Object '" + Project.class.getSimpleName() + "' with keys '"
							+ FileParams.of("doNotExist") + "' not found.");
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testInvalidSetAttributeName(@Autowired ApplicationContext context) {
		IFileStorage storage = context.getBean(IFileStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, Project.class, project1);

			FileParams params = FileParams.of(name1);
			assertThatThrownBy(() -> {
				storage.setAttribute(dir, Project.class, params, "title", "newDescription");
			}).isExactlyInstanceOf(FileStorageAttributeNotFoundException.class)//
					.hasMessage(new FileStorageAttributeNotFoundException("title", project1, null).getMessage());
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testInvalidSetAttributeNameTyped(@Autowired ApplicationContext context) {
		IFileStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, project1);

			FileParams params = FileParams.of(name1);
			assertThatThrownBy(() -> {
				storage.setAttribute(dir, params, "title", "newDescription");
			}).isExactlyInstanceOf(FileStorageAttributeNotFoundException.class)//
					.hasMessage(new FileStorageAttributeNotFoundException("title", project1, null).getMessage());
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testInvalidGetAttributeName(@Autowired ApplicationContext context) {
		IFileStorage storage = context.getBean(IFileStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, Project.class, project1);

			FileParams params = FileParams.of(name1);
			assertThatThrownBy(() -> {
				storage.getAttribute(dir, Project.class, params, "title");
			}).isExactlyInstanceOf(FileStorageAttributeNotFoundException.class)//
					.hasMessage(new FileStorageAttributeNotFoundException("title", project1, null).getMessage());
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testInvalidGetAttributeNameTyped(@Autowired ApplicationContext context) {
		IFileStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, project1);

			FileParams params = FileParams.of(name1);
			assertThatThrownBy(() -> {
				storage.getAttribute(dir, params, "title");
			}).isExactlyInstanceOf(FileStorageAttributeNotFoundException.class)//
					.hasMessage(new FileStorageAttributeNotFoundException("title", project1, null).getMessage());
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testInvalidAllAttributeName(@Autowired ApplicationContext context) {
		IFileStorage storage = context.getBean(IFileStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, Project.class, project1);

			FileParams params = FileParams.of(name1);
			FileParams names = FileParams.of("title");
			assertThatThrownBy(() -> {
				storage.attributes(dir, Project.class, params, names);
			}).isExactlyInstanceOf(FileStorageAttributeNotFoundException.class)//
					.hasMessage(new FileStorageAttributeNotFoundException("title", project1, null).getMessage());
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testInvalidAllAttributeNameTyped(@Autowired ApplicationContext context) {
		IFileStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, project1);

			FileParams params = FileParams.of(name1);
			FileParams names = FileParams.of("title");
			assertThatThrownBy(() -> {
				storage.attributes(dir, params, names);
			}).isExactlyInstanceOf(FileStorageAttributeNotFoundException.class)//
					.hasMessage(new FileStorageAttributeNotFoundException("title", project1, null).getMessage());
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testInvalidGetResources(@Autowired ApplicationContext context) {
		IFileStorage storage = context.getBean(IFileStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, Project.class, project1);

			FileParams params = FileParams.of(name1);
			assertThatThrownBy(() -> {
				storage.getResource(dir, Project.class, params, "css/example.css");
			}).isExactlyInstanceOf(FileStorageResourceNotFoundException.class)//
					.hasMessage("Resource not found: 'css/example.css'.");
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testInvalidGetResourcesTyped(@Autowired ApplicationContext context) {
		IFileStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, project1);

			FileParams params = FileParams.of(name1);
			assertThatThrownBy(() -> {
				storage.getResource(dir, params, "css/example.css");
			}).isExactlyInstanceOf(FileStorageResourceNotFoundException.class)//
					.hasMessage("Resource not found: 'css/example.css'.");
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testInvalidUpdate(@Autowired ApplicationContext context) {
		IFileStorage storage = context.getBean(IFileStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, Project.class, project1);
			assertThat(project1.getId()).isNotNull();

			// try update invalid attributes
			FileParams params = FileParams.of(name1);
			assertThatThrownBy(() -> {
				storage.setAttribute(dir, Project.class, params, "id", "\"10\"");
			}).isExactlyInstanceOf(FileStorageException.class)//
					.hasMessage("Update of @FileId annotated attribute 'id' is not allowed.");

			// try update invalid attributes
			assertThatThrownBy(() -> storage.setAttribute(dir, Project.class, params, "name", "\"newName\""))//
					.isExactlyInstanceOf(FileStorageException.class)//
					.hasMessage("Update of @FileKey annotated attribute 'name' is not allowed.");

			// try update invalid attributes
			assertThatThrownBy(() -> storage.setAttribute(dir, Project.class, params, "created", "\"10\""))//
					.isExactlyInstanceOf(FileStorageException.class)//
					.hasMessage("Update of @FileCreated annotated attribute 'created' is not allowed.");

			// try update invalid attributes
			assertThatThrownBy(() -> storage.setAttribute(dir, Project.class, params, "revision", "\"10\""))//
					.isExactlyInstanceOf(FileStorageException.class)//
					.hasMessage("Update of @FileRevision annotated attribute 'revision' is not allowed.");
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testInvalidUpdateTyped(@Autowired ApplicationContext context) {
		IFileStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, project1);
			assertThat(project1.getId()).isNotNull();

			// try update invalid attributes
			FileParams params = FileParams.of(name1);
			assertThatThrownBy(() -> {
				storage.setAttribute(dir, params, "id", "\"10\"");
			}).isExactlyInstanceOf(FileStorageException.class)//
					.hasMessage("Update of @FileId annotated attribute 'id' is not allowed.");

			// try update invalid attributes
			assertThatThrownBy(() -> storage.setAttribute(dir, params, "name", "\"newName\""))//
					.isExactlyInstanceOf(FileStorageException.class)//
					.hasMessage("Update of @FileKey annotated attribute 'name' is not allowed.");

			// try update invalid attributes
			assertThatThrownBy(() -> storage.setAttribute(dir, params, "created", "\"10\""))//
					.isExactlyInstanceOf(FileStorageException.class)//
					.hasMessage("Update of @FileCreated annotated attribute 'created' is not allowed.");

			// try update invalid attributes
			assertThatThrownBy(() -> storage.setAttribute(dir, params, "revision", "\"10\""))//
					.isExactlyInstanceOf(FileStorageException.class)//
					.hasMessage("Update of @FileRevision annotated attribute 'revision' is not allowed.");
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testInvalidSetUpdate(@Autowired ApplicationContext context) {
		IFileStorage storage = context.getBean(IFileStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		try {
			FileParams params = FileParams.of("doNotExist");
			assertThatThrownBy(() -> {
				storage.setAttribute(dir, Project.class, params, "description", "10");
			}).isExactlyInstanceOf(FileStorageNotFoundException.class)//
					.hasMessage("Object '" + Project.class.getSimpleName() + "' with keys '"
							+ FileParams.of("doNotExist") + "' not found.");
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testInvalidSetUpdateTyped(@Autowired ApplicationContext context) {
		IFileStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		try {
			FileParams params = FileParams.of("doNotExist");
			assertThatThrownBy(() -> {
				storage.setAttribute(dir, params, "description", "10");
			}).isExactlyInstanceOf(FileStorageNotFoundException.class)//
					.hasMessage("Object '" + Project.class.getSimpleName() + "' with keys '"
							+ FileParams.of("doNotExist") + "' not found.");
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testInvalidGetUpdate(@Autowired ApplicationContext context) {
		IFileStorage storage = context.getBean(IFileStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		try {
			FileParams params = FileParams.of("doNotExist");
			assertThatThrownBy(() -> {
				storage.getAttribute(dir, Project.class, params, "description");
			}).isExactlyInstanceOf(FileStorageNotFoundException.class)//
					.hasMessage("Object '" + Project.class.getSimpleName() + "' with keys '" + params + "' not found.");
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testInvalidGetUpdateTyped(@Autowired ApplicationContext context) {
		IFileStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		try {
			FileParams params = FileParams.of("doNotExist");
			assertThatThrownBy(() -> {
				storage.getAttribute(dir, params, "description");
			}).isExactlyInstanceOf(FileStorageNotFoundException.class)//
					.hasMessage("Object '" + Project.class.getSimpleName() + "' with keys '" + params + "' not found.");
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testAttributes(@Autowired ApplicationContext context) {
		IFileStorage storage = context.getBean(IFileStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, Project.class, project1);

			// attribute reading
			FileParams params = FileParams.of(name1);
			Object attribute = storage.getAttribute(dir, Project.class, params, "name");
			assertThat(attribute).isEqualTo(name1);

			// attribute writing
			Project result = storage.setAttribute(dir, Project.class, params, "description", "newDescription");
			assertThat(result.getDescription()).isEqualTo("newDescription");

			// attribute full map
			Map<String, Object> objs = storage.attributes(dir, Project.class, params, null);
			assertThat(objs).containsEntry("description", "newDescription");

			// attribute map projection
			FileParams names = FileParams.of(Arrays.asList("name", "created"));
			objs = storage.attributes(dir, Project.class, params, names);
			assertThat(objs).hasSize(2);

			// invalid attribute
			FileParams name = FileParams.of("title");
			assertThatThrownBy(() -> {
				storage.attributes(dir, Project.class, params, name);
			}).isExactlyInstanceOf(FileStorageAttributeNotFoundException.class)//
					.hasMessage(new FileStorageAttributeNotFoundException("title", project1, null).getMessage());
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testAttributesTyped(@Autowired ApplicationContext context) {
		IFileStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, project1);

			// attribute reading
			FileParams params = FileParams.of(name1);
			Object attribute = storage.getAttribute(dir, params, "name");
			assertThat(attribute).isEqualTo(name1);

			// attribute writing
			Project result = storage.setAttribute(dir, params, "description", "newDescription");
			assertThat(result.getDescription()).isEqualTo("newDescription");

			// attribute full map
			Map<String, Object> objs = storage.attributes(dir, params, null);
			assertThat(objs).containsEntry("description", "newDescription");

			// attribute map projection
			FileParams names = FileParams.of(Arrays.asList("name", "created"));
			objs = storage.attributes(dir, params, names);
			assertThat(objs).hasSize(2);

			// invalid attribute
			FileParams name = FileParams.of("title");
			assertThatThrownBy(() -> {
				storage.attributes(dir, params, name);
			}).isExactlyInstanceOf(FileStorageAttributeNotFoundException.class)//
					.hasMessage(new FileStorageAttributeNotFoundException("title", project1, null).getMessage());
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testResources(@Autowired ApplicationContext context) {
		IFileStorage storage = context.getBean(IFileStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, Project.class, project1);

			// attribute reading
			String path = "css/style.css";
			ResourceMetadata metadata = ResourceMetadata.builder().path(path).contentType("css").build();
			ResourceContent content = ResourceContent.builder().data(".table { width: 100%; }".getBytes()).build();
			Resource resource = Resource.builder().metadata(metadata).content(content).build();

			// writing
			FileParams params = FileParams.of(name1);
			Project result = storage.setResource(dir, Project.class, params, resource);

			// reading
			Resource outcome = storage.getResource(dir, Project.class, params, path);

			// object setup
			assertThat(project1.getRevision()).isEqualTo(result.getRevision() - 1);
			assertThat(project1.getChanged()).isBefore(result.getChanged());

			// serialization and deserialization
			assertThat(outcome.getMetadata().getPath()).isEqualTo(outcome.getMetadata().getPath());
			assertThat(outcome.getMetadata().getContentType()).isEqualTo(outcome.getMetadata().getContentType());
			assertThat(new String(outcome.getContent().getData()))
					.isEqualTo(new String(resource.getContent().getData()));

			// timestamp assigned
			assertThat(outcome.getMetadata().getTimestamp()).isNotNull();
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testResourcesTyped(@Autowired ApplicationContext context) {
		IFileStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, project1);

			// attribute reading
			String path = "css/style.css";
			ResourceMetadata metadata = ResourceMetadata.builder().path(path).contentType("css").build();
			ResourceContent content = ResourceContent.builder().data(".table { width: 100%; }".getBytes()).build();
			Resource resource = Resource.builder().metadata(metadata).content(content).build();

			// writing
			FileParams params = FileParams.of(name1);
			Project result = storage.setResource(dir, params, resource);

			// reading
			Resource outcome = storage.getResource(dir, params, path);

			// object setup
			assertThat(project1.getRevision()).isEqualTo(result.getRevision() - 1);
			assertThat(project1.getChanged()).isBefore(result.getChanged());

			// serialization and deserialization
			assertThat(outcome.getMetadata().getPath()).isEqualTo(outcome.getMetadata().getPath());
			assertThat(outcome.getMetadata().getContentType()).isEqualTo(outcome.getMetadata().getContentType());
			assertThat(new String(outcome.getContent().getData()))
					.isEqualTo(new String(resource.getContent().getData()));

			// timestamp assigned
			assertThat(outcome.getMetadata().getTimestamp()).isNotNull();

		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testResourcesListDelete(@Autowired ApplicationContext context) {
		IFileStorage storage = context.getBean(IFileStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, Project.class, project1);

			// attribute reading
			String path = "component/compB.css";
			ResourceMetadata metadata = ResourceMetadata.builder().path(path).contentType("css").build();
			ResourceContent content = ResourceContent.builder().data(".table { width: 100%; }".getBytes()).build();
			Resource resource = Resource.builder().metadata(metadata).content(content).build();
			FileParams params = FileParams.of(name1);
			storage.setResource(dir, Project.class, params, resource);

			path = "component/compA.html";
			metadata = ResourceMetadata.builder().path(path).contentType("html").build();
			content = ResourceContent.builder().data("<html>Here I am!</html>".getBytes()).build();
			resource = Resource.builder().metadata(metadata).content(content).build();
			storage.setResource(dir, Project.class, params, resource);

			path = "component/inner/compC.java";
			metadata = ResourceMetadata.builder().path(path).contentType("java").build();
			content = ResourceContent.builder().data("public class A {}".getBytes()).build();
			resource = Resource.builder().metadata(metadata).content(content).build();
			storage.setResource(dir, Project.class, params, resource);

			List<Resource> resources = storage.listResources(dir, Project.class, params, null, null, null);
			// count resources
			assertThat(resources.size()).isEqualTo(3);

			// sorted by path in the origin
			Resource resource1 = resources.get(0);
			assertThat(resource1.getMetadata().getPath()).isEqualTo("component/compA.html");

			Resource resource2 = resources.get(1);
			assertThat(resource2.getMetadata().getPath()).isEqualTo("component/compB.css");

			Resource resource3 = resources.get(2);
			assertThat(resource3.getMetadata().getPath()).isEqualTo("component/inner/compC.java");

			// delete last resource
			storage.deleteResource(dir, Project.class, params, "component/compB.css");
			resources = storage.listResources(dir, Project.class, params, null, null, null);
			// count resources
			assertThat(resources.size()).isEqualTo(2);

			// sorted by path in the origin
			resource1 = resources.get(0);
			assertThat(resource1.getMetadata().getPath()).isEqualTo("component/compA.html");

			resource2 = resources.get(1);
			assertThat(resource2.getMetadata().getPath()).isEqualTo("component/inner/compC.java");

			// FilePaging only 1
			resources = storage.listResources(dir, Project.class, params, null,
					FilePaging.builder().skip(1).max(1).build(), null);
			// count resources
			assertThat(resources.size()).isEqualTo(1);
			assertThat(storage.countResources(dir, Project.class, params, null,
					FilePaging.builder().skip(1).max(1).build())).isEqualTo(1);
			resource1 = resources.get(0);
			assertThat(resource1.getMetadata().getPath()).isEqualTo("component/inner/compC.java");

			// GitFilterfilter contentType with 'html'
			resources = storage.listResources(dir, Project.class, params,
					FilePredicate.builder()
							.filter(factory.read("{\"metadata.contentType\": {\"$eq\": \"html\"}}".getBytes())).build(),
					null, null);
			// count resources
			assertThat(resources.size()).isEqualTo(1);
			assertThat(storage.countResources(dir, Project.class, params,
					FilePredicate.builder()
							.filter(factory.read("{\"metadata.contentType\": {\"$eq\": \"html\"}}".getBytes())).build(),
					null)).isEqualTo(1);
			resource1 = resources.get(0);
			assertThat(resource1.getMetadata().getPath()).isEqualTo("component/compA.html");
			assertThat(resource1.getMetadata().getContentType()).isEqualTo("html");
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testResourcesListDeleteTyped(@Autowired ApplicationContext context) {
		IFileStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, project1);
			FileParams params = FileParams.of(name1);

			// attribute reading
			String path = "component/compB.css";
			ResourceMetadata metadata = ResourceMetadata.builder().path(path).contentType("css").build();
			ResourceContent content = ResourceContent.builder().data(".table { width: 100%; }".getBytes()).build();
			Resource resource = Resource.builder().metadata(metadata).content(content).build();
			storage.setResource(dir, params, resource);

			path = "component/compA.html";
			metadata = ResourceMetadata.builder().path(path).contentType("html").build();
			content = ResourceContent.builder().data("<html>Here I am!</html>".getBytes()).build();
			resource = Resource.builder().metadata(metadata).content(content).build();
			storage.setResource(dir, params, resource);

			path = "component/inner/compC.java";
			metadata = ResourceMetadata.builder().path(path).contentType("java").build();
			content = ResourceContent.builder().data("public class A {}".getBytes()).build();
			resource = Resource.builder().metadata(metadata).content(content).build();
			storage.setResource(dir, params, resource);

			List<Resource> resources = storage.listResources(dir, params, null, null, null);
			// count resources
			assertThat(resources.size()).isEqualTo(3);

			// sorted by path in the origin
			Resource resource1 = resources.get(0);
			assertThat(resource1.getMetadata().getPath()).isEqualTo("component/compA.html");

			Resource resource2 = resources.get(1);
			assertThat(resource2.getMetadata().getPath()).isEqualTo("component/compB.css");

			Resource resource3 = resources.get(2);
			assertThat(resource3.getMetadata().getPath()).isEqualTo("component/inner/compC.java");

			// delete last resource
			storage.deleteResource(dir, params, "component/compB.css");
			resources = storage.listResources(dir, params, null, null, null);
			// count resources
			assertThat(resources.size()).isEqualTo(2);

			// sorted by path in the origin
			resource1 = resources.get(0);
			assertThat(resource1.getMetadata().getPath()).isEqualTo("component/compA.html");

			resource2 = resources.get(1);
			assertThat(resource2.getMetadata().getPath()).isEqualTo("component/inner/compC.java");

			// FilePaging only 1
			resources = storage.listResources(dir, params, null, FilePaging.builder().skip(1).max(1).build(), null);
			// count resources
			assertThat(resources.size()).isEqualTo(1);
			assertThat(storage.countResources(dir, params, null, FilePaging.builder().skip(1).max(1).build()))
					.isEqualTo(1);
			resource1 = resources.get(0);
			assertThat(resource1.getMetadata().getPath()).isEqualTo("component/inner/compC.java");

			// GitFilterfilter contentType with 'html'
			resources = storage.listResources(dir, params,
					FilePredicate.builder()
							.filter(factory.read("{\"metadata.contentType\": {\"$eq\": \"html\"}}".getBytes())).build(),
					null, null);
			// count resources
			assertThat(resources.size()).isEqualTo(1);
			assertThat(storage.countResources(dir, params,
					FilePredicate.builder()
							.filter(factory.read("{\"metadata.contentType\": {\"$eq\": \"html\"}}".getBytes())).build(),
					null)).isEqualTo(1);
			resource1 = resources.get(0);
			assertThat(resource1.getMetadata().getPath()).isEqualTo("component/compA.html");
			assertThat(resource1.getMetadata().getContentType()).isEqualTo("html");
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testInvalidResources(@Autowired ApplicationContext context) {
		IFileStorage storage = context.getBean(IFileStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, Project.class, project1);
			FileParams params = FileParams.of(name1);

			// attribute reading
			String path = "css/style.css";
			ResourceMetadata metadata = ResourceMetadata.builder().path(path).contentType("css").build();
			ResourceContent content = ResourceContent.builder().data(".table { width: 100%; }".getBytes()).build();
			Resource resource = Resource.builder().metadata(metadata).content(content).build();

			// Success save @resources
			storage.setResource(dir, Project.class, params, resource);

			// sabotage
			final String newPath = "../../css/style.css";
			metadata.setPath(newPath);
			assertThatThrownBy(() -> storage.setResource(dir, Project.class, params, resource))//
					.isExactlyInstanceOf(FileStorageSecurityException.class)//
					.hasMessage("Cannot work with resources from a higher file structure. " + newPath);

			assertThatThrownBy(() -> storage.getResource(dir, Project.class, params, newPath))//
					.isExactlyInstanceOf(FileStorageSecurityException.class)//
					.hasMessage("Cannot work with resources from a higher file structure. " + newPath);

			assertThatThrownBy(() -> storage.deleteResource(dir, Project.class, params, newPath))//
					.isExactlyInstanceOf(FileStorageSecurityException.class)//
					.hasMessage("Cannot work with resources from a higher file structure. " + newPath);
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testInvalidResourcesTyped(@Autowired ApplicationContext context) {
		IFileStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, project1);
			FileParams params = FileParams.of(name1);

			// attribute reading
			String path = "css/style.css";
			ResourceMetadata metadata = ResourceMetadata.builder().path(path).contentType("css").build();
			ResourceContent content = ResourceContent.builder().data(".table { width: 100%; }".getBytes()).build();
			Resource resource = Resource.builder().metadata(metadata).content(content).build();

			// Success save @resources
			storage.setResource(dir, params, resource);

			// sabotage
			final String newPath = "../../css/style.css";
			metadata.setPath(newPath);
			assertThatThrownBy(() -> storage.setResource(dir, params, resource))//
					.isExactlyInstanceOf(FileStorageSecurityException.class)//
					.hasMessage("Cannot work with resources from a higher file structure. " + newPath);

			assertThatThrownBy(() -> storage.getResource(dir, params, newPath))//
					.isExactlyInstanceOf(FileStorageSecurityException.class)//
					.hasMessage("Cannot work with resources from a higher file structure. " + newPath);
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testEmptyResources(@Autowired ApplicationContext context) {
		IFileStorage storage = context.getBean(IFileStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, Project.class, project1);
			FileParams params = FileParams.of(name1);

			assertThat(storage.listResources(dir, Project.class, params, null, null, null)).isEmpty();
			assertThat(storage.countResources(dir, Project.class, params, null, null)).isZero();
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testInvalidAllResourcesTyped(@Autowired ApplicationContext context) {
		IFileStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, project1);

			FileParams params = FileParams.of(name1);
			assertThat(storage.listResources(dir, params, null, null, null)).isEmpty();
			assertThat(storage.countResources(dir, params, null, null)).isZero();
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testLocation(@Autowired ApplicationContext context) {
		IFileStorage storage = context.getBean(IFileStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			FileParams params = FileParams.of(name1);

			File location1 = storage.location(dir, project1);
			File location2 = storage.location(dir, Project.class, project1);
			File location3 = storage.location(dir, Project.class, params);

			assertThat(location1).isEqualTo(location2);
			assertThat(location2).isEqualTo(location3);
			assertThat(location3).isEqualTo(new File(dir, "@projects/" + name1));

			storage.write(dir, project1);
			String path = "css/style.css";
			ResourceMetadata metadata = ResourceMetadata.builder().path(path).contentType("css").build();
			ResourceContent content = ResourceContent.builder().data(".table { width: 100%; }".getBytes()).build();
			Resource resource = Resource.builder().metadata(metadata).content(content).build();
			storage.setResource(dir, Project.class, params, resource);

			File location4 = storage.locationResource(dir, Project.class, params, null);
			File location5 = storage.locationResource(dir, Project.class, params, "css/style.css");

			assertThat(location4).isEqualTo(new File(dir, "@projects/" + name1 + "/@resources"));
			assertThat(location5).isEqualTo(new File(dir, "@projects/" + name1 + "/@resources/css/style.css"));

			String invalidPath = "../css/style.css";
			assertThatThrownBy(() -> storage.locationResource(dir, Project.class, params, invalidPath))//
					.isExactlyInstanceOf(FileStorageException.class)//
					.hasMessage("Cannot read location of resources in a higher file structure. " + invalidPath);
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testLocationTyped(@Autowired ApplicationContext context) {
		IFileStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			FileParams params = FileParams.of(name1);

			File location1 = storage.location(dir, project1);
			File location2 = storage.location(dir, params);

			assertThat(location1).isEqualTo(location2);
			assertThat(location2).isEqualTo(new File(dir, "@projects/" + name1));

			storage.write(dir, project1);
			String path = "css/style.css";
			ResourceMetadata metadata = ResourceMetadata.builder().path(path).contentType("css").build();
			ResourceContent content = ResourceContent.builder().data(".table { width: 100%; }".getBytes()).build();
			Resource resource = Resource.builder().metadata(metadata).content(content).build();
			storage.setResource(dir, params, resource);

			File location4 = storage.locationResource(dir, params, null);
			File location5 = storage.locationResource(dir, params, "css/style.css");

			assertThat(location4).isEqualTo(new File(dir, "@projects/" + name1 + "/@resources"));
			assertThat(location5).isEqualTo(new File(dir, "@projects/" + name1 + "/@resources/css/style.css"));

			String invalidPath = "../css/style.css";
			assertThatThrownBy(() -> storage.locationResource(dir, params, invalidPath))//
					.isExactlyInstanceOf(FileStorageException.class)//
					.hasMessage("Cannot read location of resources in a higher file structure. " + invalidPath);
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}