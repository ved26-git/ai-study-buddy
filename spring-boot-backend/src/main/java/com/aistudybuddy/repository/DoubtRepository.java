package com.aistudybuddy.repository;

import com.aistudybuddy.entity.Doubt;
import com.aistudybuddy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoubtRepository extends JpaRepository<Doubt, Long> {
    List<Doubt> findByUserOrderByCreatedAtDesc(User user);
    Optional<Doubt> findByDoubtIdAndUser(Long doubtId, User user);
    long countByUser(User user);
    void deleteByUser(User user);
}
