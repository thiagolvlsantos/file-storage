package io.github.thiagolvlsantos.file.storage.exceptions;

@SuppressWarnings("serial")
public class FileStorageAttributeNotFoundException extends FileStorageNotFoundException {

	public FileStorageAttributeNotFoundException(String attribute, Object current, Throwable cause) {
		super("Attribute '" + attribute + "' not found for type: " + (current != null ? current.getClass() : null),
				cause);
	}
}
