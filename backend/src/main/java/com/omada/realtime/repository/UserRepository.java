package com.omada.realtime.repository;
import com.omada.realtime.entity.User; import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public interface UserRepository extends MongoRepository<User, UUID>{ User findByEmail(String email); }