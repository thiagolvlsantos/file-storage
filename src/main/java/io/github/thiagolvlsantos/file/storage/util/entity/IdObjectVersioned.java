package io.github.thiagolvlsantos.file.storage.util.entity;

import io.github.thiagolvlsantos.file.storage.concurrency.FileRevision;
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
public class IdObjectVersioned extends IdObject {

	@FileRevision
	private Long revision; // could be initialized as 0L
}