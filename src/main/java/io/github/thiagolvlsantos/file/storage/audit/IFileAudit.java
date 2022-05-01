package io.github.thiagolvlsantos.file.storage.audit;

import io.github.thiagolvlsantos.file.storage.audit.impl.FileAuditDefault;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

public interface IFileAudit {

	AuthorInfo author();

	IFileAudit INSTANCE = new FileAuditDefault();

	@Setter
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@ToString
	public static class AuthorInfo {
		private String user;
		private String email;
	}
}
