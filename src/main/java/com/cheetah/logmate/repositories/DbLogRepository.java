package com.cheetah.logmate.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.cheetah.logmate.dto.DbAudit;

public interface DbLogRepository extends MongoRepository<DbAudit, String> {

}
