const CLIENT_ID = "97e587258186bc669efbb5a871f13b2d";
const REDIRECT_URI = "http://localhost:3000/auth/login/kakao";

export const KAKAO_AUTH_URL = `https://kauth.kakao.com/oauth/authorize?client_id=${CLIENT_ID}&redirect_uri=${REDIRECT_URI}&response_type=code`;