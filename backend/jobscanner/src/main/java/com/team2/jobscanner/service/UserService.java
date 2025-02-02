package com.team2.jobscanner.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team2.jobscanner.dto.NoticeDTO;
import com.team2.jobscanner.dto.TechStackDTO;
import com.team2.jobscanner.dto.UserDTO;
import com.team2.jobscanner.entity.*;
import com.team2.jobscanner.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {
    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NoticeBookmarkRepository noticeBookmarkRepository;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private TechStackRepository techStackRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    private final TechStackBookmarkRepository techStackBookmarkRepository;


    public UserService(UserRepository userRepository,
                       TechStackBookmarkRepository techStackBookmarkRepository,
                       NoticeBookmarkRepository noticeBookmarkRepository) {
        this.userRepository = userRepository;
        this.techStackBookmarkRepository = techStackBookmarkRepository;
        this.noticeBookmarkRepository = noticeBookmarkRepository;
    }

    public User getUserInfoFromKakao(String accessToken) {
        String url = "https://kapi.kakao.com/v2/user/me";
        RestTemplate restTemplate = new RestTemplate();
        try {
            // 카카오 API로 사용자 정보 요청
            String response = restTemplate.getForObject(url + "?access_token=" + accessToken, String.class);

            // JSON 응답을 파싱하여 이메일, 이름 정보 추출
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(response);
            String email = jsonNode.path("kakao_account").path("email").asText();
            String name = jsonNode.path("properties").path("nickname").asText();

            // 사용자 정보 반환
            return userRepository.findUserByEmail(email); // 이메일로 사용자 검색
        } catch (Exception e) {
            // 오류 처리
            throw new RuntimeException("카카오 사용자 정보 요청 실패", e);
        }
    }

    // 카카오로부터 받은 정보로 새 사용자 생성
    public User createUserFromKakao(String accessToken) {
        String url = "https://kapi.kakao.com/v2/user/me";
        RestTemplate restTemplate = new RestTemplate();
        try {
            // 카카오 API로 사용자 정보 요청
            String response = restTemplate.getForObject(url + "?access_token=" + accessToken, String.class);

            // JSON 응답을 파싱하여 이메일, 이름 정보 추출
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(response);
            String email = jsonNode.path("kakao_account").path("email").asText();
            String name = jsonNode.path("properties").path("nickname").asText();

            // 새 사용자 객체 생성
            User user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setOauthProvider("kakao");

            return userRepository.save(user);  // 새 사용자 저장
        } catch (Exception e) {
            throw new RuntimeException("카카오 사용자 정보 요청 실패", e);
        }
    }

    // 기존 사용자 정보 갱신
    public void updateUserFromKakao(User user, String accessToken) {
        String url = "https://kapi.kakao.com/v2/user/me";
        RestTemplate restTemplate = new RestTemplate();
        try {
            // 카카오 API로 사용자 정보 요청
            String response = restTemplate.getForObject(url + "?access_token=" + accessToken, String.class);

            // JSON 응답을 파싱하여 이메일, 이름 정보 추출
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(response);
            String name = jsonNode.path("properties").path("nickname").asText();

            // 사용자 정보 갱신
            user.setName(name);
            userRepository.save(user);  // 갱신된 사용자 저장
        } catch (Exception e) {
            throw new RuntimeException("카카오 사용자 정보 요청 실패", e);
        }
    }

    // 리프레시 토큰 저장
    public void saveAuthToken(User user, String refreshToken) {
        Auth auth = authRepository.findByUserId(user.getId());

        if (auth == null) {
            // 새로 저장
            auth = new Auth();
            auth.setUser(user);
            auth.setRefreshToken(refreshToken);
            authRepository.save(auth);
        } else {
            // 기존 리프레시 토큰 갱신
            auth.setRefreshToken(refreshToken);
            authRepository.save(auth);
        }

    }


    // 액세스 토큰을 사용하여 사용자 정보 조회
    public User getUserInfoFromAccessToken(String accessToken) {
        String url = "https://kapi.kakao.com/v2/user/me";
        try {
            // 카카오 API로 사용자 정보 요청
            String response = restTemplate.getForObject(url + "?access_token=" + accessToken, String.class);

            // JSON 응답을 파싱하여 사용자 정보 추출
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(response);
            String email = jsonNode.path("kakao_account").path("email").asText();

            // 사용자 정보 반환
            return userRepository.findUserByEmail(email); // 이메일로 사용자 검색
        } catch (Exception e) {
            throw new RuntimeException("액세스 토큰으로 사용자 정보 요청 실패", e);
        }
    }

    // 리프레시 토큰으로 액세스 토큰을 갱신
    public String refreshAccessToken(String refreshToken) {
        String url = "https://kauth.kakao.com/oauth/token";
        try {
            // 리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급
            String response = restTemplate.postForObject(url,
                    "grant_type=refresh_token&refresh_token=" + refreshToken + "&client_id=YOUR_APP_KEY", String.class);

            // JSON 응답에서 액세스 토큰 추출
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(response);
            return jsonNode.path("access_token").asText();
        } catch (Exception e) {
            throw new RuntimeException("리프레시 토큰을 통한 액세스 토큰 갱신 실패", e);
        }
    }

    // 유저의 리프레시 토큰을 가져옴
    public String getRefreshTokenByUserId(Long userId) {
        Auth auth = authRepository.findByUserId(userId);
        return auth != null ? auth.getRefreshToken() : null;
    }

    public UserDTO getUserProfile(String email) {
        User user = userRepository.findUserByEmail(email);  // 이메일로 사용자 조회

        List<TechStackDTO> techStackDTOs = techStackBookmarkRepository.findByUser(user).stream()
                .map(bookmark -> new TechStackDTO(
                        bookmark.getTechStack().getTechName(),
                        bookmark.getTechStack().getTechDescription(),
                        bookmark.getTechStack().getYoutubeLink(),
                        bookmark.getTechStack().getBookLink(),
                        bookmark.getTechStack().getDocsLink()
                )).collect(Collectors.toList());

        List<NoticeDTO> noticeDTOs = noticeBookmarkRepository.findByUser(user).stream()
                .map(bookmark -> new NoticeDTO(
                        bookmark.getNotice().getNotice_id(),
                        bookmark.getNotice().getDueType(),
                        bookmark.getNotice().getDueDate(),
                        bookmark.getNotice().getCompany(),
                        bookmark.getNotice().getPostTitle(),
                        bookmark.getNotice().getResponsibility(),
                        bookmark.getNotice().getQualification(),
                        bookmark.getNotice().getPreferential(),
                        bookmark.getNotice().getTotTech(),
                        bookmark.getNotice().getOrgUrl()
                ))
                .collect(Collectors.toList());

        return new UserDTO(user.getEmail(), user.getName(), techStackDTOs, noticeDTOs);
    }

    public boolean addOrRemoveNoticeBookmark(String accessToken, Long noticeId) {
        // 사용자 정보를 액세스 토큰으로 얻기
        User user = getUserInfoFromAccessToken(accessToken); // 해당 메서드 필요
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // 공고 조회
        Notice notice = noticeRepository.findById(noticeId).orElse(null);
        if (notice == null) {
            throw new RuntimeException("Notice not found");
        }

        // 북마크 존재 여부 확인
        Optional<NoticeBookmark> existingBookmark = noticeBookmarkRepository.findByUserAndNotice(user, notice);

        if (existingBookmark.isPresent()) {
            // 북마크가 이미 존재하면 삭제
            noticeBookmarkRepository.delete(existingBookmark.get());
            return false;
        } else {
            // 북마크가 없다면 새로 추가
            NoticeBookmark newBookmark = new NoticeBookmark();
            newBookmark.setUser(user);
            newBookmark.setNotice(notice);
            noticeBookmarkRepository.save(newBookmark);
            return true;
        }
    }

    public boolean addOrRemoveTechStackBookmark(String accessToken, String techName) {
        User user = getUserInfoFromAccessToken(accessToken); // 사용자 정보 가져오기
        if (user == null) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }

        // 기술 스택 조회
        TechStack techStack = techStackRepository.findByTechName(techName);
        if (techStack == null) {
            throw new RuntimeException("기술 스택을 찾을 수 없습니다.");
        }

        // 이미 북마크가 있는지 확인
        Optional<TechStackBookmark> existingBookmark = techStackBookmarkRepository.findByUserAndTechStack(user, techStack);

        if (existingBookmark.isPresent()) {
            // 북마크가 존재하면 삭제
            techStackBookmarkRepository.delete(existingBookmark.get());
            return false;
        } else {
            // 북마크가 없다면 추가
            TechStackBookmark newBookmark = new TechStackBookmark();
            newBookmark.setUser(user);
            newBookmark.setTechStack(techStack);
            techStackBookmarkRepository.save(newBookmark);
            return true;
        }
    }

}

