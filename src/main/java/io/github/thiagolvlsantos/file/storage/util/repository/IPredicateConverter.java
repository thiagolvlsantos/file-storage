package io.github.thiagolvlsantos.file.storage.util.repository;

import java.util.function.Predicate;

public interface IPredicateConverter {

	Predicate<Object> toPredicate(String filter);
}
