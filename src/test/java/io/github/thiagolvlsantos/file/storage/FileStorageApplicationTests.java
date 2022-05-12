package io.github.thiagolvlsantos.file.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.thiagolvlsantos.file.storage.audit.IFileAudit;
import io.github.thiagolvlsantos.file.storage.entity.FileRepo;
import io.github.thiagolvlsantos.file.storage.exceptions.FileStorageException;
import io.github.thiagolvlsantos.file.storage.exceptions.FileStorageNotFoundException;
import io.github.thiagolvlsantos.file.storage.exceptions.FileStoragePropertyNotFoundException;
import io.github.thiagolvlsantos.file.storage.exceptions.FileStorageResourceNotFoundException;
import io.github.thiagolvlsantos.file.storage.exceptions.FileStorageSecurityException;
import io.github.thiagolvlsantos.file.storage.objects.InvalidRevision;
import io.github.thiagolvlsantos.file.storage.objects.ObjectMeta;
import io.github.thiagolvlsantos.file.storage.objects.ObjectOther;
import io.github.thiagolvlsantos.file.storage.objects.ObjectWrapped;
import io.github.thiagolvlsantos.file.storage.objects.Outlier;
import io.github.thiagolvlsantos.file.storage.objects.OutlierStorage;
import io.github.thiagolvlsantos.file.storage.objects.Project;
import io.github.thiagolvlsantos.file.storage.objects.ProjectStorage;
import io.github.thiagolvlsantos.file.storage.objects.SubProject;
import io.github.thiagolvlsantos.file.storage.objects.Target;
import io.github.thiagolvlsantos.file.storage.objects.TargetAlias;
import io.github.thiagolvlsantos.file.storage.objects.Template;
import io.github.thiagolvlsantos.file.storage.objects.TemplateAlias;
import io.github.thiagolvlsantos.file.storage.objects.TemplateAuthorization;
import io.github.thiagolvlsantos.file.storage.objects.TemplateTargetAuthorization;
import io.github.thiagolvlsantos.file.storage.resource.Resource;
import io.github.thiagolvlsantos.file.storage.resource.ResourceContent;
import io.github.thiagolvlsantos.file.storage.resource.ResourceMetadata;
import io.github.thiagolvlsantos.file.storage.search.FileFilter;
import io.github.thiagolvlsantos.file.storage.search.FilePaging;
import io.github.thiagolvlsantos.file.storage.search.FileSorting;
import io.github.thiagolvlsantos.git.commons.file.FileUtils;
import io.github.thiagolvlsantos.json.predicate.IPredicateFactory;
import io.github.thiagolvlsantos.json.predicate.impl.PredicateFactoryJson;

@SpringBootTest
@Configuration
class FileStorageApplicationTests {

	private IPredicateFactory factory = new PredicateFactoryJson();

	@Bean
	public IFileAudit audit() {
		return new IFileAudit() {
			private AuthorInfo shared = new AuthorInfo("thiagolvlsantos", "email@email.com");

			@Override
			public AuthorInfo author() {
				return shared;
			}
		};
	}

