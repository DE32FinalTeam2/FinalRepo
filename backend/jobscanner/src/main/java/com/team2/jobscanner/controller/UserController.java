package com.team2.jobscanner.controller;

import com.team2.jobscanner.dto.UserDTO;
import com.team2.jobscanner.entity.User;
import com.team2.jobscanner.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// @CrossOrigin(origins = "http://43.202.186.119", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST})
@CrossOrigin(origins = {"http://43.202.186.119","http://www.jobscanner.site" })
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    // 생성자 주입 방식
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 프로필 정보 제공
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader(value = HttpHeaders.AUTHORIZATION) String authorization) {
        try {
            // Authorization 헤더에서 "Bearer " 부분을 제거하고 액세스 토큰만 추출
            String accessToken = authorization.substring(7); 
    
            // 액세스 토큰을 사용하여 사용자 정보를 조회
            User user = userService.getUserInfoFromAccessToken(accessToken);
    
            // 액세스 토큰이 만료되었거나 유효하지 않은 경우 재로그인 요청
            if (user == null) {
                return ResponseEntity.status(401).body("Access token expired. Re-login required.");
            }
    
            // 유저 정보 반환
            UserDTO userDTO = userService.getUserProfile(user.getEmail());
            return ResponseEntity.ok(userDTO);
    
        } catch (Exception e) {
            // 예외 처리
            return ResponseEntity.status(500).body("Internal Server Error");
        }
    }

    @PostMapping("/bookmark/notice")
    public ResponseEntity<?> addOrRemoveBookmark(@RequestHeader("Authorization") String authorization,
                                                 @RequestParam Long noticeId) {
        try {
            // 액세스 토큰에서 유저 정보 추출
            String accessToken = authorization.substring(7);  // "Bearer "를 제외한 토큰
            boolean isAdded = userService.addOrRemoveNoticeBookmark(accessToken, noticeId);
            return ResponseEntity.ok(isAdded ? "북마크가 성공적으로 추가됐어요" : "북마크가 성공적으로 해제됐어요");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error processing bookmark");
        }
    }

    @PostMapping("/bookmark/tech")
    public ResponseEntity<?> addOrRemoveTechStackBookmark(@RequestHeader("Authorization") String authorization,
                                                          @RequestParam String techName) {
        try {
            String accessToken = authorization.substring(7);  // "Bearer "를 제외한 토큰
            boolean isAdded = userService.addOrRemoveTechStackBookmark(accessToken, techName);
            return ResponseEntity.ok(isAdded ? "기술 스택이 성공적으로 북마크되었습니다." : "기술 스택 북마크가 성공적으로 해제되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("기술 스택 북마크 처리 중 오류가 발생했습니다.");
        }
    }

}


