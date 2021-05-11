package com.thiagolvlsantos.git.storage.util.annotations;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class PairValue<T> {
	T annotation;
	private Field field;
	private Method read;
	private Method write;
	@ToString.Include
	private String name;
	@ToString.Include
	private Object value;
}