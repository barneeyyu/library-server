package com.library.repository;

import com.library.entity.Library;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LibraryRepository extends JpaRepository<Library, Long> {
    List<Library> findByActiveTrue();

    List<Library> findByNameContainingIgnoreCase(String name);
}