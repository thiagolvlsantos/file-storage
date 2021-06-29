package io.github.thiagolvlsantos.git.storage.impl;

import java.io.File;
import java.io.IOException;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.github.thiagolvlsantos.git.storage.IGitSerializer;
import io.github.thiagolvlsantos.git.storage.exceptions.GitStorageException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Component
public class GitSerializerImpl implements IGitSerializer {

	private ObjectMapper mapper;

	@PostConstruct
	public void configure() {
		mapper = new ObjectMapper()// specific instance
				.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)//
				.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.activateDefaultTypingAsProperty(mapper.getPolymorphicTypeValidator(),
				ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT, "@class");
		mapper.registerModule(new JavaTimeModule());
	}

	@Override
	public <T> T readValue(File file, Class<T> type) {
		try {
			ObjectWrapper wrapper = mapper.readValue(file, ObjectWrapper.class);
			return type.cast(wrapper.getObject());
		} catch (IOException e) {
			throw new GitStorageException("Could not read object.", e);
		}
	}

	@Override
	public <T> void writeValue(File file, T instance) {
		try {
			mapper.writeValue(file, new ObjectWrapper(instance));
		} catch (IOException e) {
			throw new GitStorageException("Could not write object.", e);
		}
	}

	@Getter
	@Setter
	@AllArgsConstructor
	@NoArgsConstructor
	public static class ObjectWrapper {
		private Object object;
	}
}