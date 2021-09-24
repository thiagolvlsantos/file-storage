package io.github.thiagolvlsantos.git.storage.resource;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
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
public class ResourceMetadata {

	private String path;
	private String kind;
	@Builder.Default
	private LocalDateTime timestamp = LocalDateTime.now();
}