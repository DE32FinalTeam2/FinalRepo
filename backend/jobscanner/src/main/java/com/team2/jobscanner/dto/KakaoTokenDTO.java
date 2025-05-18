package com.team2.jobscanner.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoTokenDTO {
    private String accessToken;

    public KakaoTokenDTO(String accessToken) {
        this.accessToken = accessToken;
    }
}
