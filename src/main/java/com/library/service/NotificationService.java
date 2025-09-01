package com.library.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    @Autowired
    private BorrowService borrowService;

    /**
     * 每天上午 9:00 自動發送到期通知
     * cron表達式：秒 分 時 日 月 週
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void scheduledDueNotifications() {
        System.out.println("=== 定時任務：開始發送到期通知 ===");
        borrowService.sendDueNotifications();
        System.out.println("=== 定時任務：到期通知發送完成 ===");
    }

    /**
     * 每小時檢查一次逾期書籍 (用於測試，實際可調整頻率)
     */
    @Scheduled(fixedDelay = 3600000) // 每小時執行一次
    public void checkOverdueBooks() {
        // 這裡可以實作逾期書籍的處理邏輯
        // 例如：自動更新狀態、發送逾期通知等
        System.out.println("定時檢查逾期書籍狀態...");
    }
}