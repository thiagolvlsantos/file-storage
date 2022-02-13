package io.github.thiagolvlsantos.file.storage.objects;

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
@FileRepo(ObjectMeta.REPO)
public class ObjectMeta extends NamedObject {

	public static final String REPO = "repository";
}