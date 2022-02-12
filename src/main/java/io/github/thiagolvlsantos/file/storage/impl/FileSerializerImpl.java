package io.github.thiagolvlsantos.file.storage.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.github.thiagolvlsantos.file.storage.FileEntityName;
import io.github.thiagolvlsantos.file.storage.FileEntityWrapped;
import io.github.thiagolvlsantos.file.storage.IFileSerializer;
import io.github.thiagolvlsantos.file.storage.exceptions.FileStorageException;
import io.github.thiagolvlsantos.file.storage.exceptions.FileStorageNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Component
public class FileSerializerImpl implements IFileSerializer {

	private ObjectMapper mapper;
	private ObjectMapper mapperWrapped;
	private Map<Class<?>, Boolean> wrapped = new HashMap<>();

	@PostConstruct
	public void configure() {
		mapper = configure(new ObjectMapper());
		mapperWrapped = configure(new ObjectMapper());
		mapperWrapped.activateDefaultTypingAsProperty(mapperWrapped.getPolymorphicTypeValidator(),
				ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT, "@class");
	}

	private ObjectMapper configure(ObjectMapper mapper) {
		mapper = mapper// specific instance
				.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)//
				.configure(MapperFeature.ALLOW_EXPLICIT_PROPERTY_RENAMING, true)//
				.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)//
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)//
				.enable(SerializationFeature.INDENT_OUTPUT);
		return mapper.registerModule(new JavaTimeModule());
	}

	@Override
	public <T> String getFile(Class<T> type) {
		String result = "meta";
		FileEntityName name = AnnotationUtils.findAnnotation(type, FileEntityName.class);
		if (name != null) {
			result = name.value();
		}
		return result + ".json";
	}

	@Override
	public <T> boolean isWrapped(Class<T> type) {
		Boolean wrap = wrapped.get(type);
		if (wrap == null) {
			wrapped.put(type, AnnotationUtils.findAnnotation(type, FileEntityWrapped.class) != null);
		}
		return wrapped.get(type);
	}

	@Override
	public <T> T decode(byte[] data, Class<T> type) {
		try {
			T tmp = mapper.readValue(data, type);
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
			return mapper.readValue(data, tr);
		} catch (IOException e) {
			throw new FileStorageException("Could not read value. '" + data + "'", e);
		}
	}

	@Override
	public String encode(Object instance) {
		try (ByteArrayOutputStream bout = new ByteArrayOutputStream()) {
			mapper.writeValue(bout, instance);
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
			if (isWrapped(type)) {
				ObjectWrapper wrapper = mapperWrapped.readValue(file, ObjectWrapper.class);
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
			if (isWrapped(instance.getClass())) {
				mapperWrapped.writeValue(file, new ObjectWrapper(instance));
			} else {
				mapper.writeValue(file, instance);
			}
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