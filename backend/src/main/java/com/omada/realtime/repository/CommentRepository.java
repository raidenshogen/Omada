package com.omada.realtime.repository;
import com.omada.realtime.entity.Comment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends MongoRepository<Comment, UUID>{ List<Comment> findByCardId(UUID cardId); }