package io.github.thiagolvlsantos.git.storage.objects;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import io.github.thiagolvlsantos.git.storage.GitEntity;
import io.github.thiagolvlsantos.git.storage.audit.GitChanged;
import io.github.thiagolvlsantos.git.storage.objects.common.BasicNamedObject;
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
@GitEntity(Project.REPO)
public class Project extends BasicNamedObject {

	public static final String REPO = "projects";

	@GitChanged(UUIInitializer.class)
	private UUID uuid;

	@Builder.Default
	private List<ProductAlias> products = new LinkedList<>();
}