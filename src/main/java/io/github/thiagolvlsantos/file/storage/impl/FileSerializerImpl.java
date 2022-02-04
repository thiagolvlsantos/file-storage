package io.github.thiagolvlsantos.file.storage.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.github.thiagolvlsantos.file.storage.IFileSerializer;
import io.github.thiagolvlsantos.file.storage.exceptions.FileStorageException;
import io.github.thiagolvlsantos.file.storage.exceptions.FileStorageNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Component
public class FileSerializerImpl implements IFileSerializer {

	private ObjectMapper mapperClean;
	private ObjectMapper mapper;
	@Value("${gitt.storage.serializer.wrap:false}")
	private boolean wrap;

	@Override
	public String getExtension() {
		return "json";
	}

	@PostConstruct
	public void configure() {
		mapperClean = configure(new ObjectMapper());
		mapper = configure(new ObjectMapper());
		if (wrap) {
			mapper.activateDefaultTypingAsProperty(mapper.getPolymorphicTypeValidator(),
					ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT, "@class");
		}
	}

	private ObjectMapper configure(ObjectMapper mapper) {
		mapper = mapper// specific instance
				.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)//
				.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)//
				.enable(SerializationFeature.INDENT_OUTPUT);
		return mapper.registerModule(new JavaTimeModule());
	}

	@Override
	public <T> T decode(byte[] data, Class<T> type) {
		try {
			T tmp = mapperClean.readValue(data, type);
			return type.cast(tmp);
		} catch (IOException e) {
			throw new FileStorageException("Could not read value. '" + data + "'", e);
		}
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
			throw new FileStorageException("Could not read value. '" + data + "'", e);
		}
	}

	@Override
	public String encode(Object instance) {
		try (ByteArrayOutputStream bout = new ByteArrayOutputStream()) {
			mapperClean.writeValue(bout, instance);
			return new String(bout.toByteArray());
		} catch (IOException e) {
			throw new FileStorageException("Could not write value.", e);
		}
	}

	@Override
	public <T> T readValue(File file, Class<T> type) {
		if (!file.exists()) {
			throw new FileStorageNotFoundException("Object not found.", null);
		}
		try {
			Object obj = null;
			if (wrap) {
				ObjectWrapper wrapper = mapper.readValue(file, ObjectWrapper.class);
				obj = wrapper.getObject();
			} else {
				obj = mapper.readValue(file, type);
			}
			return type.cast(obj);
		} catch (IOException e) {
			throw new FileStorageException("Could not read object.", e);
		}
	}

	@Override
	public <T> void writeValue(File file, T instance) {
		try {
			mapper.writeValue(file, wrap ? new ObjectWrapper(instance) : instance);
		} catch (IOException e) {
			throw new FileStorageException("Could not write object.", e);
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