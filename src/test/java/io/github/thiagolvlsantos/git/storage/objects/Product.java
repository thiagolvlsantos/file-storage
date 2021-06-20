package io.github.thiagolvlsantos.git.storage.objects;

import io.github.thiagolvlsantos.git.storage.GitEntity;
import io.github.thiagolvlsantos.git.storage.identity.GitKey;
import io.github.thiagolvlsantos.git.storage.objects.common.BasicNamedObject;
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
@GitEntity(Product.REPO)
public class Product extends BasicNamedObject {

	public static final String REPO = "products";

	@GitKey(order = -1) // before product name
	private ProjectAlias project;
}