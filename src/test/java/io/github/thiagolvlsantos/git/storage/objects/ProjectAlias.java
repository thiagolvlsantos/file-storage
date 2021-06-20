package io.github.thiagolvlsantos.git.storage.objects;

import io.github.thiagolvlsantos.git.storage.GitAlias;
import io.github.thiagolvlsantos.git.storage.identity.GitKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@GitAlias(Project.class)
public class ProjectAlias {

	@GitKey
	private String name;
}