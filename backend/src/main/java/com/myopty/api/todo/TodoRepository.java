package com.myopty.api.todo;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {
    List<Todo> findAllByOrderByCreatedAtDesc();

    @Query("SELECT t FROM Todo t WHERE t.status = :status")
    List<Todo> findByStatus(@Param("status") Todo.Status status);
}