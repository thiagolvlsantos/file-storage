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

	public Integer getSkip() {
		return Objects.isNull(skip) ? 0 : skip;
	}

	public Integer getMax(Integer limit) {
		return Objects.isNull(max) ? limit : Math.min(max, limit);
	}
}
