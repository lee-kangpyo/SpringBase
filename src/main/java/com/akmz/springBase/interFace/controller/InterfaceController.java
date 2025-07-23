package com.akmz.springBase.interFace.controller;

import com.akmz.springBase.interFace.service.FtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@RestController
@RequestMapping("/api/interface/ftp")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Interface - FTP", description = "FTP 파일 전송 인터페이스 API")
public class InterfaceController {

    private final FtpService ftpService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "FTP 파일 업로드",
            description = "지정된 파일들을 FTP 서버에 업로드합니다. 파일 이름은 원본 파일 이름을 사용합니다.",
            parameters = {
                    @Parameter(name = "files", description = "업로드할 파일들", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "파일 업로드 성공"),
                    @ApiResponse(responseCode = "400", description = "업로드할 파일이 없거나 요청이 잘못됨"),
                    @ApiResponse(responseCode = "500", description = "파일 업로드 실패 또는 서버 오류")
            }
    )
    public ResponseEntity<String> uploadFiles(@RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest().body("업로드할 파일이 없습니다.");
        }

        // ftp 기본 디렉토리
        String baseRemoteDir = "/temp_uploads/"; 

        StringBuilder results = new StringBuilder();
        boolean allSuccess = true; // 모든 파일이 성공했는지 추적하는 변수

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                results.append("파일 ").append(file.getOriginalFilename()).append(": 비어있음. ");
                continue;
            }

            // 기본 디렉토리와 파일 이름을 조합하여 원격 경로 생성
            String remotePath = baseRemoteDir + file.getOriginalFilename();
            boolean success = ftpService.uploadFileToFtp(file, remotePath); // MultipartFile 직접 전달
            if (success) {
                results.append("파일 ").append(file.getOriginalFilename()).append(" (경로: ").append(remotePath).append("): 업로드 성공. ");
            } else {
                results.append("파일 ").append(file.getOriginalFilename()).append(" (경로: ").append(remotePath).append("): 업로드 실패. ");
                allSuccess = false; // 하나라도 실패하면 false
            }
        }

        if (allSuccess) {
            return ResponseEntity.ok(results.toString());
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(results.toString());
        }
    }

}