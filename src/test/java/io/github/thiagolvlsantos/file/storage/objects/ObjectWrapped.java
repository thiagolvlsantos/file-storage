package io.github.thiagolvlsantos.file.storage.objects;

import io.github.thiagolvlsantos.file.storage.FileEntity;
import io.github.thiagolvlsantos.file.storage.FileEntityName;
import io.github.thiagolvlsantos.file.storage.FileEntityWrapped;
import io.github.thiagolvlsantos.file.storage.util.entity.NamedObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@FileEntity(ObjectWrapped.REPO)
@FileEntityName(ObjectWrapped.FILE)
@FileEntityWrapped
public class ObjectWrapped extends NamedObject {

	public static final String REPO = "repository";
	public static final String FILE = "wrapped";
}