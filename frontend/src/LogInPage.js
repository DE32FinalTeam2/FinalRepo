import React, { useState, useEffect } from 'react';
import './LogInPage.css';
import { useNavigate } from 'react-router-dom';
import { Cookies } from 'react-cookie';

function KakaoLogin() {
  const [userInfo, setUserInfo] = useState(null);
  const navigate = useNavigate();
  const cookies = new Cookies();

  // 로그인 후 리디렉션
  const handleRedirect = () => {
    const redirectUrl = sessionStorage.getItem('redirectUrl') || '/';  // 기본적으로 루트 페이지로 리디렉션
    console.log("Redirecting to:", redirectUrl);
    navigate(redirectUrl, { replace: true });
    sessionStorage.removeItem('redirectUrl'); // 리디렉션 후 세션 스토리지에서 URL 제거
  };

  // 쿠키에 값을 설정하는 함수
  const setCookie = (name, value, days = 7) => {
    cookies.set(name, value, { path: '/', maxAge: days * 24 * 60 * 60 });
  };

  // 쿠키에서 값을 가져오는 함수
  const getCookie = (name) => {
    return cookies.get(name);
  };

  // 카카오 SDK 초기화
  useEffect(() => {
    if (typeof window.Kakao !== 'undefined' && !window.Kakao.isInitialized()) {
      window.Kakao.init('YOUR_APP_KEY'); // 카카오 앱 키 입력
    }

    // 로그인 요청 이전에 있던 페이지 URL을 sessionStorage에 저장
    const redirectUrl = window.location.pathname;  // 현재 페이지의 경로
    sessionStorage.setItem('redirectUrl', redirectUrl);  // 세션 스토리지에 저장
  }, []);

  // 카카오 로그인
  const kakaoLogin = () => {
    if (typeof window.Kakao === 'undefined') {
      console.error('카카오 SDK가 로드되지 않았습니다.');
      return;
    }

    window.Kakao.Auth.login({
      success: (authObj) => {
        const accessToken = authObj.access_token;  // 리프레시 토큰 제거

        // 액세스 토큰을 쿠키에 저장
        setCookie('access_token', accessToken);

        // 서버로 액세스 토큰만 전송
        sendTokensToServer(accessToken);

        // 로그인 후 리디렉션
        handleRedirect();
      },
      fail: (err) => {
        console.error('로그인 실패:', err);
      },
    });
  };

  // 서버로 액세스 토큰만 전송
  const sendTokensToServer = (accessToken) => {
    const kakaoTokenDTO = {
      accessToken,  // 액세스 토큰만 추가
    };

    fetch('http://43.202.186.119:8972/login/kakao', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include', // 쿠키를 포함하는 요청
      body: JSON.stringify(kakaoTokenDTO),
    })
      .then((response) => response.json())
      .then((data) => {
        alert(data.message || '로그인 성공');
      })
      .catch((error) => {
        console.error('서버 요청 에러:', error);
        alert('서버 요청 중 오류가 발생했습니다.');
      });
  };

  // 유저 정보 가져오기
  const getUserInfo = () => {
    const token = getCookie('access_token');
    if (!token) {
      alert('먼저 카카오 로그인해주세요!');
      return;
    }

    fetch('http://43.202.186.119:8972/user/profile', {
      method: 'GET',
      headers: {
        Authorization: `Bearer ${token}`,
      },
    })
      .then((response) => response.json())
      .then((data) => {
        setUserInfo(data); // 유저 정보 상태 저장
      })
      .catch((error) => {
        console.error('유저 정보 가져오기 실패:', error);
      });
  };

  return (
    <div className="main-page">
      <div className="logo-container">
        <div className="logo" onClick={handleRedirect}>JobScanner</div>
      </div>
      <h1>로그인</h1>
      <div className="login-container">
        <div className="login-buttons">
          <button onClick={kakaoLogin}>
            <img src="/image/kakao.png" alt="Kakao Login" style={{ width: "200px", height: "auto" }} />
          </button>
        </div>
      </div>

      {/* 로그인 후 유저 정보를 보여주는 부분 */}
      {userInfo && (
        <div className="user-info">
          <h3>유저 정보</h3>
          <pre>{JSON.stringify(userInfo, null, 2)}</pre>
        </div>
      )}
    </div>
  );
}

export default KakaoLogin;