	@Test
	void testTree(@Autowired ApplicationContext context) {
		IFileStorage storage = context.getBean(IFileStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		try {
			String nameApp = "k8s-app";
			String nameJob = "k8s-job";

			Template k8sApp = Template.builder().name(nameApp).build();
			storage.write(dir, k8sApp);

			TemplateAuthorization tAppAuth = TemplateAuthorization.builder()
					.template(TemplateAlias.builder().name(nameApp).build()).build();

			storage.write(dir, tAppAuth);

			Template k8sJob = Template.builder().name(nameJob).build();
			storage.write(dir, k8sJob);

			TemplateAuthorization tJobAuth = TemplateAuthorization.builder()
					.template(TemplateAlias.builder().name(nameJob).build()).build();

			storage.write(dir, tJobAuth);

			Target dev = Target.builder().name("dev").build();
			storage.write(dir, dev);

			Target pro = Target.builder().name("pro").build();
			storage.write(dir, pro);

			TemplateTargetAuthorization taDev = TemplateTargetAuthorization.builder()
					.template(TemplateAlias.builder().name(nameApp).build())
					.target(TargetAlias.builder().name("dev").build()).build();

			storage.write(dir, taDev);

			TemplateTargetAuthorization taPro = TemplateTargetAuthorization.builder()
					.template(TemplateAlias.builder().name(nameApp).build())
					.target(TargetAlias.builder().name("pro").build()).build();

			storage.write(dir, taPro);

			TemplateTargetAuthorization taDevJob = TemplateTargetAuthorization.builder()
					.template(TemplateAlias.builder().name(nameJob).build())
					.target(TargetAlias.builder().name("dev").build()).build();

			storage.write(dir, taDevJob);

			List<Target> allTargets = storage.list(dir, Target.class, null);
			assertThat(allTargets).hasSize(2);

			List<Template> allTemplates = storage.list(dir, Template.class, null);
			assertThat(allTemplates).hasSize(2);

			List<TemplateAuthorization> allAuthorizations = storage.list(dir, TemplateAuthorization.class, null);
			assertThat(allAuthorizations).hasSize(2);

			storage.delete(dir, Template.class, KeyParams.of(nameApp));
			storage.delete(dir, Template.class, KeyParams.of(nameJob));
			allAuthorizations = storage.list(dir, TemplateAuthorization.class, null);
			assertThat(allAuthorizations).hasSize(2);

			List<TemplateTargetAuthorization> allTargetAuthorizations = storage.list(dir,
					TemplateTargetAuthorization.class, null);
			assertThat(allTargetAuthorizations).hasSize(3);

			FileFilter predicate = new FileFilter(
					factory.read(("{\"template.name\":{\"$eq\": \"" + nameJob + "\"}}").getBytes()));
			List<TemplateTargetAuthorization> filterAuthorizations = storage.list(dir,
					TemplateTargetAuthorization.class, SearchParams.builder().filter(predicate).build());
			assertThat(filterAuthorizations).hasSize(1);
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testWrapped(@Autowired ApplicationContext context) {
		IFileStorage storage = context.getBean(IFileStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		try {
			ObjectWrapped instance = ObjectWrapped.builder().name("myName").build();
			storage.write(dir, instance);
			IFileSerializer ser = storage.getSerializer();
			File target = new File(storage.location(dir, instance), ser.getFile(ObjectWrapped.class));
			assertTrue(target.exists());
			instance = storage.read(dir, ObjectWrapped.class, KeyParams.of("myName"));
			assertEquals("myName", instance.getName());
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testSavingWithNameMeta(@Autowired ApplicationContext context) {
		IFileStorage storage = context.getBean(IFileStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		try {
			final ObjectMeta instance = ObjectMeta.builder().name("myName").build();
			storage.write(dir, instance);
			File target = storage.location(dir, instance);
			assertTrue(target.exists());
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testSavingWithNameOther(@Autowired ApplicationContext context) {
		IFileStorage storage = context.getBean(IFileStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		try {
			final ObjectOther instance = ObjectOther.builder().name("myName").build();
			storage.write(dir, instance);
			File target = storage.location(dir, instance);
			assertTrue(target.exists());
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testInvalidEntityRevision(@Autowired ApplicationContext context) {
		IFileStorage storage = context.getBean(IFileStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		try {
			final InvalidRevision instance = new InvalidRevision();
			assertThatThrownBy(() -> storage.write(dir, instance))//
					.isExactlyInstanceOf(FileStorageException.class)//
					.hasMessage("@FileRevision.revision type must be a subclass of Number.class.");
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testInvalidEntity(@Autowired ApplicationContext context) {
		IFileStorage storage = context.getBean(IFileStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		try {
			final Outlier instance = new Outlier();
			assertThatThrownBy(() -> storage.write(dir, instance))//
					.isExactlyInstanceOf(FileStorageException.class)//
					.hasMessage("Entity '" + Outlier.class.getName() + "' is not annotated with @"
							+ FileRepo.class.getSimpleName() + ".");
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
					.hasMessage("Entity '" + Outlier.class.getName() + "' is not annotated with @"
							+ FileRepo.class.getSimpleName() + ".");
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
			assertThat(storage.list(dir, Project.class,
					SearchParams.builder().sorting(FileSorting.builder().property("name").build()).build()))
					.contains(project1, project2);

			// list filtered/sorted
			FileFilter predicate = new FileFilter(factory.read("{\"name\":{\"$eq\": \"projectA\"}}".getBytes()));
			assertThat(storage.list(dir, Project.class,
					SearchParams.builder().filter(predicate).paging(FilePaging.builder().skip(0).build()).build()))
					.contains(project1);

			assertThat(storage.list(dir, Project.class,
					SearchParams.builder().paging(FilePaging.builder().skip(0).build())
							.sorting(FileSorting.builder().property("name").sort(FileSorting.SORT_DESCENDING)
									.nullsFirst(true).secondary(null).build())
							.build()))
					.containsExactly(project2, project1);

			assertThat(storage.list(dir, Project.class, SearchParams.builder()
					.paging(FilePaging.builder().skip(0).build())
					.sorting(FileSorting.builder().property("name").sort(FileSorting.SORT_DESCENDING).nullsFirst(true)
							.secondary(Arrays.asList(
									FileSorting.builder().property("id").sort(FileSorting.SORT_DESCENDING).build()))
							.build())
					.build())).containsExactly(project2, project1);

			assertThat(storage.list(dir, Project.class, SearchParams.builder()
					.paging(FilePaging.builder().skip(0).build())
					.sorting(FileSorting.builder().property("parent.name").sort(FileSorting.SORT_DESCENDING)
							.secondary(Arrays.asList(
									FileSorting.builder().property("parent.name").sort(FileSorting.SORT_DESCENDING)
											.build(),
									FileSorting.builder().property("name").sort(FileSorting.SORT_ASCENDING).build(),
									FileSorting.builder().property(null).sort(null).build()))
							.build())
					.build())).containsExactly(project1, project2);

			// count all
			assertThat(storage.count(dir, Project.class, null)).isEqualTo(2L);

			// exists by key
			assertThat(storage.exists(dir, Project.class, KeyParams.of(project1.getName()))).isTrue();
			// exists by example
			assertThat(storage.exists(dir, Project.class, project1)).isTrue();

			// read by key
			project1 = storage.read(dir, Project.class, KeyParams.of(project1.getName()));
			assertThat(project1.getName()).isEqualTo("projectA");

			// read by example
			project1 = storage.read(dir, Project.class, project1);
			assertThat(project1.getName()).isEqualTo("projectA");

			// search by name
			List<Project> list = storage.list(dir, Project.class, SearchParams.builder()
					.filter(new FileFilter(factory.read("{\"name\":{\"$eq\": \"projectB\"}}".getBytes()))).build());
			assertThat(list).hasSize(1);
			assertThat(list.get(0).getName()).isEqualTo("projectB");

			// delete by key
			storage.delete(dir, Project.class, KeyParams.of(project1.getName()));
			assertThat(storage.exists(dir, Project.class, KeyParams.of(project1.getName()))).isFalse();

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
			assertThat(storage.list(dir,
					SearchParams.builder().paging(FilePaging.builder().skip(0).build())
							.sorting(FileSorting.builder().property("name").nullsFirst(true).build()).build()))
					.contains(project1, project2);

			// list filtered/sorted
			FileFilter predicate = new FileFilter(factory.read("{\"name\":{\"$eq\": \"projectA\"}}".getBytes()));
			assertThat(storage.list(dir,
					SearchParams.builder().filter(predicate).paging(FilePaging.builder().skip(0).build()).build()))
					.contains(project1);

			assertThat(storage.list(dir,
					SearchParams.builder().paging(FilePaging.builder().skip(0).build())
							.sorting(FileSorting.builder().property("name").sort(FileSorting.SORT_DESCENDING)
									.nullsFirst(true).secondary(null).build())
							.build()))
					.containsExactly(project2, project1);

			assertThat(storage.list(dir, SearchParams.builder().paging(FilePaging.builder().skip(0).build())
					.sorting(FileSorting.builder().property("name").sort(FileSorting.SORT_DESCENDING).nullsFirst(true)
							.secondary(Arrays.asList(
									FileSorting.builder().property("id").sort(FileSorting.SORT_DESCENDING).build()))
							.build())
					.build())).containsExactly(project2, project1);

			assertThat(storage.list(dir, SearchParams.builder().paging(FilePaging.builder().skip(0).build())
					.sorting(FileSorting.builder().property("parent.name").sort(FileSorting.SORT_DESCENDING)
							.secondary(Arrays.asList(
									FileSorting.builder().property("parent.name").sort(FileSorting.SORT_DESCENDING)
											.build(),
									FileSorting.builder().property("name").sort(FileSorting.SORT_ASCENDING).build(),
									FileSorting.builder().property(null).sort(null).build()))
							.build())
					.build())).containsExactly(project1, project2);

			// count all, skip 1
			assertThat(storage.count(dir,
					SearchParams.builder().paging(FilePaging.builder().skip(1).max(1).build()).build())).isEqualTo(1L);

			// exists by key
			assertThat(storage.exists(dir, KeyParams.of(project1.getName()))).isTrue();
			// exists by example
			assertThat(storage.exists(dir, project1)).isTrue();

			// read by key
			project1 = storage.read(dir, KeyParams.of(project1.getName()));
			assertThat(project1.getName()).isEqualTo("projectA");

			// read by example
			project1 = storage.read(dir, project1);
			assertThat(project1.getName()).isEqualTo("projectA");

			// search by name
			List<Project> list = storage.list(dir, SearchParams.builder()
					.filter(new FileFilter(factory.read("{\"name\":{\"$eq\": \"projectB\"}}".getBytes()))).build());
			assertThat(list).hasSize(1);
			assertThat(list.get(0).getName()).isEqualTo("projectB");

			// delete by key
			storage.delete(dir, KeyParams.of(project1.getName()));
			assertThat(storage.exists(dir, KeyParams.of(project1.getName()))).isFalse();

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

			Project newVersion = Project.builder().name(name1).revision(revision).build();
			String description = "This is a new description.";
			newVersion.setDescription(description);
			project1 = storage.write(dir, Project.class, newVersion);

			// old properties
			assertThat(project1.getId()).isEqualTo(id);
			assertThat(project1.getName()).isEqualTo(name1);
			assertThat(project1.getCreated()).isEqualTo(created);
			assertThat(project1.getRevision()).isEqualTo(revision + 1);

			// new properties
			assertThat(project1.getDescription()).isEqualTo(description);

			// update valid property
			description = "Final version";
			project1 = storage.setProperty(dir, Project.class, KeyParams.of(name1), "description", description);

			// changed property
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

			Project newVersion = Project.builder().name(name1).revision(revision).build();
			String description = "This is a new description.";
			newVersion.setDescription(description);
			project1 = storage.write(dir, newVersion);

			// old properties
			assertThat(project1.getId()).isEqualTo(id);
			assertThat(project1.getName()).isEqualTo(name1);
			assertThat(project1.getCreated()).isEqualTo(created);
			assertThat(project1.getRevision()).isEqualTo(revision + 1);

			// new properties
			assertThat(project1.getDescription()).isEqualTo(description);

			// update valid property
			description = "Final version";
			project1 = storage.setProperty(dir, KeyParams.of(name1), "description", description);

			// changed property
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
	void testInvalidSetPropertyName(@Autowired ApplicationContext context) {
		IFileStorage storage = context.getBean(IFileStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, Project.class, project1);

			KeyParams params = KeyParams.of(name1);
			assertThatThrownBy(() -> {
				storage.setProperty(dir, Project.class, params, "title", "newDescription");
			}).isExactlyInstanceOf(FileStoragePropertyNotFoundException.class)//
					.hasMessage(new FileStoragePropertyNotFoundException("title", project1, null).getMessage());
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testInvalidSetPropertyNameTyped(@Autowired ApplicationContext context) {
		IFileStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, project1);

			KeyParams params = KeyParams.of(name1);
			assertThatThrownBy(() -> {
				storage.setProperty(dir, params, "title", "newDescription");
			}).isExactlyInstanceOf(FileStoragePropertyNotFoundException.class)//
					.hasMessage(new FileStoragePropertyNotFoundException("title", project1, null).getMessage());
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testInvalidGetPropertyName(@Autowired ApplicationContext context) {
		IFileStorage storage = context.getBean(IFileStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, Project.class, project1);

			KeyParams params = KeyParams.of(name1);
			assertThatThrownBy(() -> {
				storage.getProperty(dir, Project.class, params, "title");
			}).isExactlyInstanceOf(FileStoragePropertyNotFoundException.class)//
					.hasMessage(new FileStoragePropertyNotFoundException("title", project1, null).getMessage());
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testInvalidGetPropertyNameTyped(@Autowired ApplicationContext context) {
		IFileStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, project1);

			KeyParams params = KeyParams.of(name1);
			assertThatThrownBy(() -> {
				storage.getProperty(dir, params, "title");
			}).isExactlyInstanceOf(FileStoragePropertyNotFoundException.class)//
					.hasMessage(new FileStoragePropertyNotFoundException("title", project1, null).getMessage());
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testInvalidAllPropertyName(@Autowired ApplicationContext context) {
		IFileStorage storage = context.getBean(IFileStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, Project.class, project1);

			KeyParams params = KeyParams.of(name1);
			KeyParams names = KeyParams.of("title");
			assertThatThrownBy(() -> {
				storage.properties(dir, Project.class, params, names);
			}).isExactlyInstanceOf(FileStoragePropertyNotFoundException.class)//
					.hasMessage(new FileStoragePropertyNotFoundException("title", project1, null).getMessage());
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testInvalidAllPropertyNameTyped(@Autowired ApplicationContext context) {
		IFileStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, project1);

			KeyParams params = KeyParams.of(name1);
			KeyParams names = KeyParams.of("title");
			assertThatThrownBy(() -> {
				storage.properties(dir, params, names);
			}).isExactlyInstanceOf(FileStoragePropertyNotFoundException.class)//
					.hasMessage(new FileStoragePropertyNotFoundException("title", project1, null).getMessage());
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

			KeyParams params = KeyParams.of(name1);
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

			KeyParams params = KeyParams.of(name1);
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

			// try update invalid properties
			KeyParams params = KeyParams.of(name1);
			assertThatThrownBy(() -> {
				storage.setProperty(dir, Project.class, params, "id", "\"10\"");
			}).isExactlyInstanceOf(FileStorageException.class)//
					.hasMessage("Update of @FileId annotated property 'id' is not allowed.");

			// try update invalid properties
			assertThatThrownBy(() -> storage.setProperty(dir, Project.class, params, "name", "\"newName\""))//
					.isExactlyInstanceOf(FileStorageException.class)//
					.hasMessage("Update of @FileKey annotated property 'name' is not allowed.");

			// try update invalid properties
			assertThatThrownBy(() -> storage.setProperty(dir, Project.class, params, "created", "\"10\""))//
					.isExactlyInstanceOf(FileStorageException.class)//
					.hasMessage("Update of @FileCreated annotated property 'created' is not allowed.");

			// try update invalid properties
			assertThatThrownBy(() -> storage.setProperty(dir, Project.class, params, "revision", "\"10\""))//
					.isExactlyInstanceOf(FileStorageException.class)//
					.hasMessage("Update of @FileRevision annotated property 'revision' is not allowed.");
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

			// try update invalid properties
			KeyParams params = KeyParams.of(name1);
			assertThatThrownBy(() -> {
				storage.setProperty(dir, params, "id", "\"10\"");
			}).isExactlyInstanceOf(FileStorageException.class)//
					.hasMessage("Update of @FileId annotated property 'id' is not allowed.");

			// try update invalid properties
			assertThatThrownBy(() -> storage.setProperty(dir, params, "name", "\"newName\""))//
					.isExactlyInstanceOf(FileStorageException.class)//
					.hasMessage("Update of @FileKey annotated property 'name' is not allowed.");

			// try update invalid properties
			assertThatThrownBy(() -> storage.setProperty(dir, params, "created", "\"10\""))//
					.isExactlyInstanceOf(FileStorageException.class)//
					.hasMessage("Update of @FileCreated annotated property 'created' is not allowed.");

			// try update invalid properties
			assertThatThrownBy(() -> storage.setProperty(dir, params, "revision", "\"10\""))//
					.isExactlyInstanceOf(FileStorageException.class)//
					.hasMessage("Update of @FileRevision annotated property 'revision' is not allowed.");
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
			KeyParams params = KeyParams.of("doNotExist");
			assertThatThrownBy(() -> {
				storage.setProperty(dir, Project.class, params, "description", "10");
			}).isExactlyInstanceOf(FileStorageNotFoundException.class)//
					.hasMessage("Object '" + Project.class.getSimpleName() + "' with keys '"
							+ KeyParams.of("doNotExist") + "' not found.");
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
			KeyParams params = KeyParams.of("doNotExist");
			assertThatThrownBy(() -> {
				storage.setProperty(dir, params, "description", "10");
			}).isExactlyInstanceOf(FileStorageNotFoundException.class)//
					.hasMessage("Object '" + Project.class.getSimpleName() + "' with keys '"
							+ KeyParams.of("doNotExist") + "' not found.");
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
			KeyParams params = KeyParams.of("doNotExist");
			assertThatThrownBy(() -> {
				storage.getProperty(dir, Project.class, params, "description");
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
			KeyParams params = KeyParams.of("doNotExist");
			assertThatThrownBy(() -> {
				storage.getProperty(dir, params, "description");
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
	void testPropertys(@Autowired ApplicationContext context) {
		IFileStorage storage = context.getBean(IFileStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, Project.class, project1);

			// property reading
			KeyParams params = KeyParams.of(name1);
			Object property = storage.getProperty(dir, Project.class, params, "name");
			assertThat(property).isEqualTo(name1);

			// property writing
			Project result = storage.setProperty(dir, Project.class, params, "description", "newDescription");
			assertThat(result.getDescription()).isEqualTo("newDescription");

			// property full map
			Map<String, Object> objs = storage.properties(dir, Project.class, params, (KeyParams) null);
			assertThat(objs).containsEntry("description", "newDescription");

			// property map projection
			KeyParams names = KeyParams.of(Arrays.asList("name", "created"));
			objs = storage.properties(dir, Project.class, params, names);
			assertThat(objs).hasSize(2);

			// invalid property
			KeyParams name = KeyParams.of("title");
			assertThatThrownBy(() -> {
				storage.properties(dir, Project.class, params, name);
			}).isExactlyInstanceOf(FileStoragePropertyNotFoundException.class)//
					.hasMessage(new FileStoragePropertyNotFoundException("title", project1, null).getMessage());
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testPropertysTyped(@Autowired ApplicationContext context) {
		IFileStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		String name1 = "projectA";
		try {
			// write
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, project1);

			// property reading
			KeyParams params = KeyParams.of(name1);
			Object property = storage.getProperty(dir, params, "name");
			assertThat(property).isEqualTo(name1);

			// property writing
			Project result = storage.setProperty(dir, params, "description", "newDescription");
			assertThat(result.getDescription()).isEqualTo("newDescription");

			// property full map
			Map<String, Object> objs = storage.properties(dir, params, (KeyParams) null);
			assertThat(objs).containsEntry("description", "newDescription");

			// property map projection
			KeyParams names = KeyParams.of(Arrays.asList("name", "created"));
			objs = storage.properties(dir, params, names);
			assertThat(objs).hasSize(2);

			// invalid property
			KeyParams name = KeyParams.of("title");
			assertThatThrownBy(() -> {
				storage.properties(dir, params, name);
			}).isExactlyInstanceOf(FileStoragePropertyNotFoundException.class)//
					.hasMessage(new FileStoragePropertyNotFoundException("title", project1, null).getMessage());
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

			// property reading
			String path = "css/style.css";
			ResourceMetadata metadata = ResourceMetadata.builder().path(path).contentType("css").build();
			ResourceContent content = ResourceContent.builder().data(".table { width: 100%; }".getBytes()).build();
			Resource resource = Resource.builder().metadata(metadata).content(content).build();

			// writing
			KeyParams params = KeyParams.of(name1);
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

			// property reading
			String path = "css/style.css";
			ResourceMetadata metadata = ResourceMetadata.builder().path(path).contentType("css").build();
			ResourceContent content = ResourceContent.builder().data(".table { width: 100%; }".getBytes()).build();
			Resource resource = Resource.builder().metadata(metadata).content(content).build();

			// writing
			KeyParams params = KeyParams.of(name1);
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

			// property reading
			String path = "component/compB.css";
			ResourceMetadata metadata = ResourceMetadata.builder().path(path).contentType("css").build();
			ResourceContent content = ResourceContent.builder().data(".table { width: 100%; }".getBytes()).build();
			Resource resource = Resource.builder().metadata(metadata).content(content).build();
			KeyParams params = KeyParams.of(name1);
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

			List<Resource> resources = storage.listResources(dir, Project.class, params, null);
			// count resources
			assertThat(resources).hasSize(3);

			// sorted by path in the origin
			Resource resource1 = resources.get(0);
			assertThat(resource1.getMetadata().getPath()).isEqualTo("component/compA.html");

			Resource resource2 = resources.get(1);
			assertThat(resource2.getMetadata().getPath()).isEqualTo("component/compB.css");

			Resource resource3 = resources.get(2);
			assertThat(resource3.getMetadata().getPath()).isEqualTo("component/inner/compC.java");

			// delete last resource
			storage.deleteResource(dir, Project.class, params, "component/compB.css");
			resources = storage.listResources(dir, Project.class, params, null);
			// count resources
			assertThat(resources).hasSize(2);

			// sorted by path in the origin
			resource1 = resources.get(0);
			assertThat(resource1.getMetadata().getPath()).isEqualTo("component/compA.html");

			resource2 = resources.get(1);
			assertThat(resource2.getMetadata().getPath()).isEqualTo("component/inner/compC.java");

			// FilePaging only 1
			resources = storage.listResources(dir, Project.class, params,
					SearchParams.builder().paging(FilePaging.builder().skip(1).max(1).build()).build());
			// count resources
			assertThat(resources).hasSize(1);
			assertThat(storage.countResources(dir, Project.class, params,
					SearchParams.builder().paging(FilePaging.builder().skip(1).max(1).build()).build())).isEqualTo(1);
			resource1 = resources.get(0);
			assertThat(resource1.getMetadata().getPath()).isEqualTo("component/inner/compC.java");

			// GitFilterfilter contentType with 'html'
			resources = storage.listResources(dir, Project.class, params,
					SearchParams.builder().filter(FileFilter.builder()
							.filter(factory.read("{\"metadata.contentType\": {\"$eq\": \"html\"}}".getBytes())).build())
							.build());
			// count resources
			assertThat(resources).hasSize(1);
			assertThat(storage.countResources(dir, Project.class, params,
					SearchParams.builder().filter(FileFilter.builder()
							.filter(factory.read("{\"metadata.contentType\": {\"$eq\": \"html\"}}".getBytes())).build())
							.build()))
					.isEqualTo(1);
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
			KeyParams params = KeyParams.of(name1);

			// property reading
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

			List<Resource> resources = storage.listResources(dir, params, null);
			// count resources
			assertThat(resources).hasSize(3);

			// sorted by path in the origin
			Resource resource1 = resources.get(0);
			assertThat(resource1.getMetadata().getPath()).isEqualTo("component/compA.html");

			Resource resource2 = resources.get(1);
			assertThat(resource2.getMetadata().getPath()).isEqualTo("component/compB.css");

			Resource resource3 = resources.get(2);
			assertThat(resource3.getMetadata().getPath()).isEqualTo("component/inner/compC.java");

			// delete last resource
			storage.deleteResource(dir, params, "component/compB.css");
			resources = storage.listResources(dir, params, null);
			// count resources
			assertThat(resources).hasSize(2);

			// sorted by path in the origin
			resource1 = resources.get(0);
			assertThat(resource1.getMetadata().getPath()).isEqualTo("component/compA.html");

			resource2 = resources.get(1);
			assertThat(resource2.getMetadata().getPath()).isEqualTo("component/inner/compC.java");

			// FilePaging only 1
			resources = storage.listResources(dir, params,
					SearchParams.builder().paging(FilePaging.builder().skip(1).max(1).build()).build());
			// count resources
			assertThat(resources).hasSize(1);
			assertThat(storage.countResources(dir, params,
					SearchParams.builder().paging(FilePaging.builder().skip(1).max(1).build()).build())).isEqualTo(1);
			resource1 = resources.get(0);
			assertThat(resource1.getMetadata().getPath()).isEqualTo("component/inner/compC.java");

			// GitFilterfilter contentType with 'html'
			resources = storage.listResources(dir, params,
					SearchParams.builder().filter(FileFilter.builder()
							.filter(factory.read("{\"metadata.contentType\": {\"$eq\": \"html\"}}".getBytes())).build())
							.build());
			// count resources
			assertThat(resources).hasSize(1);
			assertThat(storage.countResources(dir, params,
					SearchParams.builder().filter(FileFilter.builder()
							.filter(factory.read("{\"metadata.contentType\": {\"$eq\": \"html\"}}".getBytes())).build())
							.build()))
					.isEqualTo(1);
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
			KeyParams params = KeyParams.of(name1);

			// property reading
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
			KeyParams params = KeyParams.of(name1);

			// property reading
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
			KeyParams params = KeyParams.of(name1);

			assertThat(storage.listResources(dir, Project.class, params, null)).isEmpty();
			assertThat(storage.countResources(dir, Project.class, params, null)).isZero();
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

			KeyParams params = KeyParams.of(name1);
			assertThat(storage.listResources(dir, params, null)).isEmpty();
			assertThat(storage.countResources(dir, params, null)).isZero();
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
			KeyParams params = KeyParams.of(name1);

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

			assertThat(storage.existsResource(dir, Project.class, params, path)).isTrue();
			assertThat(location4).isEqualTo(new File(dir, "@projects/" + name1 + "/data@resources"));
			assertThat(location5).isEqualTo(new File(dir, "@projects/" + name1 + "/data@resources/css/style.css"));

			String invalidPath = "../css/style.css";
			assertThatThrownBy(() -> storage.locationResource(dir, Project.class, params, invalidPath))//
					.isExactlyInstanceOf(FileStorageSecurityException.class)//
					.hasMessage("Cannot work with resources from a higher file structure. " + invalidPath);
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
			KeyParams params = KeyParams.of(name1);

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

			assertThat(storage.existsResource(dir, params, path)).isTrue();
			assertThat(location4).isEqualTo(new File(dir, "@projects/" + name1 + "/data@resources"));
			assertThat(location5).isEqualTo(new File(dir, "@projects/" + name1 + "/data@resources/css/style.css"));

			String invalidPath = "../css/style.css";
			assertThatThrownBy(() -> storage.locationResource(dir, params, invalidPath))//
					.isExactlyInstanceOf(FileStorageSecurityException.class)//
					.hasMessage("Cannot work with resources from a higher file structure. " + invalidPath);
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testResilience(@Autowired ApplicationContext context) throws IOException {
		IFileStorage storage = context.getBean(IFileStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		try {
			// write
			String name1 = "projectA";
			Project project1 = Project.builder().name(name1).build();
			project1 = storage.write(dir, project1);

			String name2 = "projectB";
			Project project2 = Project.builder().name(name2).build();
			project2 = storage.write(dir, project2);

			// sabotage
			Files.write(new File(storage.location(dir, Project.class, KeyParams.of(name1)), "data.json").toPath(),
					"Set invalid file!".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE,
					StandardOpenOption.TRUNCATE_EXISTING);

			assertThat(storage.list(dir, Project.class, null)).containsExactly(project2);
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testCollectionPeoperties(@Autowired ApplicationContext context) throws IOException {
		IFileStorage storage = context.getBean(IFileStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		try {
			// write
			String name1 = "projectA";
			Project project1 = Project.builder().name(name1).description("group a").build();
			project1 = storage.write(dir, project1);

			String name2 = "projectB";
			Project project2 = Project.builder().name(name2).description("group b").build();
			project2 = storage.write(dir, project2);

			Map<String, Map<String, Object>> properties;

			properties = storage.properties(dir, Project.class, KeyParams.of("name;description"), (SearchParams) null);

			assertThat(properties).hasSize(2);

			Map<String, Object> pa = properties.get("projectA");
			assertThat(pa).containsEntry("name", "projectA");
			assertThat(pa).containsEntry("description", "group a");
			assertThat(pa.get("id")).isNull();

			Map<String, Object> pb = properties.get("projectB");
			assertThat(pb).containsEntry("name", "projectB");
			assertThat(pb).containsEntry("description", "group b");
			assertThat(pb.get("id")).isNull();

			FileFilter predicate = new FileFilter(factory.read("{\"description\":{\"$c\": \"a\"}}".getBytes()));
			properties = storage.properties(dir, Project.class, KeyParams.of("name;description"),
					SearchParams.builder().filter(predicate).build());
			assertThat(properties).hasSize(1);

			pa = properties.get("projectA");
			assertThat(pa).containsEntry("name", "projectA");
			assertThat(pa).containsEntry("description", "group a");
			assertThat(pa.get("id")).isNull();

			assertThat(properties.get("projectB")).isNull();

			properties = storage
					.properties(dir, Project.class, KeyParams.of("name;description"),
							SearchParams
									.builder().sorting(FileSorting.builder().property("name")
											.sort(FileSorting.SORT_DESCENDING).nullsFirst(true).secondary(null).build())
									.build());

			// keys ordered according to attribute name in descending order
			assertThat(properties.keySet().stream().collect(Collectors.toList()))
					.isEqualTo(Arrays.asList("projectB", "projectA"));

			properties = storage
					.properties(dir, Project.class, null, SearchParams.builder()
							.paging(FilePaging.builder().skip(1).build()).sorting(FileSorting.builder().property("name")
									.sort(FileSorting.SORT_DESCENDING).nullsFirst(true).secondary(null).build())
							.build());

			// only the second
			assertThat(properties.keySet().stream().collect(Collectors.toList())).isEqualTo(Arrays.asList("projectA"));

			properties = storage
					.properties(dir, Project.class, null, SearchParams.builder()
							.paging(FilePaging.builder().max(1).build()).sorting(FileSorting.builder().property("name")
									.sort(FileSorting.SORT_DESCENDING).nullsFirst(true).secondary(null).build())
							.build());

			// only the first
			assertThat(properties.keySet().stream().collect(Collectors.toList())).isEqualTo(Arrays.asList("projectB"));

			List<Project> projects = storage.setProperty(dir, Project.class, "description", "same text", null);
			Project first = projects.get(0);
			assertThat(first.getDescription()).isEqualTo("same text");
			Project second = projects.get(1);
			assertThat(second.getDescription()).isEqualTo("same text");

			projects = storage.list(dir, Project.class, null);
			first = projects.get(0);
			assertThat(first.getDescription()).isEqualTo("same text");
			second = projects.get(1);
			assertThat(second.getDescription()).isEqualTo("same text");

		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testCollectionPeopertiesTyped(@Autowired ApplicationContext context) throws IOException {
		IFileStorageTyped<Project> storage = context.getBean(ProjectStorage.class);
		File dir = new File("target/data/storage_" + System.currentTimeMillis());
		try {
			// write
			String name1 = "projectA";
			Project project1 = Project.builder().name(name1).description("group a").build();
			project1 = storage.write(dir, project1);

			String name2 = "projectB";
			Project project2 = Project.builder().name(name2).description("group b").build();
			project2 = storage.write(dir, project2);

			Map<String, Map<String, Object>> properties;

			properties = storage.properties(dir, KeyParams.of("name;description"), (SearchParams) null);

			assertThat(properties).hasSize(2);

			Map<String, Object> pa = properties.get("projectA");
			assertThat(pa).containsEntry("name", "projectA");
			assertThat(pa).containsEntry("description", "group a");
			assertThat(pa.get("id")).isNull();

			Map<String, Object> pb = properties.get("projectB");
			assertThat(pb).containsEntry("name", "projectB");
			assertThat(pb).containsEntry("description", "group b");
			assertThat(pb.get("id")).isNull();

			FileFilter predicate = new FileFilter(factory.read("{\"description\":{\"$c\": \"a\"}}".getBytes()));
			properties = storage.properties(dir, KeyParams.of("name;description"),
					SearchParams.builder().filter(predicate).build());
			assertThat(properties).hasSize(1);

			pa = properties.get("projectA");
			assertThat(pa).containsEntry("name", "projectA");
			assertThat(pa).containsEntry("description", "group a");
			assertThat(pa.get("id")).isNull();

			assertThat(properties.get("projectB")).isNull();

			properties = storage
					.properties(dir, KeyParams.of("name;description"),
							SearchParams
									.builder().sorting(FileSorting.builder().property("name")
											.sort(FileSorting.SORT_DESCENDING).nullsFirst(true).secondary(null).build())
									.build());

			// keys ordered according to attribute name in descending order
			assertThat(properties.keySet().stream().collect(Collectors.toList()))
					.isEqualTo(Arrays.asList("projectB", "projectA"));

			properties = storage.properties(dir, null,
					SearchParams.builder().paging(FilePaging.builder().skip(1).build())
							.sorting(FileSorting.builder().property("name").sort(FileSorting.SORT_DESCENDING)
									.nullsFirst(true).secondary(null).build())
							.build());

			// only the second
			assertThat(properties.keySet().stream().collect(Collectors.toList())).isEqualTo(Arrays.asList("projectA"));

			properties = storage.properties(dir, null,
					SearchParams.builder().paging(FilePaging.builder().max(1).build())
							.sorting(FileSorting.builder().property("name").sort(FileSorting.SORT_DESCENDING)
									.nullsFirst(true).secondary(null).build())
							.build());

			// only the first
			assertThat(properties.keySet().stream().collect(Collectors.toList())).isEqualTo(Arrays.asList("projectB"));

			List<Project> projects = storage.setProperty(dir, "description", "same text", null);
			Project first = projects.get(0);
			assertThat(first.getDescription()).isEqualTo("same text");
			Project second = projects.get(1);
			assertThat(second.getDescription()).isEqualTo("same text");

			projects = storage.list(dir, null);
			first = projects.get(0);
			assertThat(first.getDescription()).isEqualTo("same text");
			second = projects.get(1);
			assertThat(second.getDescription()).isEqualTo("same text");
		} finally {
			try {
				FileUtils.delete(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}