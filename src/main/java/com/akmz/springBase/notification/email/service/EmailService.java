package com.akmz.springBase.notification.email.service;

import com.akmz.springBase.attach.model.dto.Attachment;
import com.akmz.springBase.attach.service.FtpService;
import com.akmz.springBase.notification.email.model.dto.EmailRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final FtpService ftpService;

    /**
     * 이메일 전송 (파일 첨부, CC, BCC, HTML 포함)
     * @param emailRequest 이메일 전송에 필요한 모든 정보를 담은 DTO
     * @throws MessagingException 이메일 전송 중 오류 발생 시
     */
    public void sendEmail(EmailRequest emailRequest) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(emailRequest.getTo().toArray(new String[0]));
        helper.setSubject(emailRequest.getSubject());
        helper.setText(emailRequest.getText(), emailRequest.isHtml());

        if (emailRequest.getCc() != null && !emailRequest.getCc().isEmpty()) {
            helper.setCc(emailRequest.getCc().toArray(new String[0]));
        }
        if (emailRequest.getBcc() != null && !emailRequest.getBcc().isEmpty()) {
            helper.setBcc(emailRequest.getBcc().toArray(new String[0]));
        }

        // 임시 파일 경로를 저장할 리스트
        List<String> tempFilePathsToDelete = new java.util.ArrayList<>();

        try {
            if (emailRequest.getAttachments() != null && !emailRequest.getAttachments().isEmpty()) {
                for (Attachment attachment : emailRequest.getAttachments()) {
                    String filename = Objects.requireNonNull(attachment.getName());
                    if (attachment.getUrl() != null && !attachment.getUrl().isEmpty()) {
                        // FTP 경로가 있는 경우, FTP에서 파일 다운로드
                        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                            ftpService.downloadFileFromFtp(attachment.getUrl(), outputStream);
                            helper.addAttachment(filename, new ByteArrayResource(outputStream.toByteArray()), attachment.getContentType());
                            tempFilePathsToDelete.add(attachment.getUrl()); // 삭제할 임시 파일 경로 추가
                        } catch (IOException e) {
                            log.error("FTP 파일 다운로드 중 오류 발생: {}", attachment.getUrl(), e);
                            throw new MessagingException("FTP 파일 다운로드 실패: " + attachment.getUrl(), e);
                        }
                    } else if (attachment.getContent() != null) {
                        // byte array가 있는 경우, 직접 첨부
                        helper.addAttachment(filename, new ByteArrayResource(attachment.getContent()), attachment.getContentType());
                    } else {
                        log.warn("첨부파일 데이터(URL 또는 Content)가 없습니다: {}", filename);
                    }
                }
            }

            mailSender.send(message);
        } finally {
            // 이메일 발송 후 임시 파일 삭제
            for (String tempFilePath : tempFilePathsToDelete) {
                try {
                    ftpService.deleteFileFromFtp(tempFilePath);
                    log.info("임시 파일 삭제 성공: {}", tempFilePath);
                } catch (Exception e) {
                    log.error("임시 파일 삭제 중 오류 발생: {}", tempFilePath, e);
                }
            }
        }
    }
}