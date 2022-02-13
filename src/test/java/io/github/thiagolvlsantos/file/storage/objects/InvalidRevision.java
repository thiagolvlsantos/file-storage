package io.github.thiagolvlsantos.file.storage.objects;

import io.github.thiagolvlsantos.file.storage.concurrency.FileRevision;
import io.github.thiagolvlsantos.file.storage.entity.FileRepo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@FileRepo("anyname")
public class InvalidRevision {

	@FileRevision
	private String revision; // revisions can only be a subclass of Number.class
}
