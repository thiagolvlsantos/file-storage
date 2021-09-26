package io.github.thiagolvlsantos.git.storage;

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
public class GitPaging {
	private Integer skip;
	private Integer max;

	public Integer getStart(Integer limit) {
		return Objects.isNull(skip) ? 0 : Math.min(skip, limit);
	}

	public Integer getEnd(Integer limit) {
		Integer start = getStart(limit);
		Integer gap = limit - start;
		return Objects.isNull(max) ? gap : start + Math.min(max, gap);
	}
}
