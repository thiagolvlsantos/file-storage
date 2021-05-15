package com.thiagolvlsantos.git.storage.util.annotations;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.AnnotationUtils;

import com.thiagolvlsantos.git.storage.GitAlias;
import com.thiagolvlsantos.git.storage.identity.GitId;
import com.thiagolvlsantos.git.storage.identity.GitKey;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class UtilAnnotations {

	@SuppressWarnings("unchecked")
	@SneakyThrows
	public static <T extends Annotation> PairValue<T>[] getValues(Class<T> annotation, Class<?> type, Object instance) {
		List<PairValue<T>> result = new LinkedList<>();
		Class<?> clazz = type;
		int index = 0;
		while (clazz != Object.class) {
			Field[] fields = clazz.getDeclaredFields();
			for (Field f : fields) {
				Annotation a = AnnotationUtils.findAnnotation(f, annotation);
				if (a != null) {
					PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(clazz, f.getName());
					Method read = pd.getReadMethod();
					Method write = pd.getWriteMethod();
					result.add(index++, (PairValue<T>) PairValue.builder().annotation(a).field(f).read(read)
							.write(write).name(f.getName()).value(read.invoke(instance)).build());
				}
			}
			Method[] methods = clazz.getDeclaredMethods();
			for (Method m : methods) {
				Annotation a = AnnotationUtils.findAnnotation(m, annotation);
				if (a != null) {
					result.add(index++, (PairValue<T>) PairValue.builder().annotation(a).read(m).name(m.getName())
							.value(m.invoke(instance)).build());
				}
			}
			clazz = clazz.getSuperclass();
			index = 0;
		}
		return result.toArray(new PairValue[0]);
	}

	public static Object[] getKeys(Class<?> type, Object instance) {
		PairValue<GitKey>[] keys = getValues(GitKey.class, type, instance);
		Arrays.sort(keys, (a, b) -> a.getAnnotation().order() - b.getAnnotation().order());
		List<Object> path = new LinkedList<>();
		Stream.of(keys).forEach(v -> {
			Object value = v.getValue();
			if (value != null) {
				Class<?> innerType = value.getClass();
				GitAlias alias = AnnotationUtils.findAnnotation(innerType, GitAlias.class);
				if (alias != null) {
					Object[] in = getKeys((Class<?>) innerType, value);
					for (Object o : in) {
						path.add(o);
					}
				} else {
					path.add(value);
				}
			}
		});
		if (log.isInfoEnabled()) {
			log.info("keys: {}", path);
		}
		return path.toArray(new Object[0]);
	}

	public static Object[] getIds(Class<?> type, Object instance) {
		PairValue<GitId>[] ids = UtilAnnotations.getValues(GitId.class, type, instance);
		Object[] result = Stream.of(ids).map(v -> v.getValue()).toArray();
		if (log.isInfoEnabled()) {
			log.info("ids: {}", Arrays.toString(result));
		}
		return result;
	}
}
