package io.github.thiagolvlsantos.git.storage.objects.common;

import io.github.thiagolvlsantos.git.storage.identity.GitKey;
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
public class BasicNamedObject extends IdObjectVersionedAuditable {

	@GitKey(order = 0)
	private String name;

	private String description;
}