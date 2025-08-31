package com.library.repository;

import com.library.entity.Book;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByTitleContainingIgnoreCase(String title);
    List<Book> findByAuthorContainingIgnoreCase(String author);
    List<Book> findByPublishYear(Integer publishYear);
    
    @Query("SELECT b FROM Book b WHERE " +
           "(:title IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
           "(:author IS NULL OR LOWER(b.author) LIKE LOWER(CONCAT('%', :author, '%'))) AND " +
           "(:year IS NULL OR b.publishYear = :year)")
    List<Book> searchBooks(@Param("title") String title, 
                          @Param("author") String author, 
                          @Param("year") Integer year);
    
    @Query("SELECT b FROM Book b WHERE " +
           "(:title IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
           "(:author IS NULL OR LOWER(b.author) LIKE LOWER(CONCAT('%', :author, '%'))) AND " +
           "(:year IS NULL OR b.publishYear = :year)")
    List<Book> searchBooks(@Param("title") String title, 
                          @Param("author") String author, 
                          @Param("year") Integer year, 
                          Pageable pageable);
    
    Optional<Book> findByTitleAndAuthorAndPublishYear(String title, String author, Integer publishYear);
}