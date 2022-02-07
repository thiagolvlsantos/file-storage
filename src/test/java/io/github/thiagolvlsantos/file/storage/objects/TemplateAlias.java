package io.github.thiagolvlsantos.file.storage.objects;

import io.github.thiagolvlsantos.file.storage.FileAlias;
import io.github.thiagolvlsantos.file.storage.identity.FileKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FileAlias(Template.class)
public class TemplateAlias {

	@FileKey
	private String name;
}