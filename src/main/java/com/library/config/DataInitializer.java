package com.library.config;

import com.library.entity.Library;
import com.library.repository.LibraryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    
    private final LibraryRepository libraryRepository;
    
    @Override
    public void run(String... args) {
        initializeLibraries();
    }
    
    private void initializeLibraries() {
        if (libraryRepository.count() == 0) {
            log.info("初始化圖書館資料...");
            
            Library centralLibrary = new Library();
            centralLibrary.setName("中央圖書館");
            centralLibrary.setAddress("台北市中正區重慶南路一段37號");
            centralLibrary.setPhone("02-2331-2475");
            centralLibrary.setActive(true);
            
            Library branchLibrary = new Library();
            branchLibrary.setName("分館圖書館");
            branchLibrary.setAddress("台北市信義區松智路17號");
            branchLibrary.setPhone("02-8789-7777");
            branchLibrary.setActive(true);
            
            Library eastLibrary = new Library();
            eastLibrary.setName("東區圖書館");
            eastLibrary.setAddress("台北市大安區敦化南路二段63號");
            eastLibrary.setPhone("02-2325-5820");
            eastLibrary.setActive(true);
            
            libraryRepository.save(centralLibrary);
            libraryRepository.save(branchLibrary);
            libraryRepository.save(eastLibrary);
            
            log.info("圖書館資料初始化完成：已創建 {} 個圖書館", libraryRepository.count());
        } else {
            log.info("圖書館資料已存在，跳過初始化");
        }
    }
}