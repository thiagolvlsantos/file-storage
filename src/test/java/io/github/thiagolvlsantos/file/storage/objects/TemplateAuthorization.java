package io.github.thiagolvlsantos.file.storage.objects;

import java.util.LinkedList;
import java.util.List;

import io.github.thiagolvlsantos.file.storage.FileEntity;
import io.github.thiagolvlsantos.file.storage.FileEntityName;
import io.github.thiagolvlsantos.file.storage.identity.FileKey;
import io.github.thiagolvlsantos.file.storage.util.entity.IdObjectVersionedAuditable;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@FileEntity(TemplateAuthorization.REPO)
@FileEntityName("authorization")
public class TemplateAuthorization extends IdObjectVersionedAuditable {

	public static final String REPO = "templates";

	@FileKey
	private TemplateAlias template;

	@FileKey
	private TargetAlias target;

	@Builder.Default
	private List<Authorization> authorizations = new LinkedList<>();
}