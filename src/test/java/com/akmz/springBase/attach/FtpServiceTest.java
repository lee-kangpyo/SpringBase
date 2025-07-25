package com.akmz.springBase.attach;

import com.akmz.springBase.attach.service.FtpClientFactory;
import com.akmz.springBase.attach.service.FtpService;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FtpServiceTest {

    @InjectMocks
    private FtpService ftpService;

    @Mock
    private FTPClient mockFtpClient;

    @Mock
    private FtpClientFactory mockFtpClientFactory;

    @BeforeEach
    void setUp() throws IOException {
        // @Value 필드 주입 (ReflectionTestUtils 사용)
        ReflectionTestUtils.setField(ftpService, "host", "localhost");
        ReflectionTestUtils.setField(ftpService, "port", 21);
        ReflectionTestUtils.setField(ftpService, "username", "ftpUser");
        ReflectionTestUtils.setField(ftpService, "password", "ftpUser1!");
        
        // FtpClientFactory가 Mock FTPClient를 반환하도록 설정
        when(mockFtpClientFactory.createClient()).thenReturn(mockFtpClient);

        // connectAndLogin 내부에서 호출되는 메서드 Mocking
        doNothing().when(mockFtpClient).connect(anyString(), anyInt());
        when(mockFtpClient.getReplyCode()).thenReturn(FTPReply.COMMAND_OK);
        when(mockFtpClient.login(anyString(), anyString())).thenReturn(true);
        doNothing().when(mockFtpClient).enterLocalPassiveMode();
        when(mockFtpClient.setFileType(anyInt())).thenReturn(true);
        when(mockFtpClient.changeWorkingDirectory(anyString())).thenReturn(true);
        when(mockFtpClient.isConnected()).thenReturn(true);
    }

    @Test
    @DisplayName("파일 업로드 성공 테스트")
    void uploadFile_success() throws IOException {
        // Given
        String testContent = "test data";
        MockMultipartFile mockFile = new MockMultipartFile("file", "test_upload.txt", "text/plain", testContent.getBytes());
        String remotePath = "/temp_uploads/test_upload.txt"; // 전체 경로로 변경

        // Mock MultipartFile의 getInputStream()은 실제 구현을 사용하므로 Mocking 필요 없음
        when(mockFtpClient.storeFile(eq(remotePath), any(InputStream.class))).thenReturn(true);

        // When
        boolean result = ftpService.uploadFileToFtp(mockFile, remotePath);

        // Then
        assertTrue(result);
        verify(mockFtpClient, times(1)).connect(anyString(), anyInt());
        verify(mockFtpClient, times(1)).login(anyString(), anyString());
        verify(mockFtpClient, times(1)).storeFile(eq(remotePath), any(InputStream.class));
        verify(mockFtpClient, times(1)).logout();
        verify(mockFtpClient, times(1)).disconnect();
    }

    @Test
    @DisplayName("파일 업로드 실패 테스트 - storeFile 실패")
    void uploadFile_fail_storeFile() throws IOException {
        // Given
        String testContent = "test data";
        MockMultipartFile mockFile = new MockMultipartFile("file", "test_upload_fail.txt", "text/plain", testContent.getBytes());
        String remotePath = "/temp_uploads/test_upload_fail.txt"; // 전체 경로로 변경

        // Mock MultipartFile의 getInputStream()은 실제 구현을 사용하므로 Mocking 필요 없음
        when(mockFtpClient.storeFile(eq(remotePath), any(InputStream.class))).thenReturn(false);
        when(mockFtpClient.getReplyString()).thenReturn("550 File not found"); // 실패 응답 설정

        // When
        boolean result = ftpService.uploadFileToFtp(mockFile, remotePath);

        // Then
        assertFalse(result);
        verify(mockFtpClient, times(1)).storeFile(eq(remotePath), any(InputStream.class));
    }

    @Test
    @DisplayName("파일 다운로드 성공 테스트")
    void downloadFile_success() throws IOException {
        // Given
        OutputStream testOutputStream = new ByteArrayOutputStream();
        String remoteFileName = "test_download.txt";
        String remotePath = "/temp_uploads/test_download.txt"; // 전체 경로로 변경
        when(mockFtpClient.retrieveFile(eq(remotePath), any(OutputStream.class))).thenReturn(true);

        // When
        boolean result = ftpService.downloadFileFromFtp(remotePath, testOutputStream);

        // Then
        assertTrue(result);
        verify(mockFtpClient, times(1)).connect(anyString(), anyInt());
        verify(mockFtpClient, times(1)).login(anyString(), anyString());
        verify(mockFtpClient, times(1)).retrieveFile(eq(remotePath), any(OutputStream.class));
        verify(mockFtpClient, times(1)).logout();
        verify(mockFtpClient, times(1)).disconnect();
    }

    @Test
    @DisplayName("파일 다운로드 실패 테스트 - retrieveFile 실패")
    void downloadFile_fail_retrieveFile() throws IOException {
        // Given
        OutputStream testOutputStream = new ByteArrayOutputStream();
        String remoteFileName = "test_download_fail.txt";
        String remotePath = "/temp_uploads/test_download_fail.txt"; // 전체 경로로 변경
        when(mockFtpClient.retrieveFile(eq(remotePath), any(OutputStream.class))).thenReturn(false);
        when(mockFtpClient.getReplyString()).thenReturn("550 File not found"); // 실패 응답 설정

        // When
        boolean result = ftpService.downloadFileFromFtp(remotePath, testOutputStream);

        // Then
        assertFalse(result);
        verify(mockFtpClient, times(1)).retrieveFile(eq(remotePath), any(OutputStream.class));
    }

    @Test
    @DisplayName("FTP 연결 실패 테스트")
    @MockitoSettings(strictness = Strictness.LENIENT)
    void connect_fail() throws IOException {
        // Given
        MockMultipartFile mockFile = new MockMultipartFile("file", "test.txt", "text/plain", "data".getBytes());
        String remotePath = "/temp_uploads/file.txt"; // 전체 경로로 변경
        doThrow(new IOException("Connection refused")).when(mockFtpClient).connect(anyString(), anyInt());
        when(mockFtpClient.isConnected()).thenReturn(false);

        // When
        boolean result = ftpService.uploadFileToFtp(mockFile, remotePath);

        // Then
        assertFalse(result);
        verify(mockFtpClient, never()).login(anyString(), anyString());
    }

    @Test
    @DisplayName("FTP 로그인 실패 테스트")
    @MockitoSettings(strictness = Strictness.LENIENT)
    void login_fail() throws IOException {
        // Given
        MockMultipartFile mockFile = new MockMultipartFile("file", "test.txt", "text/plain", "data".getBytes());
        String remotePath = "/temp_uploads/file.txt"; // 전체 경로로 변경
        when(mockFtpClient.getReplyCode()).thenReturn(FTPReply.COMMAND_OK);
        when(mockFtpClient.login(anyString(), anyString())).thenReturn(false);
        when(mockFtpClient.isConnected()).thenReturn(false);

        // When
        boolean result = ftpService.uploadFileToFtp(mockFile, remotePath);

        // Then
        assertFalse(result);
        verify(mockFtpClient, times(1)).login(anyString(), anyString());
        verify(mockFtpClient, times(1)).disconnect();
    }
}