package io.github.thiagolvlsantos.git.storage.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
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

	private ObjectMapper mapperClean;
	private ObjectMapper mapper;

	@PostConstruct
	public void configure() {
		mapperClean = configure(new ObjectMapper());
		mapper = configure(new ObjectMapper());
		mapper.activateDefaultTypingAsProperty(mapper.getPolymorphicTypeValidator(),
				ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT, "@class");
	}

	private ObjectMapper configure(ObjectMapper mapper) {
		mapper = mapper// specific instance
				.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)//
				.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)//
				.enable(SerializationFeature.INDENT_OUTPUT);
		return mapper.registerModule(new JavaTimeModule());
	}

	@Override
	public Object decode(String data, AnnotatedType type) {
		try {
			TypeReference<?> tr = new TypeReference<Object>() {
				@Override
				public Type getType() {
					return type.getType();
				}
			};
			return mapperClean.readValue(data, tr);
		} catch (IOException e) {
			throw new GitStorageException("Could not read value. '" + data + "'", e);
		}
	}

	@Override
	public String encode(Object instance) {
		try (ByteArrayOutputStream bout = new ByteArrayOutputStream()) {
			mapperClean.writeValue(bout, instance);
			return new String(bout.toByteArray());
		} catch (IOException e) {
			throw new GitStorageException("Could not write value.", e);
		}
	}

	@Override
	public <T> T readValue(File file, Class<T> type) {
		if (!file.exists()) {
			throw new GitStorageException("Object not found.", null);
		}
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