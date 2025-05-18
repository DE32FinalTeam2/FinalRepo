package com.team2.jobscanner.controller;

import com.team2.jobscanner.dto.KakaoTokenDTO;
import com.team2.jobscanner.entity.User;
import com.team2.jobscanner.service.UserService;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = {"http://43.202.186.119", "http://www.jobscanner.site"}, allowCredentials = "true")
@RequestMapping("/login")
@RestController
public class OAuthController {
    private static final Logger logger = LoggerFactory.getLogger(OAuthController.class);

    @Autowired
    private UserService userService;

    @PostMapping("/kakao")
    public ResponseEntity<String> kakaologin(@RequestBody KakaoTokenDTO kakaoTokenDTO) {
        String accessToken = kakaoTokenDTO.getAccessToken();

        try {
            // 카카오 API를 통해 사용자 정보 요청
            User user = userService.getUserInfoFromKakao(accessToken);

            if (user == null) {
                // 신규 사용자 등록
                user = userService.createUserFromKakao(accessToken);
            } else {
                // 기존 사용자 정보 업데이트
                userService.updateUserFromKakao(user, accessToken);
            }

            return ResponseEntity.ok("{\"message\": \"로그인 성공\"}");
        } catch (Exception e) {
            logger.error("카카오 로그인 오류: ", e);
            return ResponseEntity.status(500).body("로그인 처리에 실패했습니다.");
        }
    }
}

