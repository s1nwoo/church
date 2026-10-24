# 예배 구분

`sermon_video.worship_type`에 `SUNDAY`(주일예배), `AFTERNOON`(오후예배)를 저장합니다.
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

유튜브 자동등록은 썸네일에 명시된 예배 구분을 함께 추출합니다. 구분을 확인할 수 없는 영상은 자동등록하지 않으며 수동 등록할 수 있습니다.
일요일 저녁 재시도는 당일 주일예배와 오후예배가 모두 등록된 경우에만 생략합니다.

## 검증

테스트는 외부 API와 실제 DB를 사용하지 않습니다. 기존 스키마의 영상 보존, 기본값, 구분별 검색·페이지네이션,
관리자 수정, 자동분류 응답과 저녁 재시도를 확인합니다.

```powershell
.\gradlew.bat test --tests '*Sermon*Test' --tests '*YoutubeImportSchedulerTest'
```
