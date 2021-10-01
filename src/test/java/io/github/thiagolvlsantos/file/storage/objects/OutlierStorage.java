package io.github.thiagolvlsantos.file.storage.objects;

import org.springframework.stereotype.Component;

import io.github.thiagolvlsantos.file.storage.impl.AFileStorageTypedImpl;

@Component
public class OutlierStorage extends AFileStorageTypedImpl<Outlier> {

	public OutlierStorage() {
		super(Outlier.class);
	}
}