package io.github.thiagolvlsantos.file.storage;

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

	public Integer getStart(Integer limit) {
		return Objects.isNull(skip) ? 0 : skip;
	}

	public Integer getEnd(Integer limit) {
		return Objects.isNull(max) ? limit : getStart(limit) + Math.min(max, limit);
	}
}
