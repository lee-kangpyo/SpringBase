package com.akmz.springBase.interFace.service;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Service
@Slf4j
public class FtpService {

    @Value("${ftp.host}")
    private String host;

    @Value("${ftp.port}")
    private int port;

    @Value("${ftp.username}")
    private String username;

    @Value("${ftp.password}")
    private String password;

    @Value("${ftp.remote-base-dir:/}") // 기본값은 FTP 서버의 루트 디렉토리
    private String remoteBaseDir;

    private FTPClient ftpClient;

    public FtpService() {
        this.ftpClient = new FTPClient();
    }

    /**
     * FTP 서버에 연결하고 로그인합니다.
     * @return 연결 및 로그인 성공 여부
     */
    private boolean connectAndLogin() {
        try {
            ftpClient.setControlEncoding("EUC-KR"); // <-- 여기로 이동
            ftpClient.connect(host, port);
            int reply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftpClient.disconnect();
                log.error("FTP 서버 연결 실패: {}", reply);
                return false;
            }

            if (!ftpClient.login(username, password)) {
                ftpClient.disconnect();
                log.error("FTP 서버 로그인 실패: 사용자명 또는 비밀번호 오류");
                return false;
            }
            ftpClient.enterLocalPassiveMode(); // 패시브 모드 설정
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE); // 바이너리 파일 전송 모드 설정


            log.info("FTP 서버에 성공적으로 연결 및 로그인: {}:{}", host, port);
            return true;

        } catch (IOException e) {
            log.error("FTP 연결 또는 로그인 중 오류 발생: {}", e.getMessage());
            return false;
        }
    }

    /**
     * FTP 서버에서 연결을 해제합니다.
     */
    private void disconnect() {
        try {
            if (ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
                log.info("FTP 서버에서 연결 해제");
            }
        } catch (IOException e) {
            log.error("FTP 연결 해제 중 오류 발생: {}", e.getMessage());
        }
    }

    /**
     * 파일을 FTP 서버에 업로드합니다.
     * @param file 업로드할 MultipartFile 객체
     * @param remotePath 원격 서버에 저장될 파일의 전체 경로 (디렉토리 포함)
     * @return 업로드 성공 여부
     */
    public boolean uploadFile(MultipartFile file, String remotePath) {
        if (!connectAndLogin()) {
            return false;
        }

        boolean success = false;
        try (InputStream inputStream = file.getInputStream()) {
            // 원격 파일 경로에서 디렉토리 부분 추출
            String remoteDir = remotePath.substring(0, remotePath.lastIndexOf('/'));

            // 디렉토리가 존재하지 않으면 생성
            if (!doesDirectoryExist(remoteDir)) {
                if (!makeDirectory(remoteDir)) {
                    log.error("원격 디렉토리 '{}' 생성 실패.", remoteDir);
                    return false;
                }
            }

            // 파일 업로드
            success = ftpClient.storeFile(remotePath, inputStream);
            if (success) {
                log.info("파일 '{}'가 FTP 서버에 성공적으로 업로드되었습니다.", remotePath);
            } else {
                log.error("파일 '{}' 업로드 실패. FTP 응답: {}", remotePath, ftpClient.getReplyString());
            }
        } catch (IOException e) {
            log.error("파일 업로드 중 오류 발생: {}", e.getMessage());
        } finally {
            disconnect();
        }
        return success;
    }

    /**
     * FTP 서버에서 파일을 다운로드합니다.
     * @param remotePath 원격 서버의 파일 경로 (예: "/path/to/file.txt")
     * @param outputStream 파일을 저장할 출력 스트림
     * @return 다운로드 성공 여부
     */
    public boolean downloadFile(String remotePath, OutputStream outputStream) {
        if (!connectAndLogin()) {
            return false;
        }

        boolean success = false;
        try {
            success = ftpClient.retrieveFile(remotePath, outputStream);
            if (success) {
                log.info("파일 '{}'가 FTP 서버에서 성공적으로 다운로드되었습니다.", remotePath);
            } else {
                log.error("파일 '{}' 다운로드 실패. FTP 응답: {}", remotePath, ftpClient.getReplyString());
            }
        } catch (IOException e) {
            log.error("파일 다운로드 중 오류 발생: {}", e.getMessage());
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                log.error("출력 스트림 닫기 중 오류 발생: {}", e.getMessage());
            }
            disconnect();
        }
        return success;
    }

    /**
     * FTP 서버에 특정 디렉토리가 존재하는지 확인합니다.
     * @param directoryPath 확인할 디렉토리 경로
     * @return 디렉토리 존재 여부
     */
    public boolean doesDirectoryExist(String directoryPath) throws IOException {
        String currentWorkingDirectory = ftpClient.printWorkingDirectory();
        boolean exists = ftpClient.changeWorkingDirectory(directoryPath);
        ftpClient.changeWorkingDirectory(currentWorkingDirectory); // 원래 디렉토리로 돌아옴
        return exists;
    }

    /**
     * FTP 서버에 디렉토리를 생성합니다.
     * @param directoryPath 생성할 디렉토리 경로
     * @return 생성 성공 여부
     */
    public boolean makeDirectory(String directoryPath) throws IOException {
        return ftpClient.makeDirectory(directoryPath);
    }
}