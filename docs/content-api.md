# 콘텐츠 API 구현 범위

기준: 기능 요구 사항 2.4, schema.sql 및 DDL 1.2.

## 엔드포인트

| 메서드 | 경로 | 결과 |
| --- | --- | --- |
| GET | /api/contents | 커서 기반 목록 (200) |
| GET | /api/contents/{contentId} | 상세 (200) |
| POST | /api/contents | 관리자 등록 (201, Location 헤더) |
| PATCH | /api/contents/{contentId} | 관리자 부분 수정 (200) |
| DELETE | /api/contents/{contentId} | 관리자 하드 삭제 (204) |

등록·수정·삭제는 Controller와 Service에서 ROLE_ADMIN을 검사한다.
현재 공통 SecurityConfig에는 JWT 인증 필터가 없다. 관리자 인증 공급자가 연결되어야 실제 HTTP 관리자 호출이 가능하다.
조회 공개 여부는 현재 프로젝트의 개발용 보안 정책을 따른다.

## 목록

- typeEqual: movie / tvSeries / tvSeason / sport. tvSeries 요청은 시즌 콘텐츠를 반환한다.
- 타입을 지정하지 않은 목록도 시리즈 컨테이너를 제외하고 영화·시즌·스포츠를 반환한다.
- genreIdEqual, sportTypeEqual, likedByUserIdEqual 필터를 함께 적용할 수 있다.
- keywordLike: 제목·설명·타입·메타데이터·장르·태그·출연진 기본 부분 검색. %, _는 문자 자체로 검색한다.
- sortBy: createdAt (기본값) / averageRating.
- sortDirection: DESCENDING (기본값) / ASCENDING.
- limit: 1~100, 기본 20.
- 응답의 nextCursor와 nextIdAfter를 다음 요청의 cursor와 idAfter로 함께 전달한다.
- cursor는 createdAt 정렬이면 ISO-8601 Instant, averageRating 정렬이면 소수 평점이다.
- 다음 페이지에서도 동일한 필터와 정렬을 유지한다. totalCount는 커서 적용 전 필터에 해당하는 전체 수다.
- 동점일 때 id를 동일 방향으로 정렬한다. 평점 변경 중의 페이지 간 스냅샷 일관성까지 보장하지는 않는다.

## 관리자 요청

등록 예:

```json
{
  "title": "새 영화",
  "type": "movie",
  "description": "소개",
  "tags": ["성장", "가족애"]
}
```

시즌은 parentContentId와 seasonNumber가 필요하며 부모가 실제 tvSeries인지 확인한다.
동일 부모·시즌 번호는 중복 등록할 수 없다. DB 유니크 제약이 최종 방어선이다.
스포츠는 sportType을 필수로 지정한다.

수정 가능한 필드는 title, description, type, thumbnailUrl, tags다.
null은 기존 값 유지다. tags=[]는 MANUAL 연결만 제거하며 AI/EXTERNAL 연결을 보존한다.
동일 태그에 여러 출처를 중복 저장할 수 없는 DDL의 UNIQUE(content_id, tag_id)를 따른다.
하위 시즌·회차가 있는 콘텐츠는 유형을 변경할 수 없다.
새 부모 정보·스포츠 종목을 받을 수 없는 현재 수정 DTO로는 다른 유형을 tvSeason/sport로 변경할 수 없다.

thumbnailUrl은 업로드가 완료된 이미지 URL을 받는 필드다. 이 API가 이미지를 업로드하거나 외부 URL을 다운로드하지 않는다.

## 상세 및 삭제

상세에 시즌·회차·장르·태그·출연진·OTT·플레이리스트·Watch Party를 포함한다.
플레이리스트는 구독자 수 내림차순, 생성 시각 오름차순이다.
Watch Party는 종료된 방과 시작 후 1시간 이상 지난 방을 제외한다.
시작한 지 1시간 미만인 방을 우선하고, 예정 방은 시작 시각과 리마인더 수로 정렬한다.

삭제 시 대상 콘텐츠와 하위 시즌을 잠그고 각 콘텐츠의 Watch Party 존재를 확인한다.
종료된 Watch Party도 DDL의 RESTRICT에 따라 삭제를 막는다.
삭제는 단일 쿼리와 DB CASCADE로 처리하고 영속성 컨텍스트를 정리한다.
schema.sql의 전체 연결 테이블과 FK가 적용되어 있어야 한다. ddl-auto:update만으로는 Entity가 없는 테이블이 생성되지 않는다.

삭제 후 ContentDeletedEvent에 하위 시즌을 포함한 삭제 ID와 썸네일 URL을 전달한다.
이것은 프로세스 내부 Spring 이벤트이며 Kafka content.deleted 토픽 발행이 아니다.
향후 소비자는 @TransactionalEventListener(phase = AFTER_COMMIT)로 연결해야 한다.
외부 전달의 내구성을 위해서는 outbox/재처리 설계가 추가로 필요하다.

## 후속 연동

- S3 업로드 및 삭제 소비자
- Kafka content.deleted 전달, OpenSearch·임베딩·Redis 정리
- OpenSearch 고급/의미 검색, 외부 API·Batch·AI 태깅
- 실제 MySQL 및 인증 공급자와의 통합 실행 검증

현재 DDL에는 OTT 정액제/대여/구매 구분 컬럼이 없으므로 응답에 그 값을 만들어 넣지 않는다.
DB 상세 조회에는 저장된 OTT 매핑을 사용하며 한국 제공 여부는 수집 단계에서 보장해야 한다.

## 검증

core와 app-api 전체 테스트를 실행했다.
권한·오류 응답·커서 응답·타입 검증은 Spring/MockMvc 테스트로 확인했다.
시즌 필터·동점 커서·태그 출처 보존·CASCADE·검색·Watch Party 정렬은 H2 MySQL 모드에서 검증했다.
실제 MySQL에서 실행한 테스트는 아니다.
