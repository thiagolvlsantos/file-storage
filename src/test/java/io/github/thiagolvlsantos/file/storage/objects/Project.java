package io.github.thiagolvlsantos.file.storage.objects;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import io.github.thiagolvlsantos.file.storage.FileEntity;
import io.github.thiagolvlsantos.file.storage.audit.FileChanged;
import io.github.thiagolvlsantos.file.storage.objects.common.BasicNamedObject;
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
@FileEntity(Project.REPO)
public class Project extends BasicNamedObject {

	public static final String REPO = "projects";

	private ProjectAlias parent;

	@FileChanged(UUIInitializer.class)
	private UUID uuid;

	@Builder.Default
	private List<ProductAlias> products = new LinkedList<>();
}