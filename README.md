# git-storage

[![CI with Maven](https://github.com/thiagolvlsantos/git-storage/actions/workflows/maven.yml/badge.svg)](https://github.com/thiagolvlsantos/git-storage/actions/workflows/maven.yml)
[![CI with CodeQL](https://github.com/thiagolvlsantos/git-storage/actions/workflows/codeql.yml/badge.svg)](https://github.com/thiagolvlsantos/git-storage/actions/workflows/codeql.yml)
[![CI with Sonar](https://github.com/thiagolvlsantos/git-storage/actions/workflows/sonar.yml/badge.svg)](https://github.com/thiagolvlsantos/git-storage/actions/workflows/sonar.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=thiagolvlsantos_git-storage&metric=alert_status)](https://sonarcloud.io/dashboard?id=thiagolvlsantos_git-storage)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=thiagolvlsantos_git-storage&metric=coverage)](https://sonarcloud.io/dashboard?id=thiagolvlsantos_git-storage)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.thiagolvlsantos/git-storage/badge.svg)](https://repo1.maven.org/maven2/io/github/thiagolvlsantos/git-storage/)
[![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)](http://www.apache.org/licenses/LICENSE-2.0)


## Transactions using Git repositories. 

Imagine you reading/writting locally to your file system attached to your Git repository as a transaction in your service class. An abstraction, using Aspects, to read and write files in your file system and automatically have them pulled/commited/pushed to your Git repository.

Welcome to ``git-storage``, see bellow how simple it is.

## Usage

Include latest version [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.thiagolvlsantos/git-storage/badge.svg)](https://repo1.maven.org/maven2/io/github/thiagolvlsantos/git-storage/) to your project.

```xml
		<dependency>
			<groupId>io.github.thiagolvlsantos</groupId>
			<artifactId>git-storage</artifactId>
			<version>${latestVersion}</version>
		</dependency>
```

## Build

Localy, from this root directory call Maven commands or `bin/<script name>` at our will.
