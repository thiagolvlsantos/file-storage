package io.github.thiagolvlsantos.file.storage.audit.impl;

import io.github.thiagolvlsantos.file.storage.audit.IFileAudit;

public class FileAuditDefault implements IFileAudit {

	private AuthorInfo empty = new AuthorInfo("", "");

	@Override
	public AuthorInfo author() {
		return empty;
	}
}
