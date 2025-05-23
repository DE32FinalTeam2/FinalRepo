import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import Cookies from 'js-cookie';
import "./JobSummaryPage.css";
import axios from 'axios';
import qs from 'qs'

const JobSummaryPage = () => {
    const [activeButton, setActiveButton] = useState(null);
    const [jobData, setJobData] = useState([]); // 채용 공고 데이터를 저장할 상태
    const [isDropdownOpen, setIsDropdownOpen] = useState(false);
    const [currentPage, setCurrentPage] = useState(1); // 현재 페이지 상태
    const [itemsPerPage] = useState(20); // 한 페이지에 표시할 항목 수
    const [jobTitles] = useState(["BE", "FE", "DE", "DA", "MLE"]);
    const [bookmarkedJobs, setBookmarkedJobs] = useState({}); // 예: { 1: true, 2: false }
    const [nickname, setNickname] = useState(""); // 사용자 닉네임 상태
    const navigate = useNavigate();

    // 로그인 상태 확인 함수
    const checkLoginStatus = () => {
        const accessToken = Cookies.get('access_token');
        return !!accessToken; // 토큰이 있으면 true, 없으면 false
    };

    // 카카오 SDK 초기화 및 리디렉션 URL 저장
    useEffect(() => {
        if (typeof window.Kakao !== "undefined" && !window.Kakao.isInitialized()) {
            window.Kakao.init("9ae623834d6fbc0413f981285a8fa0d5"); // YOUR_APP_KEY
        }

        // 로그인 요청 이전에 있던 페이지 URL을 sessionStorage에 저장
        // console.log("Storing redirect URL:", redirectUrl);
        const redirectUrl = window.location.pathname; // 현재 페이지의 경로
        sessionStorage.setItem("redirectUrl", redirectUrl); // 세션 스토리지에 저장
    }, []);

    // 사용자 정보를 가져오는 함수
    useEffect(() => {
        if (checkLoginStatus()) {
            // 예: API 호출로 사용자 정보를 가져온다고 가정
            const fetchUserData = async () => {
                try {
                    const response = await fetch("/user/profile", {
                        headers: { Authorization: `Bearer ${Cookies.get('access_token')}` },
                    });
                    if (response.ok) {
                        const data = await response.json();
                        setNickname(data.name || "사용자"); // 닉네임 설정
                    }
                } catch (error) {
                    console.error("Error fetching user data:", error);
                }
            };
            fetchUserData();
        }
    }, []);

    // 현재 페이지에 해당하는 데이터만 추출
    const indexOfLastJob = currentPage * itemsPerPage;
    const indexOfFirstJob = indexOfLastJob - itemsPerPage;
    const currentJobs = jobData.slice(indexOfFirstJob, indexOfLastJob);

    // 페이지 변경 처리 함수
    const paginate = (pageNumber) => setCurrentPage(pageNumber);

    // 페이지 이동 처리
    const handleClick = () => {
        navigate("/", { replace: true });
        window.location.reload();
    };

    const handleBookmark = async (noticeId) => {
        if (!checkLoginStatus()) {
            alert("로그인 후 이용하실 수 있습니다.");
            navigate("/login");
            return;
        }
    
        const accessToken = Cookies.get('access_token');
    
        // 로그로 보내는 데이터 확인
        console.log("Sending bookmark request with data:", { noticeId });

        try {
            // POST 요청을 통해 북마크 추가/삭제 처리
            const response = await axios.post(
                "http://43.202.186.119:8972/user/bookmark/notice",  // 해당 API 엔드포인트로 요청
                null,  // 데이터가 필요 없다면 null, 아니면 실제 데이터 전달
                {
                    headers: {
                        "Authorization": `Bearer ${accessToken}`,  // Authorization 헤더에 Bearer 토큰 설정
                    },
                    params: {
                        noticeId: noticeId,  // noticeId를 URL 파라미터로 전달
                    },
                }
            );
        
            // 성공적으로 응답을 받았다면
            alert(response.data);  // 서버 응답 메시지 출력
        
        } catch (error) {
            // 에러 처리
            if (error.response) {
                // 응답이 있는 경우
                alert('북마크 추가 실패: ' + (error.response.data.message || error.response.data));
            } else {
                // 응답이 없는 경우
                alert('서버에 연결할 수 없습니다. 다시 시도해주세요.');
            }
        }
    };
    
    //     try {
    //         const response = await axios.post(
    //             'http://43.202.186.119:8972/user/bookmark/notice',
    //             { noticeId: noticeId }, // 실제로 보내는 데이터 확인
    //             { headers: { Authorization: `Bearer ${accessToken}` } }
    //         );
    
    //         alert(response.data); // 서버 응답 메시지 표시
    //     } catch (error) {
    //         if (error.response) {
    //             // 응답이 있는 경우
    //             alert('북마크 추가 실패: ' + error.response.data.message || error.response.data);
    //         } else {
    //             // 응답이 없는 경우
    //             alert('서버에 연결할 수 없습니다. 다시 시도해주세요.');
    //         }
    //     }
    // };
    
    
    
    
    
    
    
    // 드롭다운 메뉴 열기/닫기 처리
    const toggleDropdown = () => {
        setIsDropdownOpen((prev) => !prev);
    };

    const goToJobSummary = () => {
        navigate("/job-summary"); // 기업 공고 요약 페이지로 이동
        window.location.reload();
    };

    // My Page 이동 처리
    const handleMypage = () => {
        if (checkLoginStatus()) {
            navigate("/mypage");
        } else {
            alert("로그인 후 이용하실 수 있습니다.");
            navigate("/login");
        }
    };

    // 데이터 fetch 함수
    const fetchJobData = async (role) => {
        try {
            const response = await fetch(`/notice?jobtitle=${role}`);
            const data = await response.json();
            if (response.ok) {
                const transformedData = data.map((job) => ({
                    id: job.noticeid,
                    deadline: job.duetype === "날짜" ? job.duedate : "상시채용",
                    org_url: job.orgurl,
                    companyName: job.company,
                    jobTitle: job.posttitle,
                    mainTask: job.responsibility || "상세 미제공",
                    qualifications: job.qualification || "상세 미제공",
                    preferences: job.preferential || "상세 미제공",
                    techStack: job.tottech || "기술 스택 미제공",
                }));
                setJobData(transformedData);
            } else {
                console.error("Error fetching job data:", data);
                setJobData([]);
            }
        } catch (error) {
            console.error("Error fetching job data:", error);
            setJobData([]);
        }
    };

    // 직무 버튼 클릭 시 데이터 로드
    useEffect(() => {
        if (activeButton) {
            fetchJobData(activeButton);
        }
    }, [activeButton]);

    // 처음 페이지가 로드될 때 BE 버튼을 활성화하도록 설정
    useEffect(() => {
        setActiveButton("BE");
    }, []);

    // 페이지네이션 버튼 생성
    const pageNumbers = [];
    const totalPages = Math.ceil(jobData.length / itemsPerPage);
    for (let i = 1; i <= totalPages; i++) {
        pageNumbers.push(i);
    }

    const paginateRangeStart = Math.floor((currentPage - 1) / 10) * 10 + 1;
    const paginateRangeEnd = Math.min(paginateRangeStart + 9, totalPages);
    const visiblePageNumbers = pageNumbers.slice(paginateRangeStart - 1, paginateRangeEnd);

    return (
        <div className="job-summary-page">
            <div className="top-right-buttons">
                {checkLoginStatus() ? (
                    <span className="welcome-message">{nickname}님 환영합니다!</span>
                ) : (
                    <button className="auth-button" onClick={() => navigate("/login")}>
                        로그인
                    </button>
                )}
            </div>

            <div className="top-left-menu">
                <button className="menu-button" onClick={toggleDropdown}>
                    ⁝⁝⁝
                </button>
                {isDropdownOpen && (
                    <div className="dropdown-menu open">
                        <button className="dropdown-item" onClick={handleClick}>
                            기술 스택 순위
                        </button>
                        <button className="dropdown-item" onClick={goToJobSummary}>
                            채용 공고 요약
                        </button>
                        <hr />
                        <button className="dropdown-item" onClick={handleMypage}>
                            My Page
                        </button>
                    </div>
                )}
            </div>

            <div className="logo-container">
                <h1 className="logo" onClick={handleClick}>
                    JobScanner
                </h1>
            </div>



            <div className="content">
                <p className="message1">직무별 채용 공고 보기</p>
                <div className="toggle-buttons">
                    {jobTitles.map((role) => (
                        <button
                            key={role}
                            className={`toggle-button ${activeButton === role ? "active" : ""}`}
                            onClick={() => setActiveButton(role)}
                        >
                            {role}
                        </button>
                    ))}
                </div>
            </div>

            {activeButton && jobData.length > 0 && (
                <div className="summary-table-container">
                    <table className="summary-table">
                        <thead className="summary-thead">
                            <tr>
                                <th>마감일</th>
                                <th>회사 이름</th>
                                <th>공고 제목</th>
                                <th>주요 업무</th>
                                <th>자격 요건</th>
                                <th>우대 사항</th>
                                <th>기술 스택 목록</th>
                                <th>북마크</th>
                            </tr>
                        </thead>
                        <tbody className="summary-td">
                        {currentJobs.map((job) => {
                            // console.log(job);  // job 객체가 제대로 전달되는지 확인
                            return (
                                <tr key={job.id}>
                                    <td>{job.deadline}</td>
                                    <td>{job.companyName}</td>
                                    <td>
                                        <a href={job.org_url} target="_blank" rel="noopener noreferrer">
                                            {job.jobTitle}
                                        </a>
                                    </td>
                                    <td>{job.mainTask}</td>
                                    <td>{job.qualifications}</td>
                                    <td>{job.preferences}</td>
                                    <td>{job.techStack}</td>
                                    <td>
                                        <span
                                            className={`bookmark-button ${bookmarkedJobs[job.id] ? "active" : ""}`}
                                            onClick={() => handleBookmark(job.id)}  // 여기서 job.id가 undefined로 나오는지 확인
                                        >
                                            {bookmarkedJobs[job.id] ? "★" : "☆"}
                                        </span>
                                    </td>
                                </tr>
                            );
                        })}
                        </tbody>
                    </table>
                    <div className="pagination">
                        {visiblePageNumbers.map((number) => (
                            <button
                                key={number}
                                onClick={() => paginate(number)}
                                className={`page-button ${currentPage === number ? "active" : ""}`}
                            >
                                {number}
                            </button>
                        ))}
                    </div>
                </div>
            )}
        </div>
    );
};

export default JobSummaryPage;
