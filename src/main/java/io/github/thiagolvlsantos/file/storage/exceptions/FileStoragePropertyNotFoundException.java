package io.github.thiagolvlsantos.file.storage.exceptions;

@SuppressWarnings("serial")
public class FileStoragePropertyNotFoundException extends FileStorageNotFoundException {

	public FileStoragePropertyNotFoundException(String property, Object current, Throwable cause) {
		super("Property '" + property + "' not found for type: " + (current != null ? current.getClass() : null),
				cause);
	}
}
