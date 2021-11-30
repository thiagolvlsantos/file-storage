package io.github.thiagolvlsantos.file.storage.annotations;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.AnnotationUtils;

import io.github.thiagolvlsantos.file.storage.FileAlias;
import io.github.thiagolvlsantos.file.storage.exceptions.FileStorageException;
import io.github.thiagolvlsantos.file.storage.identity.FileId;
import io.github.thiagolvlsantos.file.storage.identity.FileKey;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class UtilAnnotations {

	@SuppressWarnings("unchecked")
	public static <T extends Annotation> PairValue<T>[] getValues(Class<T> annotation, Class<?> type, Object instance) {
		try {
			List<PairValue<?>> result = new LinkedList<>();
			Class<?> clazz = type;
			int index = 0;
			while (clazz != Object.class) {
				Field[] fields = clazz.getDeclaredFields();
				for (Field f : fields) {
					Annotation a = AnnotationUtils.findAnnotation(f, annotation);
					if (a != null) {
						result.add(index++, fromField(clazz, instance, a, f));
					}
				}
				Method[] methods = clazz.getDeclaredMethods();
				for (Method m : methods) {
					Annotation a = AnnotationUtils.findAnnotation(m, annotation);
					if (a != null) {
						result.add(index++, fromMethod(instance, m, a));
					}
				}
				clazz = clazz.getSuperclass();
				index = 0;
			}
			return result.toArray(new PairValue[0]);
		} catch (SecurityException | IllegalAccessException | InvocationTargetException e) {
			throw new FileStorageException(e.getMessage(), e);
		}
	}

	private static PairValue<Object> fromField(Class<?> clazz, Object instance, Annotation a, Field f)
			throws IllegalAccessException, InvocationTargetException {
		PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(clazz, f.getName());
		if (pd == null) {
			throw new FileStorageException(
					"Invalid property: " + f.getName() + " for type: " + (clazz != null ? clazz : null), null);
		}
		Method read = pd.getReadMethod();
		Method write = pd.getWriteMethod();
		return PairValue.builder().annotation(a).field(f).read(read).write(write).name(f.getName())
				.value(read.invoke(instance)).build();
	}

	private static PairValue<Object> fromMethod(Object instance, Method m, Annotation a)
			throws IllegalAccessException, InvocationTargetException {
		return PairValue.builder().annotation(a).read(m).name(m.getName()).value(m.invoke(instance)).build();
	}

	public static Object[] getKeys(Class<?> type, Object instance) {
		PairValue<FileKey>[] keys = getValues(FileKey.class, type, instance);
		Arrays.sort(keys, (a, b) -> a.getAnnotation().order() - b.getAnnotation().order());
		List<Object> path = new LinkedList<>();
		Stream.of(keys).forEach(v -> {
			Object value = v.getValue();
			if (value != null) {
				Class<?> innerType = value.getClass();
				FileAlias alias = AnnotationUtils.findAnnotation(innerType, FileAlias.class);
				if (alias != null) {
					Collections.addAll(path, getKeys(innerType, value));
				} else {
					path.add(value);
				}
			}
		});
		log.info("keys: {}", path);
		return path.toArray(new Object[0]);
	}

	public static Object[] getIds(Class<?> type, Object instance) {
		PairValue<FileId>[] ids = UtilAnnotations.getValues(FileId.class, type, instance);
		Object[] result = Stream.of(ids).map(PairValue::getValue).toArray();
		log.info("ids: {}", Arrays.toString(result));
		return result;
	}
}
