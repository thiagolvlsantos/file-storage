package io.github.thiagolvlsantos.file.storage.search;

import java.util.LinkedList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@SuperBuilder
public class FileSorting {
	public static final String SORT_ASCENDING = "asc";
	public static final String SORT_DESCENDING = "desc";

	private String property; // sorting property
	private String sort; // strategy, i.e. asc, dec, etc.
	private boolean nullsFirst;

	@Builder.Default
	private List<FileSorting> secondary = new LinkedList<>();

	public boolean isValid() {
		return property != null && sort != null;
	}

	public boolean isAscending() {
		return SORT_ASCENDING.equalsIgnoreCase(sort);
	}

	public boolean isDescending() {
		return SORT_DESCENDING.equalsIgnoreCase(sort);
	}

	public boolean isSameProperty(FileSorting other) {
		return property.equalsIgnoreCase(other.property);
	}
}