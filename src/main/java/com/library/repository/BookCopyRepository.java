package com.library.repository;

import com.library.entity.Book;
import com.library.entity.BookCopy;
import com.library.entity.Library;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {
    List<BookCopy> findByBookId(Long bookId);

    List<BookCopy> findByLibraryId(Long libraryId);

    @Query("SELECT bc FROM BookCopy bc WHERE bc.book.id = :bookId AND bc.status = 'ACTIVE'")
    List<BookCopy> findActiveByBookId(@Param("bookId") Long bookId);

    @Query("SELECT bc FROM BookCopy bc WHERE bc.book.id = :bookId AND bc.availableCopies > 0 AND bc.status = 'ACTIVE'")
    List<BookCopy> findAvailableByBookId(@Param("bookId") Long bookId);

    @Query("SELECT bc FROM BookCopy bc JOIN bc.library l WHERE bc.book.id IN :bookIds AND bc.status = :status AND l.active = :libraryActive")
    List<BookCopy> findByBookIdInAndStatusAndLibraryActive(@Param("bookIds") List<Long> bookIds,
            @Param("status") BookCopy.CopyStatus status,
            @Param("libraryActive") Boolean libraryActive);

    boolean existsByBookAndLibrary(Book book, Library library);

    Optional<BookCopy> findByBookAndLibrary(Book book, Library library);
}