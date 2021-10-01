package io.github.thiagolvlsantos.file.storage.objects;

import io.github.thiagolvlsantos.file.storage.FileEntity;
import io.github.thiagolvlsantos.file.storage.identity.FileKey;
import io.github.thiagolvlsantos.file.storage.objects.common.BasicNamedObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@FileEntity(Product.REPO)
public class Product extends BasicNamedObject {

	public static final String REPO = "products";

	@FileKey(order = -1) // before product name
	private ProjectAlias project;
}