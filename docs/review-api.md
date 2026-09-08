# 리뷰 API

## 엔드포인트

| 메서드 | 경로 | 결과 |
| --- | --- | --- |
| GET | /api/reviews | 필터·커서 목록, 200 |
| GET | /api/reviews/{reviewId} | 단건 조회, 200 |
| POST | /api/reviews | 인증 사용자 등록, 201 + Location |
| PATCH | /api/reviews/{reviewId} | 작성자 부분 수정, 200 |
| DELETE | /api/reviews/{reviewId} | 작성자 삭제, 204 |

조회는 기존 개발용 공개 조회 정책을 따른다.
등록·수정·삭제의 사용자 ID는 ReviewActor가 서버의 SecurityContext에서 얻는다.
현재 연결 규약은 Authentication.getName()이 사용자 UUID인 것이다.
JWT 인증 필터는 아직 없으므로 실제 인증 공급자를 구현할 때 이 연결 규약을 맞추거나 ReviewActor를 수정해야 한다.
요청 본문·쿼리·사용자 지정 헤더의 ID를 작성자 인증으로 사용하지 않는다.

## 요청

등록 예:

```json
{
  "contentId": "콘텐츠 UUID",
  "reviewText": "리뷰 내용",
  "rating": 4.5,
  "spoiler": false
}
```

평점은 0~5, 0.5점 단위다. 본문과 평점은 등록 시 필수다.
한 사용자·콘텐츠당 리뷰 하나만 허용한다.
수정은 reviewText, rating, spoiler를 받는다. null은 기존 값 유지, spoiler:false는 스포일러 설정 해제다.
작성자와 대상 콘텐츠는 수정할 수 없다. 관리자를 포함해 타인의 리뷰를 수정·삭제할 수 없다.

## 조회와 스포일러

contentIdEqual과 userIdEqual을 함께 사용할 수 있다.
sortBy는 createdAt, sortDirection은 ASCENDING 또는 DESCENDING(기본값)이다.
limit은 1~100, 기본 20이다.
다음 페이지는 응답의 nextCursor, nextIdAfter를 cursor, idAfter로 함께 전달한다.
cursor는 ISO-8601 Instant이며, 생성 시각이 같으면 UUID를 보조 정렬 키로 사용한다.
totalCount는 커서 적용 전 필터에 해당하는 전체 리뷰 수다.

조회 기본값 includeSpoilers=false에서는 스포일러 리뷰의 reviewText가 null이다.
사용자가 내용을 보기로 선택하면 includeSpoilers=true로 다시 요청한다.
스포일러 여부·평점 등은 항상 반환한다. 작성·수정 직후 응답에는 작성자 본문을 반환한다.

## 트랜잭션과 오류

리뷰 변경은 콘텐츠 행 잠금 → 리뷰 변경/flush → 현재 평점 조회(FOR UPDATE) → 콘텐츠 집계 갱신 순서다.
수정·삭제는 콘텐츠 ID만 먼저 확인하고 콘텐츠 잠금 이후 리뷰를 잠금 조회하여 소유자를 검증한다.
콘텐츠·리뷰 잠금 순서를 동일하게 유지한다.
평균은 저장된 개별 평점을 합산해 소수점 둘째 자리 HALF_UP으로 계산한다.
리뷰가 없어지면 평균은 0.00, 리뷰 수는 0이다. 콘텐츠 좋아요 수는 변경하지 않는다.
현재 집계는 리뷰 수에 비례해 평점 목록을 읽는다. 대량 데이터에서는 합계 컬럼 또는 집계 쿼리 방식의 최적화를 검토한다.

- 400: 잘못된 DTO·커서
- 401: 인증 없음 또는 현재 사용자 UUID 연결 실패
- 403: 타인의 리뷰 변경
- 404: 사용자·콘텐츠·리뷰 없음
- 409: 중복 리뷰

DB UNIQUE(user_id, content_id)가 최종 중복 방어선이다. 해당 제약 위반만 중복 리뷰 오류로 변환한다.
리뷰 변경과 콘텐츠 집계는 같은 DB 트랜잭션에서 커밋/롤백된다.
Kafka 추천·검색 이벤트 연동은 이번 구현에 포함하지 않았다.

## 검증

H2 MySQL 모드에서 실제 Spring HTTP·Service·Repository 통합 테스트를 실행했다.
등록·수정·삭제 집계, 중복 차단, 타인 권한, 익명 차단, 스포일러 공개 옵션,
커서/필터 및 동시 등록 시 평균·개수를 검증했다.
실제 MySQL과 JWT 발급/검증을 포함하는 통합 검증은 후속 작업이다.
