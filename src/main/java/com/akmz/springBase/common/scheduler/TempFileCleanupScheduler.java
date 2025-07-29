// src/main/java/com/akmz/springBase/common/scheduler/TempFileCleanupScheduler.java

package com.akmz.springBase.common.scheduler;

import com.akmz.springBase.attach.service.FtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class TempFileCleanupScheduler {

    private final FtpService ftpService;
    @Value("${ftp.temp-uploads-dir:/temp-uploads}")
    private String tempUploadsDir;
    @Value("${ftp.temp-file-expiration-hours:1}")
    private int expirationHours;

    /**
     * 매시 정각에 실행되어 1시간 이상된 임시 파일을 삭제합니다.
     */
    @Scheduled(cron = "0 0 * * * ?") // 매시 정각에 실행
//    @Scheduled(cron = "0/30 * * * * ?") // 30초마다 실행(테스트 코드)
    public void cleanupOldTempFiles() {
        log.info("오래된 임시 파일 정리를 시작합니다...");
        long expirationTimeMs = (long) expirationHours * 60 * 60 * 1000; // 밀리초로 변환

        try {
            cleanupDirectory(tempUploadsDir, expirationTimeMs); // 계산된 만료 시간 전달
        } catch (IOException e) {
            log.error("임시 파일 정리 중 최상위 디렉토리 처리 실패", e);
        }
        log.info("오래된 임시 파일 정리를 완료했습니다.");
    }

    private void cleanupDirectory(String directoryPath, long expirationTimeMs) throws IOException {
        List<String> filePaths = ftpService.listFileNames(directoryPath);

        for (String filePath : filePaths) {
            try {
                Calendar fileTimestamp = ftpService.getFileTimestamp(filePath);
                if (fileTimestamp != null) {
                    long fileTime = fileTimestamp.getTimeInMillis();
                    long currentTime = System.currentTimeMillis();

                    if (currentTime - fileTime > expirationTimeMs) {
                        log.info("오래된 임시 파일을 삭제합니다: {}", filePath);
                        ftpService.deleteFileFromFtp(filePath);
                    }
                }
            } catch (IOException e) {
                log.error("임시 파일 처리 중 오류 발생: {}", filePath, e);
            }
        }
    }
}