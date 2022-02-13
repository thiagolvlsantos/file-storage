package io.github.thiagolvlsantos.file.storage.objects;

import io.github.thiagolvlsantos.file.storage.entity.FileRepo;
import io.github.thiagolvlsantos.file.storage.identity.FileKey;
import io.github.thiagolvlsantos.file.storage.util.entity.NamedObject;
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
@FileRepo(Product.REPO)
public class Product extends NamedObject {

	public static final String REPO = "products";

	@FileKey(order = -1) // before product name
	private ProjectAlias project;
}