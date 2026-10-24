# 관리자 전용 게시글과 첨부파일

`/admin/posts`의 **관리자 자료 올리기**는 공개 범위를 ‘관리자만 보기’로 시작합니다. 일반 ‘등록’은 기존처럼 전체 공개입니다. 등록·수정 창에서 공개 범위를 바꿀 수 있으며, 관리자 전용 글은 모든 관리자 계정이 조회·다운로드할 수 있습니다.

- 공개 소식 목록과 검색은 관리자 전용 글을 항상 제외합니다. 게시글·파일 주소를 직접 요청해도 비관리자에게는 404를 반환합니다.
- 목록·저장·수정·삭제와 다운로드에는 관리자 로그인 토큰을 전송합니다. 서버에서도 권한을 검사합니다.
- 파일은 저장 버튼을 누를 때 글과 함께 업로드합니다. 등록 취소는 업로드 요청을 보내지 않습니다. DB 저장 실패 시 새로 업로드한 파일을 정리합니다.
- 관리자 전용 글 삭제는 DB의 글·첨부 메타데이터와 실제 파일을 삭제합니다. 공개 글은 기존 소프트 삭제를 유지하되 실제 첨부파일과 첨부 메타데이터는 삭제합니다.
- 실제 파일 삭제에 실패하면 삭제 요청은 오류가 되고 메타데이터를 유지하므로 재시도할 수 있습니다. 여러 파일 중 일부가 이미 삭제됐다면 재시도 시 존재하지 않는 파일은 건너뜁니다.
- 글 수정에서 체크한 첨부파일 삭제도 저장 시 적용합니다. 취소하면 기존 파일을 유지합니다.

## 저장 위치와 제한

기본 경로는 `${user.home}/.banghwa-church/post-attachments`입니다. 운영 서버에서는 `/home/ubuntu/.banghwa-church/post-attachments`를 사용하며, JAR·정적 리소스·S3 공개 이미지 폴더와 분리되어 있습니다. `posts.attachments.directory` 또는 `POSTS_ATTACHMENTS_DIRECTORY`로 변경할 수 있습니다. Linux에서 생성 디렉터리는 700, 파일은 600 권한입니다.

UUID 파일명으로 저장하고 원래 파일명은 DB에 보관합니다. 다운로드는 게시글 권한을 확인한 후 인증된 API로 제공하며, 원래 파일명과 `application/octet-stream`, `Content-Disposition: attachment`, `Cache-Control: no-store`, `X-Content-Type-Options: nosniff`를 사용합니다. 저장 경로나 공개 URL은 응답하지 않습니다.

파일당 최대 100MB, 게시글당 최대 10개, 한 번에 업로드하는 파일 총합은 300MB입니다. Spring multipart 요청 한도는 메타데이터 여유를 포함해 310MB입니다. 운영 Nginx의 `api.bhbc.kr` 가상 호스트에도 `client_max_body_size 310m;`를 적용해야 합니다.

## 데이터 변경과 배포

`post.admin_only`는 `boolean default false not null`로 추가해 기존 글은 계속 공개 상태를 유지합니다. `post_attachment` 테이블에 파일 메타데이터와 게시글 외래 키를 저장합니다. 기존 Hibernate schema update 방식으로 적용하며, 변경 전 스키마에서 업데이트 후 기존 글을 읽는 테스트를 포함합니다.

API는 공개 `/api/posts`, 관리자 `/api/admin/posts` 목록, multipart 관리자 등록·수정, `/api/posts/{postId}/attachments/{id}` 다운로드, 기존 `/api/posts/{id}` 삭제를 사용합니다. 첨부가 있는 글의 공개 범위를 바꾸면 해당 파일의 열람 범위도 함께 바뀝니다.

검증: `PrivatePostIntegrationTest`는 실제 H2 DB·임시 폴더를 사용해 비공개 검색·직접 접근 차단, 권한 검사, 다운로드, 수정, 실제 파일 삭제, 삭제 실패, DB 롤백 파일 정리, 용량·경로 제한을 확인합니다. `PostSchemaUpgradeTest`는 기존 게시글 스키마에서의 변경을 검증합니다. 화면 테스트는 비공개 기본값·저장·취소·다운로드·첨부 삭제 선택·삭제 실패를 확인합니다.
