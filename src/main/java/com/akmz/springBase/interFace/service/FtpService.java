package com.akmz.springBase.interFace.service;

import com.akmz.springBase.common.util.PageUtils;
import com.akmz.springBase.interFace.mapper.AttachMapper;
import com.akmz.springBase.interFace.model.dto.AttachFileResponse;
import com.akmz.springBase.interFace.model.dto.AttachResponse;
import com.akmz.springBase.interFace.model.entity.Attach;
import com.akmz.springBase.interFace.model.entity.AttachFile;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class FtpService {

    @Value("${ftp.host}")
    private String host;

    @Value("${ftp.port}")
    private int port;

    @Value("${ftp.username}")
    private String username;

    @Value("${ftp.password}")
    private String password;

    private final AttachMapper attachMapper; // DB 연동을 위한 매퍼

    // ===================================================================================
    // High-level 비즈니스 로직 (DB 연동 포함)
    // ===================================================================================

    /**
     * 새로운 첨부파일들을 업로드하고 데이터베이스에 정보를 기록합니다.
     * 이 메서드는 트랜잭션으로 관리됩니다.
     *
     * @param files 업로드할 파일 목록
     * @param attachName 생성할 첨부 묶음의 이름
     * @param creatorId 첨부 생성자 ID
     * @return 생성된 첨부(Attach) 정보
     * @throws IOException 파일 처리 중 오류 발생 시
     */
    @Transactional
    public Attach uploadNewAttachment(List<MultipartFile> files, String attachName, String creatorId) throws IOException {
        // 1. 첨부 묶음(Attach) 정보 생성
        Attach attach = new Attach();
        attach.setAttachName(attachName);
        attach.setCreatorId(creatorId);
        attachMapper.insertAttach(attach);

        // 2. 각 파일을 FTP에 업로드하고 DB에 파일 정보 저장
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            // 2-1. 저장할 경로 및 파일명 생성 (예: /uploads/2025/07/22/uuid_filename.ext)
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String remoteDirPath = "/uploads/" + datePath; // Paths.get() 대신 문자열 연결
            String originalFileName = file.getOriginalFilename();
            String savedFileName = UUID.randomUUID().toString() + "_" + originalFileName;
            String remoteFullPath = remoteDirPath + "/" + savedFileName; // Paths.get() 대신 문자열 연결

            // 2-2. FTP 서버에 파일 업로드 (Low-level 메서드 호출)
            boolean uploadSuccess = uploadFileToFtp(file, remoteFullPath);

            if (uploadSuccess) {
                // 2-3. DB에 파일 메타데이터 저장
                AttachFile attachFile = new AttachFile();
                attachFile.setAttachId(attach.getAttachId());
                attachFile.setOriginalFileName(originalFileName);
                attachFile.setSavedFileName(savedFileName);
                attachFile.setFilePath(remoteDirPath);
                attachFile.setFileSize(file.getSize());
                attachFile.setUploaderId(creatorId);
                attachMapper.insertAttachFile(attachFile);
            } else {
                // 업로드 실패 시 예외 발생시켜 트랜잭션 롤백
                throw new IOException("FTP 파일 업로드 실패: " + originalFileName);
            }
        }
        return attach;
    }

    /**
     * 특정 첨부파일을 논리적으로 삭제하고, FTP 서버에서도 물리적으로 삭제합니다.
     * 이 메서드는 트랜잭션으로 관리됩니다.
     *
     * @param fileId 삭제할 파일의 ID
     * @return 삭제 성공 여부
     */
    @Transactional
    public boolean deleteAttachmentFile(long fileId) {
        // 1. DB에서 파일 정보 조회
        AttachFile fileToDelete = attachMapper.findFileById(fileId);
        if (fileToDelete == null || !"AVAILABLE".equals(fileToDelete.getStatus())) {
            log.warn("삭제할 파일이 존재하지 않거나 이미 삭제된 파일입니다. fileId: {}", fileId);
            return false;
        }

        // 2. FTP 서버에서 물리적 파일 삭제 (Low-level 메서드 호출)
        String remoteFullPath = fileToDelete.getFilePath() + "/" + fileToDelete.getSavedFileName();
        boolean deleteSuccess = deleteFileFromFtp(remoteFullPath);

        if (deleteSuccess) {
            // 3. DB에서 논리적 삭제 처리
            attachMapper.softDeleteFileById(fileId);
            log.info("파일이 성공적으로 삭제되었습니다. fileId: {}", fileId);
            return true;
        } else {
            log.error("FTP 파일 삭제에 실패하여 DB 상태를 변경하지 않았습니다. fileId: {}", fileId);
            // FTP 삭제 실패 시 롤백을 위해 예외를 던질 수도 있음
            // throw new RuntimeException("FTP file deletion failed.");
            return false;
        }
    }

    /**
     * 특정 첨부 ID에 속한 모든 파일 목록을 조회합니다. (삭제된 파일 제외)
     *
     * @param attachId 조회할 첨부 ID
     * @return 파일 목록
     */
    public List<AttachFile> getFilesByAttachId(Long attachId) {
        return attachMapper.findFilesByAttachId(attachId);
    }

    /**
     * 특정 첨부 ID에 속한 모든 첨부파일을 논리적으로 삭제하고, FTP 서버에서도 물리적으로 삭제합니다.
     * 이 메서드는 트랜잭션으로 관리됩니다.
     *
     * @param attachId 삭제할 첨부 묶음의 ID
     * @return 모든 파일 삭제 성공 여부 (하나라도 실패하면 false)
     */
    @Transactional
    public boolean deleteAttachmentByAttachId(Long attachId) {
        // 1. 해당 attachId에 속한 모든 파일 정보 조회 (AVAILABLE 상태만)
        List<AttachFile> filesToDelete = attachMapper.findFilesByAttachId(attachId);

        if (filesToDelete.isEmpty()) {
            log.warn("삭제할 파일이 존재하지 않습니다. attachId: {}", attachId);
            return true; // 삭제할 파일이 없으면 성공으로 간주
        }

        boolean allSuccess = true;
        for (AttachFile file : filesToDelete) {
            // 2. FTP 서버에서 물리적 파일 삭제
            String remoteFullPath = file.getFilePath() + "/" + file.getSavedFileName();
            boolean deleteFtpSuccess = deleteFileFromFtp(remoteFullPath);

            if (deleteFtpSuccess) {
                // 3. DB에서 논리적 삭제 처리
                attachMapper.softDeleteFileById(file.getFileId());
                log.info("파일이 성공적으로 삭제되었습니다. fileId: {}", file.getFileId());
            } else {
                log.error("FTP 파일 삭제에 실패했습니다. fileId: {}, path: {}", file.getFileId(), remoteFullPath);
                allSuccess = false; // 하나라도 실패하면 전체 실패
                // 이 경우 트랜잭션은 롤백되지 않고, 실패한 파일만 DB 상태가 변경되지 않음.
                // 필요에 따라 RuntimeException을 던져 전체 롤백 가능.
            }
        }

        // 모든 파일이 성공적으로 삭제되었을 경우에만 attach 묶음도 논리적으로 삭제 (선택 사항)
        // attachMapper.softDeleteAttach(attachId); // ATTACH 테이블에 STATUS 컬럼이 있다면

        return allSuccess;
    }

    /**
     * 모든 첨부파일 목록을 페이징하여 조회합니다. (삭제된 파일 제외)
     *
     * @param pageable 페이징 정보 (페이지 번호, 페이지 크기, 정렬)
     * @return 페이징 처리된 AttachFileResponse 목록
     */
    public Page<AttachFileResponse> getAllAttachFiles(Pageable pageable) {
        List<AttachFile> files = attachMapper.findAllAttachFiles();
        return PageUtils.convertPage(files, pageable, AttachFileResponse.class);
    }

    /**
     * 모든 첨부 묶음 목록을 페이징하여 조회합니다.
     *
     * @param pageable 페이징 정보 (페이지 번호, 페이지 크기, 정렬)
     * @return 페이징 처리된 AttachResponse 목록
     */
    public Page<AttachResponse> getAllAttaches(Pageable pageable) {
        List<Attach> attaches = attachMapper.findAllAttaches();
        return PageUtils.convertPage(attaches, pageable, AttachResponse.class);
    }

    /**
     * [High-level] 파일 ID를 이용하여 파일을 다운로드합니다.
     *
     * @param fileId 다운로드할 파일의 ID
     * @param outputStream 파일 내용을 쓸 출력 스트림
     * @return 다운로드된 파일의 메타데이터 (원본 파일명, 파일 크기) 또는 null
     * @throws IOException 파일 처리 중 오류 발생 시
     */
    public AttachFile downloadFile(Long fileId, OutputStream outputStream) throws IOException {
        AttachFile fileToDownload = attachMapper.findFileById(fileId);
        if (fileToDownload == null || !"AVAILABLE".equals(fileToDownload.getStatus())) {
            log.warn("다운로드할 파일이 존재하지 않거나 유효하지 않습니다. fileId: {}", fileId);
            return null;
        }

        String remoteFullPath = fileToDownload.getFilePath() + "/" + fileToDownload.getSavedFileName();
        boolean success = downloadFileFromFtp(remoteFullPath, outputStream);

        if (success) {
            log.info("파일 다운로드 성공: fileId={}", fileId);
            return fileToDownload;
        } else {
            log.error("FTP에서 파일 다운로드 실패: fileId={}", fileId);
            throw new IOException("FTP에서 파일 다운로드 실패: " + fileToDownload.getOriginalFileName());
        }
    }

    /**
     * [High-level] 첨부 묶음 ID를 이용하여 해당 묶음의 모든 파일을 ZIP으로 압축하여 다운로드합니다.
     *
     * @param attachId 다운로드할 첨부 묶음의 ID
     * @param outputStream ZIP 파일 내용을 쓸 출력 스트림
     * @throws IOException 파일 처리 중 오류 발생 시
     */
    public void downloadAttachBundle(Long attachId, OutputStream outputStream) throws IOException {
        List<AttachFile> files = attachMapper.findFilesByAttachId(attachId);
        if (files.isEmpty()) {
            log.warn("다운로드할 첨부 묶음이 비어있습니다. attachId: {}", attachId);
            return; // 또는 예외 처리
        }

        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
            for (AttachFile file : files) {
                String remoteFullPath = file.getFilePath() + "/" + file.getSavedFileName();
                ZipEntry zipEntry = new ZipEntry(file.getOriginalFileName());
                zos.putNextEntry(zipEntry);

                // FTP에서 파일 읽어와 ZIP 스트림에 쓰기
                FTPClient ftpClient = new FTPClient();
                if (!connectAndLogin(ftpClient)) {
                    log.error("FTP 연결 실패: 파일 다운로드 중");
                    throw new IOException("FTP 연결 실패");
                }
                try (InputStream ftpInputStream = ftpClient.retrieveFileStream(remoteFullPath)) {
                    if (ftpInputStream == null) {
                        log.error("FTP에서 파일 스트림을 가져오지 못했습니다: {}", remoteFullPath);
                        throw new IOException("FTP에서 파일 스트림을 가져오지 못했습니다: " + file.getOriginalFileName());
                    }
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = ftpInputStream.read(buffer)) != -1) {
                        zos.write(buffer, 0, bytesRead);
                    }
                    if (!ftpClient.completePendingCommand()) {
                        log.error("FTP completePendingCommand 실패: {}", ftpClient.getReplyString());
                        throw new IOException("FTP completePendingCommand 실패");
                    }
                } finally {
                    disconnect(ftpClient);
                }
                zos.closeEntry();
            }
        } catch (IOException e) {
            log.error("첨부 묶음 다운로드 중 오류 발생: attachId={}", attachId, e);
            throw e;
        }
    }


    // ===================================================================================
    // Low-level 순수 FTP 처리 메서드 (DB 연동 없음)
    // ===================================================================================

    /**
     * [Low-level] 파일을 FTP 서버에 업로드합니다. (DB 기록 없음)
     *
     * @param file 업로드할 MultipartFile 객체
     * @param remoteFullPath 원격 서버에 저장될 파일의 전체 경로 (예: /uploads/2025/07/22/file.txt)
     * @return 업로드 성공 여부
     */
    public boolean uploadFileToFtp(MultipartFile file, String remoteFullPath) {
        FTPClient ftpClient = new FTPClient();
        if (!connectAndLogin(ftpClient)) return false;

        boolean success = false;
        try (InputStream inputStream = file.getInputStream()) {
            String remoteDir = remoteFullPath.substring(0, remoteFullPath.lastIndexOf('/'));

            if (!changeOrMakeDirectory(ftpClient, remoteDir)) {
                return false; // 디렉토리 생성/변경 실패
            }

            success = ftpClient.storeFile(remoteFullPath, inputStream);
            if (success) {
                log.info("파일 '{}'가 FTP 서버에 성공적으로 업로드되었습니다.", remoteFullPath);
            } else {
                log.error("파일 '{}' 업로드 실패. FTP 응답: {}", remoteFullPath, ftpClient.getReplyString());
            }
        } catch (IOException e) {
            log.error("파일 업로드 중 오류 발생: {}", e.getMessage(), e);
        } finally {
            disconnect(ftpClient);
        }
        return success;
    }

    /**
     * [Low-level] FTP 서버에서 파일을 다운로드합니다. (DB 기록 없음)
     *
     * @param remoteFullPath 원격 서버의 파일 전체 경로 (예: /path/to/file.txt)
     * @param outputStream 파일을 저장할 출력 스트림
     * @return 다운로드 성공 여부
     */
    public boolean downloadFileFromFtp(String remoteFullPath, OutputStream outputStream) {
        FTPClient ftpClient = new FTPClient();
        if (!connectAndLogin(ftpClient)) return false;

        boolean success = false;
        try {
            success = ftpClient.retrieveFile(remoteFullPath, outputStream);
            if (success) {
                log.info("파일 '{}'가 FTP 서버에서 성공적으로 다운로드되었습니다.", remoteFullPath);
            } else {
                log.error("파일 '{}' 다운로드 실패. FTP 응답: {}", remoteFullPath, ftpClient.getReplyString());
            }
        } catch (IOException e) {
            log.error("파일 다운로드 중 오류 발생: {}", e.getMessage(), e);
        } finally {
            disconnect(ftpClient);
        }
        return success;
    }

    /**
     * [Low-level] FTP 서버에서 파일을 물리적으로 삭제합니다. (DB 기록 없음)
     *
     * @param remoteFullPath 삭제할 원격 파일의 전체 경로
     * @return 삭제 성공 여부
     */
    public boolean deleteFileFromFtp(String remoteFullPath) {
        FTPClient ftpClient = new FTPClient();
        if (!connectAndLogin(ftpClient)) return false;

        boolean success = false;
        try {
            success = ftpClient.deleteFile(remoteFullPath);
            if (success) {
                log.info("FTP 파일 '{}'가 성공적으로 삭제되었습니다.", remoteFullPath);
            } else {
                log.warn("FTP 파일 '{}' 삭제 실패. 파일이 없거나 권한 문제일 수 있습니다. 응답: {}", remoteFullPath, ftpClient.getReplyString());
                // 파일이 없는 경우(550)도 성공으로 간주할 수 있음
                if (ftpClient.getReplyCode() == 550) return true;
            }
        } catch (IOException e) {
            log.error("FTP 파일 삭제 중 오류 발생: {}", e.getMessage(), e);
        } finally {
            disconnect(ftpClient);
        }
        return success;
    }


    // ===================================================================================
    // Private FTP Helper 메서드
    // ===================================================================================

    private boolean connectAndLogin(FTPClient ftpClient) {
        try {
            ftpClient.setControlEncoding("EUC-KR");
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
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            log.info("FTP 서버에 성공적으로 연결 및 로그인: {}:{}", host, port);
            return true;
        } catch (IOException e) {
            log.error("FTP 연결 또는 로그인 중 오류 발생: {}", e.getMessage(), e);
            return false;
        }
    }

    private void disconnect(FTPClient ftpClient) {
        try {
            if (ftpClient != null && ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
            }
        } catch (IOException e) {
            log.error("FTP 연결 해제 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    private boolean changeOrMakeDirectory(FTPClient ftpClient, String remoteDirPath) throws IOException {
        // 최종 디렉토리가 이미 존재하는지 확인
        if (doesDirectoryExist(ftpClient, remoteDirPath)) {
            return true;
        }

        // 경로를 세그먼트로 분할
        String[] pathSegments = remoteDirPath.split("/");

        // 절대 경로인 경우 루트부터 시작
        String currentPath = "";
        if (remoteDirPath.startsWith("/")) {
            currentPath = "/";
        }

        for (String segment : pathSegments) {
            if (segment.isEmpty()) {
                continue; // 분할로 인해 발생하는 빈 문자열 건너뛰기 (예: 선행 /)
            }

            // 현재 경로 세그먼트 추가
            if (currentPath.equals("/")) { // currentPath가 루트인 경우 직접 추가
                currentPath += segment;
            } else if (currentPath.isEmpty()) { // currentPath가 비어있는 경우 (상대 경로 시작)
                currentPath = segment;
            } else { // 슬래시와 함께 추가
                currentPath += "/" + segment;
            }

            // 디렉토리로 이동하거나 생성 시도
            if (!doesDirectoryExist(ftpClient, currentPath)) {
                if (!ftpClient.makeDirectory(currentPath)) {
                    log.error("원격 디렉토리 '{}' 생성 실패.", currentPath);
                    return false;
                }
                log.info("원격 디렉토리 '{}' 생성 성공.", currentPath);
            }
        }
        // 최종적으로 대상 디렉토리로 이동
        return ftpClient.changeWorkingDirectory(remoteDirPath);
    }

    private boolean doesDirectoryExist(FTPClient ftpClient, String path) throws IOException {
        String current = ftpClient.printWorkingDirectory();
        try {
            return ftpClient.changeWorkingDirectory(path);
        } finally {
            ftpClient.changeWorkingDirectory(current);
        }
    }
}