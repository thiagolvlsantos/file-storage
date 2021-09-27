package io.github.thiagolvlsantos.git.storage.exceptions;

@SuppressWarnings("serial")
public class GitStorageNotFoundException extends GitStorageException {

	public GitStorageNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}
