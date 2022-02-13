package io.github.thiagolvlsantos.file.storage.objects;

import io.github.thiagolvlsantos.file.storage.entity.FileAliasFor;
import io.github.thiagolvlsantos.file.storage.identity.FileKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FileAliasFor(Product.class)
public class ProductAlias {

	@FileKey
	private ProjectAlias project;

	@FileKey
	private String name;
}