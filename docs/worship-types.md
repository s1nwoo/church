# 예배 구분

`sermon_video.worship_type`에 `SUNDAY`(주일예배), `AFTERNOON`(오후예배), `COMMUNITY`(공동체영상)를 저장합니다.
등록·수정 API의 JSON 필드는 `worshipType`이며, 목록 API는 같은 이름의 선택적 쿼리 파라미터로 필터링합니다.
필터가 없으면 전체 예배를 조회하며, 검색·삭제 여부·페이지네이션과 함께 적용됩니다.

## 기존 DB와 배포

현재 local / docker / prod 프로필은 모두 Hibernate `ddl-auto: update`를 사용합니다.
백엔드 시작 시 `varchar(20) default 'SUNDAY' not null` 컬럼이 추가되어 기존 영상은 주일예배로 유지됩니다.
백엔드를 먼저 배포하고 새 컬럼 및 API 응답을 확인한 뒤 프론트엔드를 배포합니다.
운영 설정에서 자동 스키마 변경을 끈 경우에는 아래 SQL을 컬럼이 없는 상태에서 한 번 실행해야 합니다.

```sql
ALTER TABLE sermon_video
  ADD COLUMN worship_type VARCHAR(20) NOT NULL DEFAULT 'SUNDAY';
```

기존 영상 중 실제 오후예배 영상이 있다면 관리자 설교 관리에서 해당 영상을 수정해 예배 구분을 변경합니다.
신규 등록 시 예배 구분을 선택할 수 있고, 관리 목록의 필터로 구분별 확인이 가능합니다.

## 공동체영상과 화면

관리자 메뉴는 ‘영상 관리’로 통합합니다. 영상 제목, 날짜, 유튜브 링크는 공통 필수 항목입니다.
공동체영상은 설교자·성경본문 입력 없이 등록하며 상세 화면에서도 설교본문 영역을 표시하지 않습니다.
기존 `preacher NOT NULL` 컬럼과의 호환성을 위해 공동체영상의 설교자는 빈 문자열, 성경본문은 null로 저장합니다.
주일·오후예배는 계속 설교자 입력이 필수입니다. 공동체영상은 관리자 수동 등록으로 관리합니다.

물리 테이블명 `sermon_video`, API `/api/sermons`, ID를 유지하여 기존 링크와 데이터를 보존합니다.
공동체 구분 추가에는 컬럼이나 테이블 재생성이 필요하지 않습니다 (`worship_type`은 VARCHAR).
향후 물리 명칭을 `video`로 변경할 때는 Hibernate 자동 생성 대신 별도 마이그레이션을 사용해야 합니다.

`/worship-media?tab=sunday|afternoon|community`는 해당 유형의 최신 영상 상세를 표시합니다.
`/worship-media/:id`는 지정한 영상 상세, `/worship-media/all?tab=...`은 전체 목록을 표시합니다.
탭 클릭은 항상 최신 상세로 이동하고 ‘전체목록’ 버튼만 해당 구분의 목록으로 이동합니다.

유튜브 자동등록은 썸네일에 명시된 예배 구분을 함께 추출합니다. 구분을 확인할 수 없는 영상은 자동등록하지 않으며 수동 등록할 수 있습니다.
일요일 저녁 재시도는 당일 주일예배와 오후예배가 모두 등록된 경우에만 생략합니다.

## 검증

테스트는 외부 API와 실제 DB를 사용하지 않습니다. 기존 스키마의 영상 보존, 기본값, 구분별 검색·페이지네이션,
관리자 수정, 자동분류 응답과 저녁 재시도를 확인합니다.

```powershell
.\gradlew.bat test --tests '*Sermon*Test' --tests '*YoutubeImportSchedulerTest'
```
