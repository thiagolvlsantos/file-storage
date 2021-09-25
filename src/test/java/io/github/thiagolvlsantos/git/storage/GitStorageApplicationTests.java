package io.github.thiagolvlsantos.git.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import io.github.thiagolvlsantos.git.commons.file.FileUtils;
import io.github.thiagolvlsantos.git.storage.exceptions.GitStorageException;
import io.github.thiagolvlsantos.git.storage.objects.Outlier;
import io.github.thiagolvlsantos.git.storage.objects.OutlierStorage;
import io.github.thiagolvlsantos.git.storage.objects.Project;
import io.github.thiagolvlsantos.git.storage.objects.ProjectStorage;
import io.github.thiagolvlsantos.git.storage.objects.SubProject;
import io.github.thiagolvlsantos.git.storage.resource.Resource;
import io.github.thiagolvlsantos.git.storage.resource.ResourceContent;
import io.github.thiagolvlsantos.git.storage.resource.ResourceMetadata;

@SpringBootTest
class GitStorageApplicationTests {

	@Test
	void testInvalidEntity(@Autowired ApplicationContext context) {
		IGitStorage storage = context.getBean(IGitStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		try {
			final Outlier instance = new Outlier();
			assertThatThrownBy(() -> storage.write(dir, instance))//
					.isExactlyInstanceOf(GitStorageException.class)//
					.hasMessage("Entity is not annotated with @GitEntity.");
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
		IGitStorageTyped<Outlier> storage = context.getBean(OutlierStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		try {
			final Outlier instance = new Outlier();
			assertThatThrownBy(() -> storage.write(dir, instance))//
					.isExactlyInstanceOf(GitStorageException.class)//
					.hasMessage("Entity is not annotated with @GitEntity.");
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
		IGitStorage storage = context.getBean(IGitStorage.class);
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
					.isExactlyInstanceOf(GitStorageException.class)//
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
		IGitStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
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
					.isExactlyInstanceOf(GitStorageException.class)//
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
			assertThat(storage.all(dir, Project.class)).contains(project1, project2);
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
			assertThat(storage.all(dir)).contains(project1, project2);
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

	@Test
	void testUpdate(@Autowired ApplicationContext context) {
		IGitStorage storage = context.getBean(IGitStorage.class);
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
			project1 = storage.merge(dir, Project.class, newVersion, name1);

			// old attributes
			assertThat(project1.getId()).isEqualTo(id);
			assertThat(project1.getName()).isEqualTo(name1);
			assertThat(project1.getCreated()).isEqualTo(created);
			assertThat(project1.getRevision()).isEqualTo(revision + 1);

			// new attributes
			assertThat(project1.getDescription()).isEqualTo(description);

			// update valid attribute
			description = "Final version";
			project1 = storage.setAttribute(dir, Project.class, "description", description, name1);

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
		IGitStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
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
			project1 = storage.merge(dir, newVersion, name1);

			// old attributes
			assertThat(project1.getId()).isEqualTo(id);
			assertThat(project1.getName()).isEqualTo(name1);
			assertThat(project1.getCreated()).isEqualTo(created);
			assertThat(project1.getRevision()).isEqualTo(revision + 1);

			// new attributes
			assertThat(project1.getDescription()).isEqualTo(description);

			// update valid attribute
			description = "Final version";
			project1 = storage.setAttribute(dir, "description", description, name1);

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
		IGitStorage storage = context.getBean(IGitStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		try {
			final Project instance = new Project();
			assertThatThrownBy(() -> storage.merge(dir, Project.class, instance, "doNotExist"))//
					.isExactlyInstanceOf(GitStorageException.class)//
					.hasMessage("Object '" + Project.class.getSimpleName() + "' with keys '"
							+ Arrays.toString(new String[] { "doNotExist" }) + "' not found.");
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
		IGitStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		try {
			final Project instance = new Project();
			assertThatThrownBy(() -> storage.merge(dir, instance, "doNotExist"))//
					.isExactlyInstanceOf(GitStorageException.class)//
					.hasMessage("Object '" + Project.class.getSimpleName() + "' with keys '"
							+ Arrays.toString(new String[] { "doNotExist" }) + "' not found.");
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
		IGitStorage storage = context.getBean(IGitStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, Project.class, project1);

			assertThatThrownBy(() -> storage.setAttribute(dir, Project.class, "title", "newDescription", name1))//
					.isExactlyInstanceOf(GitStorageException.class)//
					.hasMessage("Attribute '" + "title" + "' not found for type: " + project1.getClass());
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
		IGitStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, project1);

			assertThatThrownBy(() -> storage.setAttribute(dir, "title", "newDescription", name1))//
					.isExactlyInstanceOf(GitStorageException.class)//
					.hasMessage("Attribute '" + "title" + "' not found for type: " + project1.getClass());
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
		IGitStorage storage = context.getBean(IGitStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, Project.class, project1);

			assertThatThrownBy(() -> storage.getAttribute(dir, Project.class, "title", name1))//
					.isExactlyInstanceOf(GitStorageException.class)//
					.hasMessage("Attribute '" + "title" + "' not found for type: " + project1.getClass());
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
		IGitStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, project1);

			assertThatThrownBy(() -> storage.getAttribute(dir, "title", name1))//
					.isExactlyInstanceOf(GitStorageException.class)//
					.hasMessage("Attribute '" + "title" + "' not found for type: " + project1.getClass());
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
		IGitStorage storage = context.getBean(IGitStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, Project.class, project1);

			assertThatThrownBy(() -> storage.getResource(dir, Project.class, "css/example.css", name1))//
					.isExactlyInstanceOf(GitStorageException.class)//
					.hasMessage("Resources for " + Arrays.toString(new String[] { name1 }) + " not found.");
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
		IGitStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, project1);

			assertThatThrownBy(() -> storage.getResource(dir, "css/example.css", name1))//
					.isExactlyInstanceOf(GitStorageException.class)//
					.hasMessage("Resources for " + Arrays.toString(new String[] { name1 }) + " not found.");
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
		IGitStorage storage = context.getBean(IGitStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, Project.class, project1);
			assertThat(project1.getId()).isNotNull();

			// try update invalid attributes
			assertThatThrownBy(() -> storage.setAttribute(dir, Project.class, "id", "\"10\"", name1))//
					.isExactlyInstanceOf(GitStorageException.class)//
					.hasMessage("Update of @GitId annotated attribute 'id' is not allowed.");

			// try update invalid attributes
			assertThatThrownBy(() -> storage.setAttribute(dir, Project.class, "name", "\"newName\"", name1))//
					.isExactlyInstanceOf(GitStorageException.class)//
					.hasMessage("Update of @GitKey annotated attribute 'name' is not allowed.");

			// try update invalid attributes
			assertThatThrownBy(() -> storage.setAttribute(dir, Project.class, "created", "\"10\"", name1))//
					.isExactlyInstanceOf(GitStorageException.class)//
					.hasMessage("Update of @GitCreated annotated attribute 'created' is not allowed.");

			// try update invalid attributes
			assertThatThrownBy(() -> storage.setAttribute(dir, Project.class, "revision", "\"10\"", name1))//
					.isExactlyInstanceOf(GitStorageException.class)//
					.hasMessage("Update of @GitRevision annotated attribute 'revision' is not allowed.");
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
		IGitStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, project1);
			assertThat(project1.getId()).isNotNull();

			// try update invalid attributes
			assertThatThrownBy(() -> storage.setAttribute(dir, "id", "\"10\"", name1))//
					.isExactlyInstanceOf(GitStorageException.class)//
					.hasMessage("Update of @GitId annotated attribute 'id' is not allowed.");

			// try update invalid attributes
			assertThatThrownBy(() -> storage.setAttribute(dir, "name", "\"newName\"", name1))//
					.isExactlyInstanceOf(GitStorageException.class)//
					.hasMessage("Update of @GitKey annotated attribute 'name' is not allowed.");

			// try update invalid attributes
			assertThatThrownBy(() -> storage.setAttribute(dir, "created", "\"10\"", name1))//
					.isExactlyInstanceOf(GitStorageException.class)//
					.hasMessage("Update of @GitCreated annotated attribute 'created' is not allowed.");

			// try update invalid attributes
			assertThatThrownBy(() -> storage.setAttribute(dir, "revision", "\"10\"", name1))//
					.isExactlyInstanceOf(GitStorageException.class)//
					.hasMessage("Update of @GitRevision annotated attribute 'revision' is not allowed.");
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
		IGitStorage storage = context.getBean(IGitStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		try {
			assertThatThrownBy(() -> storage.setAttribute(dir, Project.class, "description", "10", "doNotExist"))//
					.isExactlyInstanceOf(GitStorageException.class)//
					.hasMessage("Object '" + Project.class.getSimpleName() + "' with keys '"
							+ Arrays.toString(new String[] { "doNotExist" }) + "' not found.");
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
		IGitStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		try {
			assertThatThrownBy(() -> storage.setAttribute(dir, "description", "10", "doNotExist"))//
					.isExactlyInstanceOf(GitStorageException.class)//
					.hasMessage("Object '" + Project.class.getSimpleName() + "' with keys '"
							+ Arrays.toString(new String[] { "doNotExist" }) + "' not found.");
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
		IGitStorage storage = context.getBean(IGitStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		try {
			assertThatThrownBy(() -> storage.getAttribute(dir, Project.class, "description", "doNotExist"))//
					.isExactlyInstanceOf(GitStorageException.class)//
					.hasMessage("Object '" + Project.class.getSimpleName() + "' with keys '"
							+ Arrays.toString(new String[] { "doNotExist" }) + "' not found.");
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
		IGitStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		try {
			assertThatThrownBy(() -> storage.getAttribute(dir, "description", "doNotExist"))//
					.isExactlyInstanceOf(GitStorageException.class)//
					.hasMessage("Object '" + Project.class.getSimpleName() + "' with keys '"
							+ Arrays.toString(new String[] { "doNotExist" }) + "' not found.");
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
		IGitStorage storage = context.getBean(IGitStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, Project.class, project1);

			// attribute reading
			Object attribute = storage.getAttribute(dir, Project.class, "name", name1);
			assertThat(attribute).isEqualTo(name1);

			// attribute writing
			Project result = storage.setAttribute(dir, Project.class, "description", "newDescription", name1);
			assertThat(result.getDescription()).isEqualTo("newDescription");
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
		IGitStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, project1);

			// attribute reading
			Object attribute = storage.getAttribute(dir, "name", name1);
			assertThat(attribute).isEqualTo(name1);

			// attribute writing
			Project result = storage.setAttribute(dir, "description", "newDescription", name1);
			assertThat(result.getDescription()).isEqualTo("newDescription");
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
		IGitStorage storage = context.getBean(IGitStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, Project.class, project1);

			// attribute reading
			String path = "css/arquivo.css";
			ResourceMetadata metadata = ResourceMetadata.builder().path(path).contentType("css").build();
			ResourceContent content = ResourceContent.builder().data(".table { width: 100%; }".getBytes()).build();
			Resource resource = Resource.builder().metadata(metadata).content(content).build();

			// writing
			Project result = storage.setResource(dir, Project.class, resource, name1);

			// reading
			Resource outcome = storage.getResource(dir, Project.class, path, name1);

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
		IGitStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, project1);

			// attribute reading
			String path = "css/arquivo.css";
			ResourceMetadata metadata = ResourceMetadata.builder().path(path).contentType("css").build();
			ResourceContent content = ResourceContent.builder().data(".table { width: 100%; }".getBytes()).build();
			Resource resource = Resource.builder().metadata(metadata).content(content).build();

			// writing
			Project result = storage.setResource(dir, resource, name1);

			// reading
			Resource outcome = storage.getResource(dir, path, name1);

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
		IGitStorage storage = context.getBean(IGitStorage.class);
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
			storage.setResource(dir, Project.class, resource, name1);

			path = "component/compA.html";
			metadata = ResourceMetadata.builder().path(path).contentType("html").build();
			content = ResourceContent.builder().data("<html>Here I am!</html>".getBytes()).build();
			resource = Resource.builder().metadata(metadata).content(content).build();
			storage.setResource(dir, Project.class, resource, name1);

			path = "component/inner/compC.java";
			metadata = ResourceMetadata.builder().path(path).contentType("java").build();
			content = ResourceContent.builder().data("public class A {}".getBytes()).build();
			resource = Resource.builder().metadata(metadata).content(content).build();
			storage.setResource(dir, Project.class, resource, name1);

			List<Resource> resources = storage.allResources(dir, Project.class, name1);
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
			storage.delResource(dir, Project.class, "component/compB.css", name1);
			resources = storage.allResources(dir, Project.class, name1);
			// count resources
			assertThat(resources.size()).isEqualTo(2);

			// sorted by path in the origin
			resource1 = resources.get(0);
			assertThat(resource1.getMetadata().getPath()).isEqualTo("component/compA.html");

			resource2 = resources.get(1);
			assertThat(resource2.getMetadata().getPath()).isEqualTo("component/inner/compC.java");
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
		IGitStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, project1);

			// attribute reading
			String path = "component/compB.css";
			ResourceMetadata metadata = ResourceMetadata.builder().path(path).contentType("css").build();
			ResourceContent content = ResourceContent.builder().data(".table { width: 100%; }".getBytes()).build();
			Resource resource = Resource.builder().metadata(metadata).content(content).build();
			storage.setResource(dir, resource, name1);

			path = "component/compA.html";
			metadata = ResourceMetadata.builder().path(path).contentType("html").build();
			content = ResourceContent.builder().data("<html>Here I am!</html>".getBytes()).build();
			resource = Resource.builder().metadata(metadata).content(content).build();
			storage.setResource(dir, resource, name1);

			path = "component/inner/compC.java";
			metadata = ResourceMetadata.builder().path(path).contentType("java").build();
			content = ResourceContent.builder().data("public class A {}".getBytes()).build();
			resource = Resource.builder().metadata(metadata).content(content).build();
			storage.setResource(dir, resource, name1);

			List<Resource> resources = storage.allResources(dir, name1);
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
			storage.delResource(dir, "component/compB.css", name1);
			resources = storage.allResources(dir, name1);
			// count resources
			assertThat(resources.size()).isEqualTo(2);

			// sorted by path in the origin
			resource1 = resources.get(0);
			assertThat(resource1.getMetadata().getPath()).isEqualTo("component/compA.html");

			resource2 = resources.get(1);
			assertThat(resource2.getMetadata().getPath()).isEqualTo("component/inner/compC.java");
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
		IGitStorage storage = context.getBean(IGitStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, Project.class, project1);

			// attribute reading
			String path = "css/arquivo.css";
			ResourceMetadata metadata = ResourceMetadata.builder().path(path).contentType("css").build();
			ResourceContent content = ResourceContent.builder().data(".table { width: 100%; }".getBytes()).build();
			Resource resource = Resource.builder().metadata(metadata).content(content).build();

			// Success save @resources
			storage.setResource(dir, Project.class, resource, name1);

			// sabotage
			final String newPath = "../../css/arquivo.css";
			metadata.setPath(newPath);
			assertThatThrownBy(() -> storage.setResource(dir, Project.class, resource, name1))//
					.isExactlyInstanceOf(GitStorageException.class)//
					.hasMessage("Cannot save resources in a higher file structure. " + newPath);

			assertThatThrownBy(() -> storage.getResource(dir, Project.class, newPath, name1))//
					.isExactlyInstanceOf(GitStorageException.class)//
					.hasMessage("Cannot read resources from a higher file structure. " + newPath);
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
		IGitStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, project1);

			// attribute reading
			String path = "css/arquivo.css";
			ResourceMetadata metadata = ResourceMetadata.builder().path(path).contentType("css").build();
			ResourceContent content = ResourceContent.builder().data(".table { width: 100%; }".getBytes()).build();
			Resource resource = Resource.builder().metadata(metadata).content(content).build();

			// Success save @resources
			storage.setResource(dir, resource, name1);

			// sabotage
			final String newPath = "../../css/arquivo.css";
			metadata.setPath(newPath);
			assertThatThrownBy(() -> storage.setResource(dir, resource, name1))//
					.isExactlyInstanceOf(GitStorageException.class)//
					.hasMessage("Cannot save resources in a higher file structure. " + newPath);

			assertThatThrownBy(() -> storage.getResource(dir, newPath, name1))//
					.isExactlyInstanceOf(GitStorageException.class)//
					.hasMessage("Cannot read resources from a higher file structure. " + newPath);
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testInvalidAllResources(@Autowired ApplicationContext context) {
		IGitStorage storage = context.getBean(IGitStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, Project.class, project1);

			assertThatThrownBy(() -> storage.allResources(dir, Project.class, name1))//
					.isExactlyInstanceOf(GitStorageException.class)//
					.hasMessage("Resources for " + Arrays.toString(new String[] { name1 }) + " not found.");
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
		IGitStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, project1);

			assertThatThrownBy(() -> storage.allResources(dir, name1))//
					.isExactlyInstanceOf(GitStorageException.class)//
					.hasMessage("Resources for " + Arrays.toString(new String[] { name1 }) + " not found.");
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}