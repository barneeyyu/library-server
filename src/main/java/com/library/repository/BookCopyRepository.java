package com.library.repository;

import com.library.entity.BookCopy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {
    List<BookCopy> findByBookId(Long bookId);
    List<BookCopy> findByLibraryId(Long libraryId);
    
    @Query("SELECT bc FROM BookCopy bc WHERE bc.book.id = :bookId AND bc.status = 'ACTIVE'")
    List<BookCopy> findActiveByBookId(@Param("bookId") Long bookId);
    
    @Query("SELECT bc FROM BookCopy bc WHERE bc.book.id = :bookId AND bc.availableCopies > 0 AND bc.status = 'ACTIVE'")
    List<BookCopy> findAvailableByBookId(@Param("bookId") Long bookId);
}