package io.github.thiagolvlsantos.file.storage.objects;

import java.util.LinkedList;
import java.util.List;

import io.github.thiagolvlsantos.file.storage.entity.FileName;
import io.github.thiagolvlsantos.file.storage.entity.FileRepo;
import io.github.thiagolvlsantos.file.storage.identity.FileKey;
import io.github.thiagolvlsantos.file.storage.util.entity.FileObjectVersionedAuditable;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@FileRepo(Template.REPO)
@FileName(TemplateTargetAuthorization.NAME)
public class TemplateTargetAuthorization extends FileObjectVersionedAuditable {

	public static final String NAME = "target.authorization";

	@FileKey
	private TemplateAlias template;

	@FileKey
	private TargetAlias target;

	@Builder.Default
	private List<Authorization> authorizations = new LinkedList<>();
}