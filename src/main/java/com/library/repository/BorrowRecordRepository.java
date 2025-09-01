package com.library.repository;

import com.library.entity.BorrowRecord;
import com.library.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {
    List<BorrowRecord> findByUserId(Long userId);
    List<BorrowRecord> findByUserIdAndStatus(Long userId, BorrowRecord.BorrowStatus status);
    
    @Query("SELECT COUNT(br) FROM BorrowRecord br WHERE br.user.id = :userId AND br.status = 'BORROWED' AND br.bookCopy.book.type = :bookType")
    long countCurrentBorrowsByUserAndBookType(@Param("userId") Long userId, @Param("bookType") Book.BookType bookType);
    
    @Query("SELECT br FROM BorrowRecord br WHERE br.status = 'BORROWED' AND br.dueDate BETWEEN :startDate AND :endDate")
    List<BorrowRecord> findDueSoon(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT br FROM BorrowRecord br WHERE br.status = 'BORROWED' AND br.dueDate < :currentDate")
    List<BorrowRecord> findOverdue(@Param("currentDate") LocalDate currentDate);
    
    @Query("SELECT br FROM BorrowRecord br WHERE br.user.id = :userId AND br.bookCopy.id = :bookCopyId AND br.status = 'BORROWED'")
    Optional<BorrowRecord> findActiveBorrowByUserAndBookCopy(@Param("userId") Long userId, @Param("bookCopyId") Long bookCopyId);
    
    @Query("SELECT br FROM BorrowRecord br WHERE br.user.id = :userId AND br.bookCopy.book.id = :bookId AND br.status = 'BORROWED'")
    Optional<BorrowRecord> findActiveBorrowByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);
    
    @Query("SELECT br FROM BorrowRecord br JOIN FETCH br.bookCopy bc JOIN FETCH bc.book b JOIN FETCH br.library l WHERE br.user.id = :userId ORDER BY br.borrowDate DESC")
    List<BorrowRecord> findByUserIdWithDetails(@Param("userId") Long userId);
    
    @Query("SELECT br FROM BorrowRecord br JOIN FETCH br.user u JOIN FETCH br.bookCopy bc JOIN FETCH bc.book b JOIN FETCH br.library l WHERE br.status = 'BORROWED' AND br.dueDate < :currentDate")
    List<BorrowRecord> findOverdueWithDetails(@Param("currentDate") LocalDate currentDate);
}