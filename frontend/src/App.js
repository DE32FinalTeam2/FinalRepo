import React from "react";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import MainPage from "./MainPage"; // 메인 페이지 컴포넌트
import JobSummaryPage from "./JobSummaryPage"; // 기업 공고 요약 페이지 컴포넌트
import TechStackDetailsPage from "./TechStackDetailsPage"; // TechStackDetailsPage 컴포넌트 임포트
import LoginPage from "./LogInPage";
import SignUpPage from "./SignUpPage";
import MyPage from "./MyPage";

function App() {
    return (
        <Router>
            <Routes>
                <Route path="/" element={<MainPage />} /> {/* 메인 페이지 */}
                <Route path="/tech-stack-details/:role/:category/:techStackName" element={<TechStackDetailsPage />} />
                <Route path="/job-summary" element={<JobSummaryPage />} /> {/* 기업 공고 요약 */}
                <Route path="/auth/login" element={<LoginPage />} />
                <Route path="/sign-up" element={<SignUpPage />} />
                <Route path="/mypage" element={<MyPage />} />
            </Routes>
        </Router>
    );
}

export default App;
