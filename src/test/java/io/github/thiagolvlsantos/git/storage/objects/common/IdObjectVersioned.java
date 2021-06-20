package io.github.thiagolvlsantos.git.storage.objects.common;

import io.github.thiagolvlsantos.git.storage.concurrency.GitRevision;
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

	@GitRevision
	@Builder.Default
	private Long revision = 0L;
}