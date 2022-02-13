package io.github.thiagolvlsantos.file.storage.objects;

import io.github.thiagolvlsantos.file.storage.entity.FileName;
import io.github.thiagolvlsantos.file.storage.entity.FileRepo;
import io.github.thiagolvlsantos.file.storage.util.entity.NamedObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@FileRepo(ObjectOther.REPO)
@FileName(ObjectOther.FILE)
public class ObjectOther extends NamedObject {

	public static final String REPO = "repository";
	public static final String FILE = "other";
}