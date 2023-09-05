package com.cheetah.logmate.dto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import jakarta.annotation.PostConstruct;

@Configuration
@Conditional(value = ExecCondition.class)
@PropertySources({
		@PropertySource(value = "classpath:/vgi-audit.properties"),
		@PropertySource(value = "classpath:/vgi-audit-local.properties", ignoreResourceNotFound = true),
		@PropertySource(value = "classpath:/vgi-audit-dev.properties", ignoreResourceNotFound = true),
		@PropertySource(value = "classpath:/vgi-audit-test.properties", ignoreResourceNotFound = true),
		@PropertySource(value = "classpath:/vgi-audit-prod.properties", ignoreResourceNotFound = true)
}
)
@EnableMongoRepositories(basePackages = "com.vgi.common.audit.dblog.repositories")
public class DbLogConfig {

	// The purpose of this class is to simply remove the _class field from the collections on insert
    @Autowired
    private MappingMongoConverter mappingMongoConverter;

    // remove _class
    @PostConstruct
    public void setUpMongoEscapeCharacterConversion() {
        mappingMongoConverter.setTypeMapper(new DefaultMongoTypeMapper(null));
    }

}
