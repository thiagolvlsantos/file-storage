package io.github.thiagolvlsantos.file.storage.objects;

import io.github.thiagolvlsantos.file.storage.entity.FileName;
import io.github.thiagolvlsantos.file.storage.entity.FileRepo;
import io.github.thiagolvlsantos.file.storage.entity.FileWrapped;
import io.github.thiagolvlsantos.file.storage.util.entity.FileNamedObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@FileRepo(ObjectWrapped.REPO)
@FileName(ObjectWrapped.FILE)
@FileWrapped
public class ObjectWrapped extends FileNamedObject {

	public static final String REPO = "repository";
	public static final String FILE = "wrapped";
}