package com.newscheck.newsserver.repository;

import com.newscheck.newsserver.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /**
     * Find all users subscribed to a given category and who have an FCM token.
     * Used by the notification service.
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.subscriptions s " +
           "WHERE s.category = :category AND u.fcmToken IS NOT NULL AND u.fcmToken <> ''")
    List<User> findSubscribersWithFcmToken(@Param("category") String category);
}
