# 성경 PPT 관리자 기능

- 화면: `/admin/bible-ppt` (관리자 메뉴의 **성경 PPT 만들기**)
- 관리자 로그인에서 사용하는 `accessToken`을 Bearer 헤더로 전송합니다.
- 모든 `/api/admin/bible-ppt/**` API는 서버에서 `ROLE_ADMIN`을 검사합니다.

## 사용 방법

1. `창1:1-5`, `창1:1,3,5`, `창1:1~3 요3:16` 형식으로 구절을 입력합니다.
2. 한 장의 최대 줄 수를 선택하고 슬라이드를 만듭니다. `0`은 자동 분할을 하지 않습니다.
3. 목록의 체크박스로 여러 슬라이드를 골라 합칠 수 있습니다. 합친 슬라이드는 합치기 전 내용으로 나눌 수 있습니다.
4. 제목·본문을 편집하고 미리보기의 점선 상자를 끌거나 숫자를 입력해 크기·위치를 조절합니다.
5. 전체 설정 또는 슬라이드별 개별 설정을 적용하고 PPT를 다운로드합니다.

다시 만들기는 기존 슬라이드 편집을 대체합니다. 전체 스타일은 ‘전체 설정 저장’으로 현재 브라우저에 기억할 수 있습니다. 편집한 슬라이드 자체는 서버나 브라우저에 저장되지 않으므로 페이지를 벗어나기 전에 다운로드하세요.

미리보기는 Apache POI로 실제 PPT와 같은 생성 코드를 사용해 PNG로 그립니다. 다른 PC의 PowerPoint에서는 Pretendard Black 설치 여부와 PowerPoint의 텍스트 렌더링에 따라 차이가 날 수 있습니다. 긴 본문이나 합친 슬라이드는 미리보기를 보고 글자 크기·상자 크기를 조정하세요.

## 구성과 배포

기존 BiblePPT의 구절 해석, 분할, PPT 생성 코드를 `com.banghwa.bibleppt`로 옮겼습니다. JavaFX UI와 PC 파일 경로 의존성은 포함하지 않습니다. 홈페이지의 `data/bible-*.json`을 재사용합니다.

`src/main/resources/bible-ppt/`에는 기존 PPT 템플릿, Pretendard Black, 원본 프로그램과 글꼴의 라이선스를 포함합니다. 리소스는 Spring Boot JAR에 포함되어 로컬 경로 설정이 필요하지 않습니다. 새 DB 테이블이나 환경변수도 필요하지 않습니다.

서버에는 Apache POI 의존성이 추가되어 **백엔드와 프런트엔드를 모두 배포**해야 합니다. Linux에서 AWT 미리보기를 위해 fontconfig/freetype이 필요합니다. Dockerfile에는 Alpine 패키지 설치를 반영했습니다. Docker 없이 운영하는 서버도 해당 시스템 라이브러리가 설치되어 있어야 합니다.

API:

| 메서드 | 경로 | 용도 |
| --- | --- | --- |
| GET | `/api/admin/bible-ppt/metadata` | 성경 목록, 템플릿 크기, 기본 스타일 |
| POST | `/api/admin/bible-ppt/slides` | `{reference, lineLimit}`로 슬라이드 생성 |
| POST | `/api/admin/bible-ppt/preview` | `{title, body, style}`을 PNG로 렌더링 |
| POST | `/api/admin/bible-ppt/download` | `{slides: [...]}`를 PPTX로 다운로드 |

입력은 2,000자, 구절/슬라이드는 200개, 슬라이드 본문은 8,000자, 전체 본문은 100,000자로 제한합니다. 잘못된 구절이 포함되어 있으면 일부만 생성하지 않고 오류를 반환합니다.

## 검증

- 백엔드 전체 테스트는 아래 임시 H2 설정으로 실행했습니다. 기존 `CommunityServiceTest`는 설정된 DB에 게시글을 저장하므로 운영/개발 DB 연결 설정 그대로 실행하지 않습니다.
- `npm.cmd run build`: 프런트엔드 운영 빌드
- `npm.cmd test -- --watchAll=false --runInBand`: 새 편집 흐름, 관리자 메뉴에서 새 경로로의 이동, 기존 관련 화면 테스트 (21개)

백엔드 PowerShell 명령 (임시 H2 사용, 전체 22개 테스트 및 JAR 빌드):

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:h2:mem:biblepptqa;MODE=MySQL;DB_CLOSE_DELAY=-1'
$env:SPRING_DATASOURCE_DRIVER_CLASS_NAME='org.h2.Driver'
$env:SPRING_DATASOURCE_USERNAME='sa'
$env:SPRING_DATASOURCE_PASSWORD=''
$env:SPRING_JPA_DATABASE_PLATFORM='org.hibernate.dialect.H2Dialect'
$env:SPRING_JPA_HIBERNATE_DDL_AUTO='create-drop'
.\gradlew.bat test bootJar --no-daemon
```

이 환경변수는 테스트용 셸에서만 사용하고, 개발/운영 서버 실행 시에는 기존 DB 설정을 사용합니다. 검사 항목은 실데이터 구절 처리, 범위 제한, 편집 내용·개별 스타일·슬라이드 크기, PNG 생성, 관리자 접근 제어를 포함합니다.

샘플 PPT와 PNG는 백엔드 테스트 시 `build/bible-ppt-qa/`에 생성됩니다. 기본 템플릿의 4:3 비율을 유지하며, 웹 편집 상자는 페이지 안에 맞추고 본문 상자를 여러 줄 높이로 확장합니다.
