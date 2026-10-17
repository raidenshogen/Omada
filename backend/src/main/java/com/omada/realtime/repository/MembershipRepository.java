package com.omada.realtime.repository; import com.omada.realtime.entity.Membership; import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MembershipRepository extends MongoRepository<Membership, UUID>{
    List<Membership> findByOrganizationId(String organizationId);
    List<Membership> findByUserId(String userId);
    java.util.Optional<Membership> findByOrganizationIdAndUserId(String organizationId, String userId);
    void deleteByOrganizationIdAndUserId(String organizationId, String userId);
    boolean existsByOrganizationIdAndUserId(String organizationId, String userId);
}