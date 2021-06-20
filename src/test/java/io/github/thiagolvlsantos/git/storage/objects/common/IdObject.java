package io.github.thiagolvlsantos.git.storage.objects.common;

import io.github.thiagolvlsantos.git.storage.identity.GitId;
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

	@GitId
	private Long id;
}