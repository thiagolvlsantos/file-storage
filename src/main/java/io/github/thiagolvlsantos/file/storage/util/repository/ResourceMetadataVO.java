package io.github.thiagolvlsantos.file.storage.util.repository;

import java.io.Serializable;
import java.time.LocalDateTime;

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
public class ResourceMetadataVO implements Serializable {
	public String path;
	public String encoding;
	public String contentType;
	public LocalDateTime timestamp;
}