package io.github.thiagolvlsantos.file.storage.objects.common;

import io.github.thiagolvlsantos.file.storage.concurrency.FileRevision;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
	@Builder.Default
	private Long revision = 0L;
}