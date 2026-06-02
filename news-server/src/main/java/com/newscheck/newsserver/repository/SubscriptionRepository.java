package com.newscheck.newsserver.repository;

import com.newscheck.newsserver.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByUserId(Long userId);

    Optional<Subscription> findByUserIdAndCategory(Long userId, String category);

    boolean existsByUserIdAndCategory(Long userId, String category);

    void deleteByUserIdAndCategory(Long userId, String category);
}
