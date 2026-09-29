package com.moduplaylist.api.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TemporaryPasswordMailService {

  private final JavaMailSender mailSender;

  @Value("${spring.mail.username}")
  private String senderEmail;

  public void send(String email, String temporaryPassword) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(senderEmail);
    message.setTo(email);
    message.setSubject("[MOPL] 임시 비밀번호 안내");
    message.setText(
        "임시 비밀번호: " + temporaryPassword + "\n\n"
            + "유효시간은 발급 시점부터 3분입니다.\n"
            + "로그인 후 비밀번호를 변경해 주세요."
    );

    mailSender.send(message);
  }
}