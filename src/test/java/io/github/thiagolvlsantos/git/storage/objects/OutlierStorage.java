package io.github.thiagolvlsantos.git.storage.objects;

import org.springframework.stereotype.Component;

import io.github.thiagolvlsantos.git.storage.Outlier;
import io.github.thiagolvlsantos.git.storage.impl.AGitStorageTypedImpl;

@Component
public class OutlierStorage extends AGitStorageTypedImpl<Outlier> {

	public OutlierStorage() {
		super(Outlier.class);
	}
}