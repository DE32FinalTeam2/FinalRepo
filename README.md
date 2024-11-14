|**프로젝트 기획** | **문서 분류**: 기획안 <br> **팀명**: Team2Job <br> **작성일자**: 2024-11-14 |
|---------------------|-------------------------------------------------------------------------|


| **항목**                  | **내용**                                                                                        |
|---------------------------|--------------------------------------------------------------------------------------------------|
| **프로젝트 개요**         | 해당 프로젝트는 각종 취업 사이트의 정보를 취합하여 필요한 핵심 요구사항이 무엇인지 총 집계하여 <br> 취업 전략을 세우는데 도움이 될 수 있는 **Job Scanner** 서비스입니다.                                                                            |
| **목표**                  | 1. 취업에 관심이 많은 우리의 고민 문제도 해결 가능 <br> 2. 전처리되지 않은 raw한 데이터를 가공하여 서비스를 구축 및 배포 <br> 3. 데이터 엔지니어에게 요구되는 다양한 기술 경험 및 적용       |
| **기대 효과**             | 취업/이직을 준비하는 사람들에게 다양한 취업 사이트의 채용 공고 데이터를 기반으로 취업 방향성을 정할 수 있다. <br> 또한, 커뮤니티를 통해 같은 고민을 하는 사람들과 소통할 수 있다. |
| **팀원 소개**             | - PM(Project Manager): 조하영 <br> - AC(Aglie Coach): 이상훈 <br> - TL(Techical Leader): 이상우 <br> - AA(Application Administrator): 김도현 <br> - GL(Gag Leader): 김동욱 |
| **기본 기능 및 서비스**   | 1. 채용공고에서 요구하는 핵심 키워드를 집계하여 순위를 제공  <br> 2. 해당 키워드를 바탕으로 도움이 될만한 정보/데이터 제공 <br> 3. 카테고리(희망 직무, 분야, 기업 등)을 기준으로 소통할 수 있는 커뮤니티 제공 <br> 4. 데이터 파이프라인 관리자를 위한 대시보드 제작          |

| **사용 기술**                       | **기술**                                |
|-------------------------------------|-----------------------------------------|
| **1. 협업툴**                       | Git, GitHub                             |
| **2. 데이터 베이스**                | S3, MySQL, MariaDB                      |
| **3. Data Engineering/Analytics**   | Crawling: Python(Selenium), VS Code <br> ETL: Airflow(scheduler), Spark(분산 처리) <br> EDA : Python(Numpy, Pandas, Plotly, Folium, WordCloud), Colab  <br> ML: Hugging Face <br> DashBoard: Tableau |
| **4. Back End**                     | Java(Gradle, Maven) <br> IDE, Lib, Intellij |
| **5. Front End**                    | HTML(BootStrap), VS Code, Intellij <br> CSS(TailWind, Font-Awesome), VS Code, Intellij <br> JS(React, Node.js, ESLint), VS Code, Intellij  |
| **6. ETC**                          | Docker, Linux Ubuntu 22.04 Server LTS, k8s  |
