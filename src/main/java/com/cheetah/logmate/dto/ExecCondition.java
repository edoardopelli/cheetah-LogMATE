package com.cheetah.logmate.dto;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class ExecCondition implements Condition {

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		return System.getenv("DB_LOG_ENABLED")!=null && System.getenv("DB_LOG_ENABLED").equals("true");
	}

}
