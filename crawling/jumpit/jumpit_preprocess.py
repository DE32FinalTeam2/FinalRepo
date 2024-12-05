from pyspark.sql import SparkSession
from pyspark.sql.functions import lit
from datetime import datetime
import boto3
import pymysql

# Spark 세션 생성
spark = SparkSession.builder \
    .appName("S3_to_MySQL") \
    .getOrCreate()

# S3 클라이언트 설정
s3_client = boto3.client('s3')

def list_s3_files(bucket_name, prefix):
    """S3 버킷에서 지정된 prefix(경로)에 있는 모든 파일 목록을 반환"""
    files = []
    continuation_token = None
    while True:
        params = {
            'Bucket': bucket_name,
            'Prefix': prefix
        }
        if continuation_token:
            params['ContinuationToken'] = continuation_token

        response = s3_client.list_objects_v2(**params)
        files.extend([content['Key'] for content in response.get('Contents', [])])
        continuation_token = response.get('NextContinuationToken')
        if not continuation_token:
            break
    return files

def extract_text_from_s3_file(bucket_name, file_key, keyword_groups):
    """S3에서 파일을 읽고 텍스트 추출"""
    response = s3_client.get_object(Bucket=bucket_name, Key=file_key)
    file_content = response['Body'].read().decode('utf-8')

    extracted_sections = {}
    for group in keyword_groups:
        section_name = group['section_name']
        start_keywords = group['start_keywords']
        end_keywords = group['end_keywords']

        start_index = None
        end_index = None
        section_text = ""

        for start in start_keywords:
            start_index = file_content.find(start)
            if start_index != -1:
                break

        if start_index != -1:
            for end in end_keywords:
                end_index = file_content.find(end, start_index)
                if end_index != -1:
                    break

        if start_index != -1 and end_index != -1:
            section_text = file_content[start_index:end_index].strip()

        extracted_sections[section_name] = section_text

    return extracted_sections

def process_and_insert_into_db(bucket_name, prefix, keyword_groups):
    """S3 데이터를 처리하고 MySQL에 적재"""
    files = list_s3_files(bucket_name, prefix)

    # MySQL 연결 설정
    conn = pymysql.connect(
        host='43.201.40.223',
        user='user',
        password='1234',
        db='testdb',
        charset='utf8mb4'
    )
    cursor = conn.cursor()

    for file_key in files:
        if file_key.endswith('.txt'):  # .txt 파일만 처리
            sections = extract_text_from_s3_file(bucket_name, file_key, keyword_groups)
            full_s3_url = "s3://t2jt/" + file_key  # S3 파일 URL 생성
            current_time = datetime.now().strftime('%Y-%m-%d %H:%M:%S')  # 현재 시각

            # 필요한 데이터 추출
            responsibility = sections.get('주요업무', '')
            qualification = sections.get('자격요건', '')
            preferential = sections.get('우대사항', '')

            # MySQL 삽입 쿼리 생성
            insert_query = """
                UPDATE jumpit
                SET 
                    update_time = %s,
                    responsibility = %s,
                    qualification = %s,
                    preferential = %s
                WHERE s3_text_url = %s;

            """
            insert_data = (current_time, responsibility, qualification, preferential, full_s3_url)

            try:
                cursor.execute(insert_query, insert_data)
                conn.commit()
            except Exception as e:
                print(f"Error executing query: {e}")

    # 연결 종료
    cursor.close()
    conn.close()

