package io.github.thiagolvlsantos.git.storage.objects.common;

import io.github.thiagolvlsantos.git.storage.identity.GitKey;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class BasicNamedObject extends IdObjectVersionedAuditable {

	@GitKey(order = 0)
	@Include
	private String name;

	private String description;
}