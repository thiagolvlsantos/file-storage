package io.github.thiagolvlsantos.file.storage.util.repository;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString
@SuppressWarnings("serial")
public class ResourceVO implements Serializable {

	private ResourceMetadataVO metadata;
	private ResourceContentVO content;
}
