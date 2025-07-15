package com.akmz.springBase.base.service;

import com.akmz.springBase.base.model.dto.Attachment;
import com.akmz.springBase.base.model.dto.EmailRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * 이메일 전송 (파일 첨부, CC, BCC, HTML 포함)
     * @param emailRequest 이메일 전송에 필요한 모든 정보를 담은 DTO
     * @throws MessagingException 이메일 전송 중 오류 발생 시
     */
    public void sendEmail(EmailRequest emailRequest) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(emailRequest.getTo());
        helper.setSubject(emailRequest.getSubject());
        helper.setText(emailRequest.getText(), emailRequest.isHtml());

        if (emailRequest.getCc() != null && emailRequest.getCc().length > 0) {
            helper.setCc(emailRequest.getCc());
        }
        if (emailRequest.getBcc() != null && emailRequest.getBcc().length > 0) {
            helper.setBcc(emailRequest.getBcc());
        }

        if (emailRequest.getAttachments() != null && !emailRequest.getAttachments().isEmpty()) {
            for (Attachment attachment : emailRequest.getAttachments()) {
                helper.addAttachment(
                        Objects.requireNonNull(attachment.getFilename()),
                        new ByteArrayResource(attachment.getContent()),
                        attachment.getContentType()
                );
            }
        }

        mailSender.send(message);
    }
}