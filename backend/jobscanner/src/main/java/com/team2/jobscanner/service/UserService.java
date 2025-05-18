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
    private UserRepository userRepository;

    @Autowired
    private NoticeBookmarkRepository noticeBookmarkRepository;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private TechStackRepository techStackRepository;

    @Autowired
    private TechStackBookmarkRepository techStackBookmarkRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String KAKAO_API_URL = "https://kapi.kakao.com/v2/user/me";

    // 카카오 API로 사용자 정보 요청
    private User getUserInfoFromKakaoAPI(String accessToken) {
        try {
            String response = restTemplate.getForObject(KAKAO_API_URL + "?access_token=" + accessToken, String.class);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(response);
            String email = jsonNode.path("kakao_account").path("email").asText();
            return userRepository.findUserByEmail(email); // 이메일로 사용자 검색
        } catch (Exception e) {
            throw new RuntimeException("카카오 사용자 정보 요청 실패", e);
        }
    }

    // 카카오로부터 받은 정보로 새 사용자 생성
    public User createUserFromKakao(String accessToken) {
        User user = getUserInfoFromKakaoAPI(accessToken);
        if (user == null) {
            user = new User();
            String name = getNameFromKakaoAPI(accessToken);
            user.setEmail(user.getEmail());
            user.setName(name);
            user.setOauthProvider("kakao");
            return userRepository.save(user);
        }
        return user;
    }

    // 카카오 API에서 사용자 이름 조회
    private String getNameFromKakaoAPI(String accessToken) {
        try {
            String response = restTemplate.getForObject(KAKAO_API_URL + "?access_token=" + accessToken, String.class);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(response);
            return jsonNode.path("properties").path("nickname").asText();
        } catch (Exception e) {
            throw new RuntimeException("카카오 사용자 정보 요청 실패", e);
        }
    }

    // 액세스 토큰을 사용하여 사용자 정보 조회
    public User getUserInfoFromAccessToken(String accessToken) {
        return getUserInfoFromKakaoAPI(accessToken); // 액세스 토큰을 이용해 카카오 API에서 사용자 정보를 가져옴
    }

    // 유저 프로필 정보 조회
    public UserDTO getUserProfile(String email) {
        User user = userRepository.findUserByEmail(email);  // 이메일로 사용자 조회

        // 기술 스택 정보 조회
        List<TechStackDTO> techStackDTOs = techStackBookmarkRepository.findByUser(user).stream()
                .map(bookmark -> new TechStackDTO(
                        bookmark.getTechStack().getTechName(),
                        bookmark.getTechStack().getTechDescription(),
                        bookmark.getTechStack().getYoutubeLink(),
                        bookmark.getTechStack().getBookLink(),
                        bookmark.getTechStack().getDocsLink()
                )).collect(Collectors.toList());

        // 공고 북마크 정보 조회
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

        // UserDTO 반환
        return new UserDTO(user.getEmail(), user.getName(), techStackDTOs, noticeDTOs);
    }

    // 공고 북마크 추가 및 삭제
    public boolean addOrRemoveNoticeBookmark(String accessToken, Long noticeId) {
        User user = getUserInfoFromAccessToken(accessToken); // 사용자 정보 가져오기
        if (user == null) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }

        // 공고 조회
        Notice notice = noticeRepository.findById(noticeId).orElse(null);
        if (notice == null) {
            throw new RuntimeException("공고를 찾을 수 없습니다.");
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

    // 기술 스택 북마크 추가 및 삭제
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

}

