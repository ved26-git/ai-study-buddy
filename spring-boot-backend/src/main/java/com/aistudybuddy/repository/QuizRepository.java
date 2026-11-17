package com.aistudybuddy.repository;

import com.aistudybuddy.entity.Quiz;
import com.aistudybuddy.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findByUserOrderByCreatedAtDesc(User user);
    Optional<Quiz> findByQuizIdAndUser(Long quizId, User user);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM Quiz q WHERE q.quizId = :quizId AND q.user = :user")
    Optional<Quiz> findByQuizIdAndUserWithLock(@Param("quizId") Long quizId, @Param("user") User user);

    long countByUser(User user);
    long countByUserAndScoreIsNotNull(User user);
}
