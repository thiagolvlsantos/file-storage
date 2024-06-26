package io.github.thiagolvlsantos.file.storage.util.entity;

import io.github.thiagolvlsantos.file.storage.identity.FileKey;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(builderMethodName = "builderFineNamedObject")
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class FileNamedObject extends FileObjectVersionedAuditable {

	@FileKey(order = 0)
	@Include
	private String name;

	private String description;
}