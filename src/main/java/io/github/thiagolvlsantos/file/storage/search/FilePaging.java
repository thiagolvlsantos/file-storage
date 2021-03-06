package io.github.thiagolvlsantos.file.storage.search;

import java.util.Objects;

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
@ToString
@SuperBuilder
public class FilePaging {
	private Integer skip;
	private Integer max;

	public Integer getStart() {
		return Objects.isNull(skip) ? 0 : skip;
	}

	public Integer getEnd(Integer limit) {
		return Objects.isNull(max) ? limit : getStart() + Math.min(max, limit);
	}
}
