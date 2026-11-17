package com.aistudybuddy.repository;

import com.aistudybuddy.entity.StudyNotes;
import com.aistudybuddy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudyNotesRepository extends JpaRepository<StudyNotes, Long> {
    List<StudyNotes> findByUserOrderByCreatedAtDesc(User user);
    Optional<StudyNotes> findByNoteIdAndUser(Long noteId, User user);
    long countByUser(User user);
}
