package com.cheetah.logmate.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Builder
@Getter
public class ColumnState {

	private String columnName;
	private Object currentValue;
	private Object previousValue;
	private boolean changed;
	
	
}
