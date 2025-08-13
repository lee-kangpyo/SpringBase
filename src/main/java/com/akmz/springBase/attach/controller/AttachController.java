package com.akmz.springBase.attach.controller;

import com.akmz.springBase.attach.model.dto.AttachFileResponse;
import com.akmz.springBase.attach.model.dto.AttachResponse;
import com.akmz.springBase.attach.model.entity.Attach;
import com.akmz.springBase.attach.model.entity.AttachFile;
import com.akmz.springBase.attach.service.FtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

import org.springdoc.core.annotations.ParameterObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/attach")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Attachment API", description = "첨부파일 관리 API")
public class AttachController {

    private final FtpService ftpService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "[POST] 파일 업로드 및 첨부 생성", description = "여러 파일을 업로드하여 새로운 첨부 묶음을 생성.")
    public ResponseEntity<?> uploadAttachment(
            @Parameter(description = "업로드할 파일들", required = true) @RequestParam("files") List<MultipartFile> files,
            @Parameter(description = "생성할 첨부의 이름 (예: 1번 게시글 첨부)", required = true) @RequestParam("attachName") String attachName,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body("업로드할 파일이 없습니다.");
        }

        // 현재 로그인한 사용자 ID
        String creatorId = (userDetails != null) ? userDetails.getUsername() : "anonymousUser";

        try {
            Attach attach = ftpService.uploadNewAttachment(files, attachName, creatorId);
            return ResponseEntity.status(HttpStatus.CREATED).body(attach);
        } catch (IOException e) {
            log.error("파일 업로드 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("파일 업로드에 실패했습니다: " + e.getMessage());
        }
    }

    @PostMapping(value = "/{attachId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "[POST] 기존 첨부에 파일 추가", description = "기존 첨부 묶음에 새로운 파일들을 추가합니다.")
    public ResponseEntity<?> addFilesToAttachment(
            @Parameter(description = "파일을 추가할 첨부의 ID", required = true) @PathVariable Long attachId,
            @Parameter(description = "추가할 파일들", required = true) @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body("업로드할 파일이 없습니다.");
        }

        String uploaderId = (userDetails != null) ? userDetails.getUsername() : "anonymousUser";

        try {
            // 새로운 서비스 메서드 호출
            List<AttachFile> addedFiles = ftpService.addFilesToAttachment(attachId, files, uploaderId);
            return ResponseEntity.ok(addedFiles);
        } catch (IllegalArgumentException e) {
            // 존재하지 않는 attachId일 경우
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IOException e) {
            log.error("기존 첨부에 파일 추가 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("파일 업로드에 실패했습니다: " + e.getMessage());
        }
    }

    @GetMapping("/{attachId}/files")
    @Operation(summary = "[GET] 특정 첨부에 속한 파일 목록 조회", description = "첨부 ID를 이용해 해당 묶음에 포함된 모든 파일 목록을 조회.")
    public ResponseEntity<List<AttachFileResponse>> getAttachmentFiles(
            @Parameter(description = "조회할 첨부의 ID", required = true) @PathVariable Long attachId) {

        List<AttachFile> files = ftpService.getFilesByAttachId(attachId);

        // Entity List -> DTO List 변환
        List<AttachFileResponse> responseList = files.stream()
                .map(file -> AttachFileResponse.builder()
                        .fileId(file.getFileId())
                        .originalFileName(file.getOriginalFileName())
                        .fileSize(file.getFileSize())
                        .uploadedAt(file.getUploadedAt())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
    }

    @GetMapping("/all-files")
    @PreAuthorize("hasRole('ADMIN')") // 관리자 권한 필요
    @Operation(summary = "[GET] 모든 첨부파일 목록 조회 (관리자용)", description = "시스템에 업로드된 모든 첨부파일 목록을 페이징하여 조회. 관리자만 접근 가능.")
    public ResponseEntity<Page<AttachFileResponse>> getAllAttachFiles(
            @ParameterObject @PageableDefault(size = 10, sort = "UPLOADED_AT", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AttachFileResponse> filesPage = ftpService.getAllAttachFiles(pageable);
        return ResponseEntity.ok(filesPage);
    }

    @GetMapping("/all-attaches")
    @PreAuthorize("hasRole('ADMIN')") // 관리자 권한 필요
    @Operation(summary = "[GET] 모든 첨부 묶음 목록 조회 (관리자용)", description = "시스템에 업로드된 모든 첨부 묶음 목록을 페이징하여 조회. 관리자만 접근 가능.")
    public ResponseEntity<Page<AttachResponse>> getAllAttaches(
            @ParameterObject @PageableDefault(size = 10, sort = "CREATED_AT", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AttachResponse> attachesPage = ftpService.getAllAttaches(pageable);
        return ResponseEntity.ok(attachesPage);
    }

    @PostMapping("/files/{fileId}/delete")
    @Operation(summary = "[DELETE] 첨부파일 논리적 삭제", description = "파일 ID를 이용해 특정 파일을 논리적으로 삭제하고, FTP 서버에서도 물리적으로 삭제.")
    public ResponseEntity<String> deleteAttachmentFile(
            @Parameter(description = "삭제할 파일의 ID", required = true) @PathVariable long fileId) {

        boolean success = ftpService.deleteAttachmentFile(fileId);

        if (success) {
            return ResponseEntity.ok("파일이 성공적으로 삭제되었습니다.");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("파일 삭제에 실패했거나, 이미 삭제된 파일입니다.");
        }
    }

    @PostMapping("/{attachId}/delete")
    @Operation(summary = "[DELETE] 첨부 묶음 전체 논리적 삭제", description = "첨부 ID를 이용해 해당 묶음에 속한 모든 파일을 논리적으로 삭제하고, FTP 서버에서도 물리적으로 삭제.")
    public ResponseEntity<String> deleteAttachmentByAttachId(
            @Parameter(description = "삭제할 첨부 묶음의 ID", required = true) @PathVariable Long attachId) {

        boolean success = ftpService.deleteAttachmentByAttachId(attachId);

        if (success) {
            return ResponseEntity.ok("첨부 묶음의 모든 파일이 성공적으로 삭제되었습니다.");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("첨부 묶음 파일 삭제에 실패했습니다.");
        }
    }

    @GetMapping("/download/{fileId}")
    @Operation(summary = "[GET] 개별 첨부파일 다운로드", description = "파일 ID를 이용해 특정 첨부파일을 다운로드.")
    public ResponseEntity<byte[]> downloadFile(
            @Parameter(description = "다운로드할 파일의 ID", required = true) @PathVariable Long fileId) {
        try {
            // 파일을 스트림으로 직접 응답에 쓰기 위해 ByteArrayOutputStream 사용
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            AttachFile fileInfo = ftpService.downloadFile(fileId, outputStream);

            if (fileInfo == null) {
                return ResponseEntity.notFound().build();
            }

            // 파일명 인코딩 (한글 파일명 깨짐 방지)
            String encodedFileName = java.net.URLEncoder.encode(fileInfo.getOriginalFileName(), "UTF-8").replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM) // 바이너리 파일임을 명시
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileInfo.getFileSize()))
                    .body(outputStream.toByteArray());

        } catch (IOException e) {
            log.error("파일 다운로드 중 오류 발생: fileId={}", fileId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/download-bundle/{attachId}")
    @Operation(summary = "[GET] 첨부 묶음 ZIP 다운로드", description = "첨부 ID를 이용해 해당 묶음에 포함된 모든 파일을 ZIP 파일로 압축하여 다운로드.")
    public ResponseEntity<byte[]> downloadAttachBundle(
            @Parameter(description = "다운로드할 첨부 묶음의 ID", required = true) @PathVariable Long attachId,
            @Parameter(description = "다운로드할 ZIP 파일명 (선택 사항)") @RequestParam(required = false) String zipFileName) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ftpService.downloadAttachBundle(attachId, outputStream);

            String finalZipFileName = (zipFileName != null && !zipFileName.trim().isEmpty()) ?
                    zipFileName : "attach_bundle_" + attachId + ".zip";
            String encodedFileName = java.net.URLEncoder.encode(finalZipFileName, "UTF-8").replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"")
                    .body(outputStream.toByteArray());

        } catch (IOException e) {
            log.error("첨부 묶음 다운로드 중 오류 발생: attachId={}", attachId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}