package com.cheetah.logmate.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Document("dbaudit")
public class DbAudit {

	@Id
	private String id;
	@Builder.Default
	private List<ColumnState> changes = new ArrayList<>();
	private LocalDateTime dateTime;
	private String tableName;
	private String userId;
	@Builder.Default
	private String applicationName = System.getenv("APPLICATION_NAME");
	private QueryType queryType;

}
