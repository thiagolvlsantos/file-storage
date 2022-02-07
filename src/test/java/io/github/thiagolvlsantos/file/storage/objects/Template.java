package io.github.thiagolvlsantos.file.storage.objects;

import io.github.thiagolvlsantos.file.storage.FileEntity;
import io.github.thiagolvlsantos.file.storage.util.entity.NamedObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@FileEntity(Template.REPO)
public class Template extends NamedObject {

	public static final String REPO = "templates";
}