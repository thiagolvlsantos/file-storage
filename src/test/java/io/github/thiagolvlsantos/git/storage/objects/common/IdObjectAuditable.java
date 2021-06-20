package io.github.thiagolvlsantos.git.storage.objects.common;

import java.time.LocalDateTime;

import io.github.thiagolvlsantos.git.storage.audit.GitChanged;
import io.github.thiagolvlsantos.git.storage.audit.GitCreated;
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
public class IdObjectAuditable extends IdObject {

	@GitCreated
	@Builder.Default
	private LocalDateTime created = LocalDateTime.now();

	@GitChanged
	@Builder.Default
	private LocalDateTime changed = LocalDateTime.now();
}