package com.akmz.springBase.inter.fax;

import com.akmz.springBase.interFace.service.FtpService;
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
class FtpServiceTest {

    @InjectMocks
    private FtpService ftpService;

    @Mock
    private FTPClient mockFtpClient;

    @BeforeEach
    void setUp() throws IOException {
        // MockitoAnnotations.openMocks(this); // @ExtendWith(MockitoExtension.class) 사용 시 필요 없음

        // @Value 필드 주입 (ReflectionTestUtils 사용)
        ReflectionTestUtils.setField(ftpService, "host", "localhost");
        ReflectionTestUtils.setField(ftpService, "port", 21);
        ReflectionTestUtils.setField(ftpService, "username", "ftpUser");
        ReflectionTestUtils.setField(ftpService, "password", "ftpUser1!");
        ReflectionTestUtils.setField(ftpService, "remoteBaseDir", "/FTP/");

        // FtpService 내부의 ftpClient 필드를 Mock 객체로 설정
        ReflectionTestUtils.setField(ftpService, "ftpClient", mockFtpClient);

        // connectAndLogin 내부에서 호출되는 메서드 Mocking
        doNothing().when(mockFtpClient).connect(anyString(), anyInt()); // void 메서드이므로 doNothing() 사용
        when(mockFtpClient.getReplyCode()).thenReturn(FTPReply.COMMAND_OK); // 200 OK
        when(mockFtpClient.login(anyString(), anyString())).thenReturn(true);
        doNothing().when(mockFtpClient).enterLocalPassiveMode();
        when(mockFtpClient.setFileType(anyInt())).thenReturn(true); // setFileType은 boolean을 반환하므로 thenReturn(true) 사용
        when(mockFtpClient.changeWorkingDirectory(anyString())).thenReturn(true); // 디렉토리 변경 성공 가정
        when(mockFtpClient.isConnected()).thenReturn(true); // 연결되어 있다고 가정하여 logout/disconnect 호출 가능하게 함
    }

    @Test
    @DisplayName("파일 업로드 성공 테스트")
    void uploadFile_success() throws IOException {
        // Given
        InputStream testInputStream = new ByteArrayInputStream("test data".getBytes());
        String remoteFileName = "test_upload.txt";
        when(mockFtpClient.storeFile(eq(remoteFileName), any(InputStream.class))).thenReturn(true);

        // When
        boolean result = ftpService.uploadFile(testInputStream, remoteFileName);

        // Then
        assertTrue(result);
        verify(mockFtpClient, times(1)).connect(anyString(), anyInt());
        verify(mockFtpClient, times(1)).login(anyString(), anyString());
        verify(mockFtpClient, times(1)).storeFile(eq(remoteFileName), any(InputStream.class));
        verify(mockFtpClient, times(1)).logout();
        verify(mockFtpClient, times(1)).disconnect();
    }

    @Test
    @DisplayName("파일 업로드 실패 테스트 - storeFile 실패")
    void uploadFile_fail_storeFile() throws IOException {
        // Given
        InputStream testInputStream = new ByteArrayInputStream("test data".getBytes());
        String remoteFileName = "test_upload_fail.txt";
        when(mockFtpClient.storeFile(eq(remoteFileName), any(InputStream.class))).thenReturn(false);
        when(mockFtpClient.getReplyString()).thenReturn("550 File not found"); // 실패 응답 설정

        // When
        boolean result = ftpService.uploadFile(testInputStream, remoteFileName);

        // Then
        assertFalse(result);
        verify(mockFtpClient, times(1)).storeFile(eq(remoteFileName), any(InputStream.class));
    }

    @Test
    @DisplayName("파일 다운로드 성공 테스트")
    void downloadFile_success() throws IOException {
        // Given
        OutputStream testOutputStream = new ByteArrayOutputStream();
        String remoteFileName = "test_download.txt";
        when(mockFtpClient.retrieveFile(eq(remoteFileName), any(OutputStream.class))).thenReturn(true);

        // When
        boolean result = ftpService.downloadFile(remoteFileName, testOutputStream);

        // Then
        assertTrue(result);
        verify(mockFtpClient, times(1)).connect(anyString(), anyInt());
        verify(mockFtpClient, times(1)).login(anyString(), anyString());
        verify(mockFtpClient, times(1)).retrieveFile(eq(remoteFileName), any(OutputStream.class));
        verify(mockFtpClient, times(1)).logout();
        verify(mockFtpClient, times(1)).disconnect();
    }

    @Test
    @DisplayName("파일 다운로드 실패 테스트 - retrieveFile 실패")
    void downloadFile_fail_retrieveFile() throws IOException {
        // Given
        OutputStream testOutputStream = new ByteArrayOutputStream();
        String remoteFileName = "test_download_fail.txt";
        when(mockFtpClient.retrieveFile(eq(remoteFileName), any(OutputStream.class))).thenReturn(false);
        when(mockFtpClient.getReplyString()).thenReturn("550 File not found"); // 실패 응답 설정

        // When
        boolean result = ftpService.downloadFile(remoteFileName, testOutputStream);

        // Then
        assertFalse(result);
        verify(mockFtpClient, times(1)).retrieveFile(eq(remoteFileName), any(OutputStream.class));
    }

    @Test
    @DisplayName("FTP 연결 실패 테스트")
    @MockitoSettings(strictness = Strictness.LENIENT)
    void connect_fail() throws IOException {
        // Given
        doThrow(new IOException("Connection refused")).when(mockFtpClient).connect(anyString(), anyInt());
        when(mockFtpClient.isConnected()).thenReturn(false); // 연결 실패 시 isConnected는 false

        // When
        boolean result = ftpService.uploadFile(new ByteArrayInputStream("data".getBytes()), "file.txt");

        // Then
        assertFalse(result);
        verify(mockFtpClient, never()).login(anyString(), anyString()); // 연결 실패했으므로 로그인 시도 안 함
    }

    @Test
    @DisplayName("FTP 로그인 실패 테스트")
    @MockitoSettings(strictness = Strictness.LENIENT)
    void login_fail() throws IOException {
        // Given
        when(mockFtpClient.getReplyCode()).thenReturn(FTPReply.COMMAND_OK);
        when(mockFtpClient.login(anyString(), anyString())).thenReturn(false); // 로그인 실패 가정
        when(mockFtpClient.isConnected()).thenReturn(false); // 로그인 실패 시 isConnected는 false

        // When
        boolean result = ftpService.uploadFile(new ByteArrayInputStream("data".getBytes()), "file.txt");

        // Then
        assertFalse(result);
        verify(mockFtpClient, times(1)).login(anyString(), anyString());
        verify(mockFtpClient, times(1)).disconnect(); // 로그인 실패 후 disconnect 호출
    }
}