# 키워드 그룹 정의 (기존 코드와 동일)
keyword_groups = [
    {
        "section_name": "주요업무",
        "start_keywords": [
            "합류하면 함께 할 업무예요", "이런 일을 함께하고 싶습니다.", "담당 업무는 다음과 같습니다.", "[담당업무]", "[담당 업무]", "[담당업무] ", "이런 업무를 해요", "담당업무 내용",
            "함께 담당하게 될 업무입니다", "Key Responsibilities", "담당 업무", "담당업무", "주요업무", "주요 업무", "[주요 업무]", "[주요업무]", "# 담당업무 ",
            "  [우리는 이런일을 합니다]", "담당업무 ", " 담당업무", "담당 업무 ", " 담당 업무", "  【업무소개】 이러한 일을 해요", "주요 업무: ", "이런 업무를 해요 [주요 업무]",
            "직무상세", "[ 업무 안내 ]", "업무 내용:", " 업무 내용 ", "직무소개",
        ],
        "end_keywords": [
            "자격요건", "자격 요건", "필수자격", "필수 자격", "필요 역량", "필요역량", "Required Qualifications", "자격 요건:", "필요역량 및 경험", "기술스택",
            "Bigdata Engineer로 이런 분이 필요합니다", "이런 분과 함께하고 싶습니다.", "이런 분들을 찾고 있어요", "필요역량 ", "  【기술스택】 이러한 툴을 활용해요",
            "이런 역량을 가지신 분을 찾습니다.", "이런 분과 함께하고 싶어요", "[자격요건]", "[자격 요건]", "# 자격요건", "[우리는 이런사람을 원합니다]", "  자격요건",
            "이런 자격을 갖춘 분을 찾고 있어요 [자격 요건]", "[필수 기술]", "필수사항", "지원자격", "지원 자격", "지원자격 ","[ 필수(자격) 조건 ]", "경험/역량 ",
            "[필수 경험과 역량]", "지원자격(필수)", "필수 경험과 역량 ", "자격요건 ", "[필수자격요건]", "필요 자격 및 기술 ", "[우대사항] ", "[우대사항]",
            "우대사항", "우대 사항", "우대 요건", "우대요건", "이런 분이면 더 좋아요", "[우대사항]", "[우대 사항]", "[우대 요건]", "[우대요건]", "# 우대사항 ", " 우대조건",
            "  【자격요건】 이러한 분을 찾고 있어요", "우대 사항:", "이런 분이 오시면 좋아요 [우대사항]", "Benefits ", "우대경험/역량", " 지원자격",
            "스킬", "모집 강의 주제 예시 (교안 및 교육 제공 / 일부만 강의 가능)", "개발 환경", "[모집절차]", "학력 및 경력", "기타 사항", " [ 근무 장소 ] ",
            "경력/기술 요건:", "우대사항 ", "[모집 절차]", "자격요건  ", "[Key Responsibilities]", "이런 조건에서 근무할 예정이에요", "청주본사",
            "[기본조건]", "기타사항", "[지원 및 진행 절차]", "  [자격 요건]", "[우대조건]", " 복리후생", "저희가 우대하는 분들은 :  ", "근무지",
            "●  요구 경험", "■ 근무시간", "자격사항", " [이런 경험이 있으시면 더욱 좋아요]", " [꼭 확인해 주세요!]", "□ 경력 요구사항", "□ 우대사항",
            "[경력] 경력 3년 이내", "[핵심역량 및 기술]",
        ]
    },
    {
        "section_name": "자격요건",
        "start_keywords": [
            "자격요건", "자격 요건", "필수자격", "필수 자격", "필요 역량", "필요역량", "Required Qualifications", "[우리는 이런사람을 원합니다]", "기술스택",
            "Bigdata Engineer로 이런 분이 필요합니다", "이런 분과 함께하고 싶습니다.", "이런 분들을 찾고 있어요", "# 자격요건", "자격 요건:", "필요역량 및 경험",
            "이런 역량을 가지신 분을 찾습니다.", "이런 분과 함께하고 싶어요", "[자격요건]", "[자격 요건]", "필요역량 ", "  【자격요건】 이러한 분을 찾고 있어요",
            "이런 자격을 갖춘 분을 찾고 있어요 [자격 요건]", "[필수 기술]", "필수사항", "지원 자격", "[ 필수(자격) 조건 ]", "경험/역량 ", "지원자격 ", "지원자격",
            "  자격요건", "[필수 경험과 역량]", "지원자격(필수)", "필수 경험과 역량 ", "자격요건 ", "필수자격요건", "필요 자격 및 기술",
        ],
        "end_keywords": [
            "우대사항", "우대 사항", "우대 요건", "우대요건", "이런 분이면 더 좋아요", "[우대사항]", "[우대 사항]", "[우대 요건]", "[우대요건]", "# 우대사항 ",
            "우대조건", "복리후생", "저희가 우대하는 분들은 : ", "근무지", "●  요구 경험", "■ 근무시간", "자격사항", " [이런 경험이 있으시면 더욱 좋아요]",
            "[꼭 확인해 주세요!]", "□ 경력 요구사항", "□ 우대사항", " [자격 요건]", "[우대조건]"
        ]
    }, 
    {
      "section_name": "우대사항",
      "start_keywords": [
          "우대사항", "우대 사항", "우대 요건", "우대요건", "이런 분이면 더 좋아요", "[우대사항]", "[우대 사항]", "[우대 요건]", "[우대요건]", "# 우대사항 ", "  【우대사항】 이러한 분이면 더욱 좋아요", "우대 사항:",
          "이런 분이 오시면 좋아요 [우대사항]", "Benefits ", "우대경험/역량", " 우대조건", "우대사항 ", "우대사항-", "[우대조건]", "저희가 우대하는 분들은 :  ",
          " [이런 경험이 있으시면 더욱 좋아요]", "□ 우대사항", "[우대사항]", "<우대>", "우대조건", "(우대)", "[ 우대 사항 ]"
      ],
      "end_keywords": [
          "합류하면 함께 할 업무예요", "이런 일을 함께하고 싶습니다.", "담당 업무는 다음과 같습니다.", "[담당업무]", "[담당 업무]", "# 담당업무 ", "담당업무 내용", "[담당업무] ",
          "함께 담당하게 될 업무입니다", "Key Responsibilities", "담당 업무", "담당업무", "주요업무", "주요 업무", "[주요 업무]", "[주요업무]", "  [우리는 이런일을 합니다]",
          "담당업무 ", " 담당업무", "담당 업무 ", " 담당 업무", "  【업무소개】 이러한 일을 해요", "주요 업무: ", "이런 업무를 해요 [주요 업무]", " 업무 내용 ",
          "직무상세", "[ 업무 안내 ]", "업무 내용:", "지원자격(필수)", "필수 경험과 역량 ", "자격요건 ", "[필수자격요건]", "필요 자격 및 기술 ",
          "자격요건", "자격 요건", "필수자격", "필수 자격", "필요 역량", "필요역량", "Required Qualifications", "필요역량 및 경험", "기술스택", "[필수 기술]",
          "필수사항", "지원 자격", "[ 필수(자격) 조건 ]", "경험/역량", "지원자격 ", "  자격요건", "[필수 경험과 역량]", "자격요건  ",
          "Bigdata Engineer로 이런 분이 필요합니다", "이런 분과 함께하고 싶습니다.", "이런 기술이 필요해요", "[우리는 이런사람을 원합니다]", "이런 자격을 갖춘 분을 찾고 있어요 [자격 요건]",
          "이런 역량을 가지신 분을 찾습니다.", "이런 분과 함께하고 싶어요", "[자격요건]", "[자격 요건]", "근무지", "# 자격요건","  【자격요건】 이러한 분을 찾고 있어요",
          "전형절차", "이런 조건에서 근무할 예정이에요", "○명", "근무조건", "스킬", "  【업무방향】 이러한 경험도 할 수 있어요", "혜택:", "혜택 및 복지", "기타사항",
          "제출서류", "▶ 지원서는 이렇게 작성하시면 좋아요", "[혜택 및 복지]", "지원자격", "측정", "기타 사항", "경력/기술 요건:", "●  요구 경험",
          "-----------------------------------------------------------------------", "[채용 전형 안내]", "BAT에 합류하는 여정", "기타 참고 사항", "Video Platform Team은 이런 팀입니다",
          "TSS > 기술컨설팅팀", "전형 절차 ", "전형 절차", "0명 ", "0명", "Full Stack Developer", "[모집 절차]", "[Key Responsibilities]", "[Application]",
          "○기타사항", "[근무 조건]", "[기본조건]", "기타사항 ㆍ사원~대리 직급 채용 희망", "[지원 및 진행 절차]", "  [자격 요건]", "재무팀 정규직", "지원 전 확인 부탁드립니다",
          "근무기간 : 6개월 (당사자 간 협의에 따라 연장 가능) / 인턴으로 3개월 근무 후 정규직 전환 평가", " 복리후생", "근무시간", "근무지", "■ 근무시간", "수행업무",
          "□ 기업문화 ", "자격사항", "근무조건 및 지원방법", " [꼭 확인해 주세요!]", "□ 경력 요구사항", "□ 직무상세", "[핵심역량 및 기술]",
      ]
  }
]

# S3에서 파일을 읽고, MySQL에 데이터를 적재
process_and_insert_into_db('t2jt', "job/DE/sources/jumpit/txt/", keyword_groups)
spark.stop()
