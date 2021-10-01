package io.github.thiagolvlsantos.file.storage.objects.common;

import io.github.thiagolvlsantos.file.storage.identity.FileId;
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
public class IdObject {

	@FileId
	private Long id;
}