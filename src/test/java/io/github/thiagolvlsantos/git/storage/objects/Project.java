package io.github.thiagolvlsantos.git.storage.objects;

import java.util.LinkedList;
import java.util.List;

import io.github.thiagolvlsantos.git.storage.GitEntity;
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

	@Builder.Default
	private List<ProductAlias> products = new LinkedList<>();
}