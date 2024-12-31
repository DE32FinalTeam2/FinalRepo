package com.jobscanner.service;

import org.springframework.stereotype.Service;
import com.jobscanner.dto.KakaoDTO;
import com.jobscanner.repository.UserRepository;
import com.jobscanner.util.JwtUtil;
import com.jobscanner.util.KakaoUtil;
import lombok.RequiredArgsConstructor;
import com.jobscanner.converter.AuthConverter;
import com.jobscanner.domain.User;
import jakarta.servlet.http.HttpServletResponse;

@Service
@RequiredArgsConstructor
public class AuthService implements AuthServiceInterface {
    private final KakaoUtil kakaoUtil;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Override
    public User oAuthLogin(String accessCode, HttpServletResponse httpServletResponse) {
        KakaoDTO.OAuthToken oAuthToken = kakaoUtil.requestToken(accessCode);
        KakaoDTO.KakaoProfile kakaoProfile = kakaoUtil.requestProfile(oAuthToken);
        String email = kakaoProfile.getKakao_account().getEmail();

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createNewUser(kakaoProfile));

        // Access Token과 Refresh Token을 생성
        String accessToken = jwtUtil.createAccessToken(user.getEmail(), user.getRole().toString());
        String refreshToken = jwtUtil.createRefreshToken(user.getEmail());

        // 클라이언트에 Authorization 헤더로 Access Token 전달
        httpServletResponse.setHeader("Authorization", "Bearer " + accessToken);

        // Refresh Token을 응답에 포함시켜 클라이언트에 반환
        return user;
    }

    private User createNewUser(KakaoDTO.KakaoProfile kakaoProfile) {
        User newUser = AuthConverter.toUser(
                kakaoProfile.getKakao_account().getEmail(),
                kakaoProfile.getKakao_account().getProfile().getNickname()
        );
        return userRepository.save(newUser);
    }
}


// @Service
// @RequiredArgsConstructor
// public class AuthService implements AuthServiceInterface{
//     private final KakaoUtil kakaoUtil;
//     private final UserRepository userRepository;
//     private final JwtUtil jwtUtil;

//     @Override
//     public User oAuthLogin(String accessCode, HttpServletResponse httpServletResponse) {
//         KakaoDTO.OAuthToken oAuthToken = kakaoUtil.requestToken(accessCode);
//         KakaoDTO.KakaoProfile kakaoProfile = kakaoUtil.requestProfile(oAuthToken);
//         String email = kakaoProfile.getKakao_account().getEmail();

//         User user = userRepository.findByEmail(email)
//                 .orElseGet(() -> createNewUser(kakaoProfile));

//         String token = jwtUtil.createAccessToken(user.getEmail(), user.getRole().toString());
//         httpServletResponse.setHeader("Authorization", token);

//         return user;
//     }

//     private User createNewUser(KakaoDTO.KakaoProfile kakaoProfile) {
//         User newUser = AuthConverter.toUser(
//                 kakaoProfile.getKakao_account().getEmail(),
//                 kakaoProfile.getKakao_account().getProfile().getNickname()
//         );
//         return userRepository.save(newUser);
//     }
// }