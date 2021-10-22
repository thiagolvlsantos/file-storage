# file-storage

[![CI with Maven](https://github.com/thiagolvlsantos/file-storage/actions/workflows/maven.yml/badge.svg)](https://github.com/thiagolvlsantos/file-storage/actions/workflows/maven.yml)
[![CI with CodeQL](https://github.com/thiagolvlsantos/file-storage/actions/workflows/codeql.yml/badge.svg)](https://github.com/thiagolvlsantos/file-storage/actions/workflows/codeql.yml)
[![CI with Sonar](https://github.com/thiagolvlsantos/file-storage/actions/workflows/sonar.yml/badge.svg)](https://github.com/thiagolvlsantos/file-storage/actions/workflows/sonar.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=thiagolvlsantos_file-storage&metric=alert_status)](https://sonarcloud.io/dashboard?id=thiagolvlsantos_file-storage)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=thiagolvlsantos_file-storage&metric=coverage)](https://sonarcloud.io/dashboard?id=thiagolvlsantos_file-storage)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.thiagolvlsantos/file-storage/badge.svg)](https://repo1.maven.org/maven2/io/github/thiagolvlsantos/file-storage/)
[![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)](http://www.apache.org/licenses/LICENSE-2.0)


## Objects storage in file system.
Lets suppose you need to:
- save an object in files systems in straigthforward manner, in readable files (i.e. JSON);
- have a well defined interface to store and return these objects;
- have type polimorphism returning the concrete class you persisted;
- have ways to perform parametrized queries on this repository;
- have the possibility to extend Object concept with 'resources' which are files associated to a given type (new concept);
- and more.

Depending on your needs, the use of a database with file storage is not necessary, and moreover easy access to these files does not mean that you can easily find usefull information just by crawling file system.

These are some of the capabilities provided by `file-storage`.

## Usage

Include latest version [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.thiagolvlsantos/file-storage/badge.svg)](https://repo1.maven.org/maven2/io/github/thiagolvlsantos/file-storage/) to your project.

```xml
		<dependency>
			<groupId>io.github.thiagolvlsantos</groupId>
			<artifactId>file-storage</artifactId>
			<version>${latestVersion}</version>
		</dependency>
```

## Add `@EnableFileStorage` to you app
It will provide an instance of `IStorageFile` which is the generic interface for objects storage.

```java
...
@EnableFileStorage
public class Application {
	...main(String[] args) {...}
}
```

## Add `@FileEntity` related annotations to your object
Its kind of Hibernate annotations, but in this case we are annotating information to describe how a given object will be persisted to the file system.

Bellow the example of an object of type `Project` with a set of possible annotations. The semantics of each annotation is explained bellow in sequence.

```java
...

@EntityFile("projects")
public class Project {
	// file id field, objects can be found by keys (@FileKey) or ids(@FileIds).
	@FileId 
	private Long id; 

	// concurrency control, increases every save action
	@FileRevision
	private Long revision; 

	//creation time, only set once on first save action
	@FileCreated
	private LocalDateTime created = LocalDateTime.now(); 

	// update time, defaul behavior LocalDateTime.now() when changes are made.
	@FileChanged
	private LocalDateTime changed = LocalDateTime.now(); 

	// project name, if there are multipl keys, attribute 'order' can be used.
	@FileKey(order = 0)
	private String name; 

	// alias for project parent
	private ProjectAlias parent; 

	@FileChanged(UUIInitializer.class)
	private UUID uuid; // example of custom changed info

	...
}
```
The usage of annotations above has the following semantics:
|Annotation|Semantics|
|-|-|
|`@FileEntity`| Stands for the entity name on file system. In the example above, supposing a base directory `/data`, the Project objects will be persisted to `/data/@projects` acording to the value in the annotation.|
|`@FileId`|Stands for id field(s), this field is set automatically on saving actions, similar to @Id in Hibernate|
|`@FileRevision`| Stands for a concurrency control on saving actions in order to avoid rewrite of data without having set the right revision (version behind current), similar to @Revision in Hibernate|
|`@FileCreate`| Stands for an attribute that will be keeped unchanged after the first saving action. Kind of Hibernate audit annotations.|
|`@FileChanged`| Stands for an attribute that will be changed on every saving action. Kind of Hibernate audit annotations. Both `@FileCreated` and `@FileChanged` admit custom generators.|
|`@FileKey`| Stands for attributes that will be used as directory structure in file system. In the previous example, if we have a Project named `example` and we send a `IFileStorage` save it a directory with name `/data/@projects/example` will be created where the serialized version will lay and its resources will reside under folder `/data/@project/example/@resources`.|

`IFileStorage` will refuse saving objects without minimal annotations: `@FileEntity`, `@FileId` and `@FileKey`. The other annotations are optional.

## Using `IFileStorage` to persist objects
Application using a service...
```java
...
@EnableFileStorage
public class Application {
	...main(String[] args) {
		...
		ProjetService service = ...;
		service.save(new Project("example"));
		...
	}
}
```
to avoid duplicaton and using a repository...
```java 
...
@Service
public class ProjectService {
	private @Autowired ProjectRepository repository;

	public void save(Project p) {
		if(!repository.exists(p)) {
			respository.save(p);
		} else {
			throw new ObjectAlreadExists("choose you exception!");
		}
	}
}
```
to save object in file system.
```java
...
@Respository
public class ProjectRespository ... {

private @Value("#{storage.directory:/data}") String dir;
private @Autowired IFileStorage storage;

private File baseDir() {
	return new File(dir);
}

public boolean exists(Project p) {
	return storage.exists(baseDir(),p);
}

public void save(Project p) {
	storage.write(baseDir(),p);
}

...
```

After this call, supposing an initially empty folder `/data`, we`ll have the structure:
|dir|content|
|-|-|
|`/data/@projects`| With data related to all projects|
|`/data/@projects/example`| With data related to the specific project `example`|
|`/data/@projects/example/@resources` | With all project `example` resource files|
|`/data/@projects/.current`| Controll file for ids sequencial. |
|`/data/@projects/.index`| Controll directory for mapping keys to ids and vice-versa. |


## Interface `IFileSerializer` abstraction
The serializer in `IFileStorage` is reponsible for preparing and saving the object itself to the file system.

The default implementation saves/restores objects in `JSON` format which can be easily read.

You can change it using `setSerializer(...)` on `IFileStorage` interafce.

## Performing queries on objects
Call `IFileStorage` passing a `Predicate` object which will be user as filter for selection.

```java
...
	//filtering project with name starting with 'm'
	Predicate<Project> p = (p)->p.getName().startsWith("m");
	respository.search(p);
...
```
The predicate can be as complex as you want. For the repository its complexity wil not matter, all projects will be verified.

```java
...
private @Value("#{storage.directory:/data}") String dir;
private @Autowired IFileStorage storage;

private File baseDir() {
	return new File(dir);
}

private List<Project> query(Predicate<Project> filter) {
	return storage.list(baseDir(),Project.class,new FilePredicate(filter),null,null);
}
...

```

### Pagination and Sorting are always optional
You can use, or not, `FilePaging` and `FileSorting` for paging and sorting on any search methods, for objects or resources.

## Resources
An extension of object concept to cope also files as part of object elements besides attributes and methods. 

For example, suppose an entity called `Template`, it can have a name attribute, and a content attribute. Depending on how much templates are possible with that name we could have to crate a list attribute to keep track on then. 

but could be just an object with a name, and the templates related to that name could be saved as resources in `/data/@templates/mytemplate/@resources`.

Check `IFileStorage.*Resource*()` methods to save/update/query these extra object dimension.

# Using `file-storage` in conjuction with `git-transactions`
Imagine a world without databases (I didn`t say without 'data'), a scenario where you already have object keys to access information in a straightforward manner. Yes, you can do it by using a NoSql database, but you already have your file system and can use it to navigate/edit your data. 

Why not organizing you objects in directories that can be easily accessed? Furthermore use an API like ``git-transactions`` to automatically pull/commit/push this structure to a Git repository. It`s a perfect match, an object API to write data into file system in a simple structure, and an API to automatically have it persisted in your Git repository, there is your database with:
- Easy visualization;
- Resiliency;
- History.

Just to be clear, I`not saying that this is a 'database' for massive data, but most part of applications can use such a simpler structure where objects are stored in file systems as JSON without the majority of restrictions imposed by relational databases.

## Build

Localy, from this root directory call Maven commands or `bin/<script name>` at our will.
