package com.akmz.springBase.attach.controller;

import com.akmz.springBase.attach.model.dto.TempFileResponse;
import com.akmz.springBase.attach.service.FtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/temp-files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Temporary File API", description = "임시 파일 업로드 및 관리 API")
public class TempFileController {

    private final FtpService ftpService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "[POST] 임시 파일 업로드", description = "파일을 FTP 서버의 임시 디렉토리에 업로드하고 경로를 반환합니다. DB에 기록되지 않습니다.")
    @ApiResponse(responseCode = "200", description = "파일 업로드 성공", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = TempFileResponse.class)))
    @ApiResponse(responseCode = "400", description = "잘못된 요청 (예: 파일 없음)", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = TempFileResponse.class)))
    @ApiResponse(responseCode = "500", description = "서버 내부 오류 (예: FTP 업로드 실패)", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = TempFileResponse.class)))
    public ResponseEntity<TempFileResponse> uploadTempFile(
            @Parameter(description = "업로드할 파일", required = true) @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(TempFileResponse.builder().name("업로드할 파일이 없습니다.").build());
        }

        try {
            TempFileResponse tempFileResponse = ftpService.uploadTempFile(file);
            return ResponseEntity.ok(tempFileResponse);
        } catch (IOException e) {
            log.error("임시 파일 업로드 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(TempFileResponse.builder().name("임시 파일 업로드에 실패했습니다: " + e.getMessage()).build());
        }
    }
}