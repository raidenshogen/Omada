package com.omada.realtime.repository; import com.omada.realtime.entity.Organization; import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrganizationRepository extends MongoRepository<Organization, UUID>{ Organization findBySlug(String slug); }