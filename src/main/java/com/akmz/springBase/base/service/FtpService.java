package com.akmz.springBase.base.service;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

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
            }      } catch (IOException e) {
            log.error("FTP 연결 해제 중 오류 발생: {}", e.getMessage());
        }
    }

    /**
     * 파일을 FTP 서버에 업로드합니다.
     * @param localFilePath 로컬 파일 경로 (예: "C:/temp/my_file.txt")
     * @param remoteFileName 원격 서버에 저장될 파일 이름 (예: "uploaded_file.txt")
     * @return 업로드 성공 여부
     */
    public boolean uploadFile(InputStream inputStream, String remoteFileName) {
        if (!connectAndLogin()) {
            return false;
        }

        boolean success = false;
        try {
            // 원격 기본 디렉토리로 이동 (필요하다면)
            if (!"/".equals(remoteBaseDir) && !ftpClient.changeWorkingDirectory(remoteBaseDir)) {
                log.warn("원격 기본 디렉토리 '{}'로 이동할 수 없습니다. 루트 디렉토리에 업로드 시도.", remoteBaseDir);
                // 그래도 업로드를 시도하기 위해 changeWorkingDirectory 실패를 에러로 처리하지 않음
            }

            success = ftpClient.storeFile(remoteFileName, inputStream);
            if (success) {
                log.info("파일 '{}'가 FTP 서버에 성공적으로 업로드되었습니다.", remoteFileName);
            } else {
                log.error("파일 '{}' 업로드 실패. FTP 응답: {}", remoteFileName, ftpClient.getReplyString());
            }
        } catch (IOException e) {
            log.error("파일 업로드 중 오류 발생: {}", e.getMessage());
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                log.error("입력 스트림 닫기 중 오류 발생: {}", e.getMessage());
            }
            disconnect();
        }
        return success;
    }

    /**
     * FTP 서버에서 파일을 다운로드합니다.
     * @param remoteFileName 원격 서버의 파일 이름 (예: "downloaded_file.txt")
     * @param outputStream 파일을 저장할 출력 스트림
     * @return 다운로드 성공 여부
     */
    public boolean downloadFile(String remoteFileName, OutputStream outputStream) {
        if (!connectAndLogin()) {
            return false;
        }

        boolean success = false;
        try {
            // 원격 기본 디렉토리로 이동 (필요하다면)
            if (!"/".equals(remoteBaseDir) && !ftpClient.changeWorkingDirectory(remoteBaseDir)) {
                log.warn("원격 기본 디렉토리 '{}'로 이동할 수 없습니다. 루트 디렉토리에서 다운로드 시도.", remoteBaseDir);
            }

            success = ftpClient.retrieveFile(remoteFileName, outputStream);
            if (success) {
                log.info("파일 '{}'가 FTP 서버에서 성공적으로 다운로드되었습니다.", remoteFileName);
            } else {
                log.error("파일 '{}' 다운로드 실패. FTP 응답: {}", remoteFileName, ftpClient.getReplyString());
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
}
