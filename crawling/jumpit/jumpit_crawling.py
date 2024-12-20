import os
import time
import uuid
import boto3
import pymysql
from datetime import datetime, timedelta
from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from webdriver_manager.chrome import ChromeDriverManager
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from urllib.parse import urlparse, parse_qs
import re

output_folder = "links"
today = datetime.today().strftime('%Y%m%d')
log_file_name = os.path.join(output_folder, f"{today}.log")

def save_crawled_content(url, content):
    file_name = url.split('/')[-1] + ".txt"
    file_path = os.path.join('textnotice', file_name)
    
    if not os.path.exists('textnotice'):
        os.makedirs('textnotice')
    
    with open(file_path, 'w', encoding='utf-8') as file:
        file.write(content)
    print(f"Content saved to {file_path}")

def insert_into_db(data, connection): 
    with connection.cursor() as cursor:
        sql = """
        INSERT INTO airflowT (site, job_title, due_type, due_date, company, post_title, org_url, s3_text_url, notice_type, create_time, update_time)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        """
        cursor.execute(sql, (
            data['site'], data['job_title'], data['due_type'], data['due_date'], data['company'],
            data['post_title'], data['org_url'], data['s3_text_url'], data['notice_type'],
            data['create_time'], data['update_time']
        ))
        connection.commit()

def update_removed_links_in_db(removed_links, connection):
    try:
        with connection.cursor() as cursor:
            for link in removed_links:
                sql = """
                UPDATE airflowT SET removed_time = %s WHERE org_url = %s
                """
                cursor.execute(sql, (datetime.now().strftime("%Y-%m-%d %H:%M:%S"), link))
            connection.commit()
            print(f"Updated removed links in DB: {len(removed_links)}")
    except Exception as e:
        print(f"Error updating removed links in DB: {e}")

def read_links_and_ddays_from_s3(bucket_name, s3_file_key):
    s3 = boto3.client('s3')
    try:
        response = s3.get_object(Bucket=bucket_name, Key=s3_file_key)
        content = response['Body'].read().decode('utf-8')
        
        links_with_ddays = {}
        for line in content.splitlines():
            if ": " in line:
                link, d_day = line.strip().split(": ", 1)
                link = re.sub(r':\s*\d+$', '', link)
                
                try:
                    d_day_int = int(d_day)
                    links_with_ddays[link] = d_day_int
                except ValueError:
                    print(f"Invalid D-day value: {d_day} for URL: {link}")
        
        return links_with_ddays
    except Exception as e:
        print(f"Error reading file from S3: {e}")
        return {}

def upload_to_s3(file_path, bucket_name, object_name):
    s3 = boto3.client('s3')
    try:
        s3.upload_file(file_path, bucket_name, object_name)
        print(f"Uploaded {file_path} to S3 bucket {bucket_name} as {object_name}")
        return f"s3://{bucket_name}/{object_name}"
    except Exception as e:
        print(f"Error uploading file to S3: {e}")
        return None

def extract_site_name(url):
    parsed_url = urlparse(url)
    domain = parsed_url.hostname
    return domain.split('.')[0] if domain else None

def ensure_directories():
    os.makedirs("links", exist_ok=True)

def calculate_deadline_from_dday(d_day):
    today = datetime.now().date()
    deadline = today + timedelta(days=d_day)
    return deadline

def update_log_file(url, crawl_time):
    with open(log_file_name, 'r', encoding='utf-8') as file:
        lines = file.readlines()

    updated_lines = []
    
    for line in lines:
        columns = line.strip().split(',')
        if columns[0] == url:
            columns[2] = "done"
            columns[3] = crawl_time
            updated_line = ','.join(columns)
            updated_lines.append(updated_line + '\n')
        else:
            updated_lines.append(line)
    
    with open(log_file_name, 'w', encoding='utf-8') as file:
        file.writelines(updated_lines)

ensure_directories()

bucket_name = 't2jt'
today_file_key = f"job/DE/airflow_test/jumpit/links/{today}.txt"

links_with_ddays = read_links_and_ddays_from_s3(bucket_name, today_file_key)

urls_to_crawl = list(links_with_ddays.keys())

connection = pymysql.connect(
    host='43.201.40.223',
    user='user',
    password='1234',
    database='testdb',
    port=3306
)

options = Options()
options.headless = False
driver = webdriver.Chrome(service=Service(ChromeDriverManager().install()), options=options)

with open(log_file_name, 'r', encoding='utf-8') as file:
    lines = file.readlines()

    removed_links = []

    for line in lines[1:]:
        columns = line.strip().split(',')
        url = columns[0]
        notice_status = columns[1]
        work_status = columns[2]
        done_time = columns[3]
        d_day = columns[4]  # D-day 값을 가져옴
        
        # D-day 값이 숫자인 경우 deadline 계산
        try:
            d_day_int = int(d_day)
            deadline = calculate_deadline_from_dday(d_day_int)
        except ValueError:
            deadline = None  # D-day 값이 유효하지 않으면 deadline은 None
        
        if notice_status == "deleted":
            removed_links.append(url)
        elif notice_status == "update" and work_status == "null":
            print(f"Starting crawl for {url}")
            crawl_time = datetime.now().strftime('%Y-%m-%d %H:%M:%S')

            # Open the URL and perform web scraping directly
            try:
                driver.get(url)
                time.sleep(3)  # Wait for the page to load

                # Extract job content
                job_content_text = None
                try:
                    job_content_section = WebDriverWait(driver, 10).until(
                        EC.presence_of_element_located((By.CSS_SELECTOR, ".sc-10492dab-3.hiVlDL"))  # Adjust this selector to match the correct element
                    )
                    job_content_text = job_content_section.text
                except Exception:
                    print(f"Failed to extract job content from {url}")
                
                # If content is found, save to file and upload to S3
                if job_content_text:
                    text_path = os.path.join("texts", f"{uuid.uuid4()}.txt")
                    with open(text_path, "w", encoding="utf-8") as f:
                        f.write(job_content_text)
                    s3_text_url = upload_to_s3(text_path, bucket_name, f"job/DE/airflow_test/{extract_site_name(url)}/txt/{uuid.uuid4()}.txt")
                
                # Extract additional info (like company name, post title)
                company_name = None
                post_title = None
                try:
                    company_link = WebDriverWait(driver, 10).until(
                        EC.presence_of_element_located((By.CSS_SELECTOR, "a.name"))
                    )
                    company_name = parse_qs(urlparse(company_link.get_attribute("href")).query).get("company_nm", [None])[0]

                    post_title_element = WebDriverWait(driver, 10).until(
                        EC.presence_of_element_located((By.CSS_SELECTOR, "h1"))
                    )
                    post_title = post_title_element.text
                except Exception:
                    pass

                # Determine due type
                due_type = '상시채용' if not deadline else '날짜'
                due_date = deadline if deadline else None

                # Prepare data for DB insertion
                data = {
                    'site': extract_site_name(url),
                    'job_title': '데이터 엔지니어',
                    'due_type': due_type,
                    'due_date': due_date,
                    'company': company_name,
                    'notice_type': 'text',
                    'post_title': post_title,
                    'org_url': url,
                    's3_text_url': s3_text_url,
                    's3_image_url': None,
                    'create_time': datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                    'update_time': datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                }

                insert_into_db(data, connection)  # Insert the data into the DB
                update_log_file(url, crawl_time)  # Update log file

            except Exception as e:
                print(f"Error processing {url}: {e}")

    if removed_links:
        update_removed_links_in_db(removed_links, connection)  # Update removed links in DB

driver.quit()
connection.close()
