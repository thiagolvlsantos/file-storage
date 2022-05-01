package io.github.thiagolvlsantos.file.storage.util.entity;

import java.time.LocalDateTime;

import io.github.thiagolvlsantos.file.storage.audit.FileChanged;
import io.github.thiagolvlsantos.file.storage.audit.FileChangedBy;
import io.github.thiagolvlsantos.file.storage.audit.FileCreated;
import io.github.thiagolvlsantos.file.storage.audit.FileCreatedBy;
import io.github.thiagolvlsantos.file.storage.audit.IFileAudit.AuthorInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(builderMethodName = "builderFileObjectAuditable")
public class FileObjectAuditable extends FileObject {

	@FileCreated
	private LocalDateTime created;

	@FileCreatedBy
	private AuthorInfo createdBy;

	@FileChanged
	private LocalDateTime changed;

	@FileChangedBy
	private AuthorInfo changedBy;
}