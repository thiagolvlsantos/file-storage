package io.github.thiagolvlsantos.file.storage.objects.common;

import java.time.LocalDateTime;

import io.github.thiagolvlsantos.file.storage.audit.FileChanged;
import io.github.thiagolvlsantos.file.storage.audit.FileCreated;
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

	@FileCreated
	@Builder.Default
	private LocalDateTime created = LocalDateTime.now();

	@FileChanged
	@Builder.Default
	private LocalDateTime changed = LocalDateTime.now();
}