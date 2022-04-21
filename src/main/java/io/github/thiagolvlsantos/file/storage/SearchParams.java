package io.github.thiagolvlsantos.file.storage;

import io.github.thiagolvlsantos.file.storage.search.FilePaging;
import io.github.thiagolvlsantos.file.storage.search.FileFilter;
import io.github.thiagolvlsantos.file.storage.search.FileSorting;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchParams {
	private FileFilter filter;
	private FilePaging paging;
	private FileSorting sorting;
}
