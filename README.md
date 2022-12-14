# 개요

* 김영한 - QueryDSL

# 메모

* Q 파일 생성하도록 빌드 스크립트 설정하는게 핵심
  * 자동코드 생성을 이용한 기술인 만큼 버전따라 설정이 달라지기도 하니 다소 삽질이 필요한 부분
  * vcs에 포함되지 않도록 주의
  * gradle은 `compileQuerydsl` task 로 빌드하면 Q파일 생성됨

---

* JPQL과 QueryDSL의 가장 큰 차이점은 QueryDSL은 Q Type을 활용해서 컴파일 타임에 문법 오류를 잡을 수 있다는 점
* QueryDSL은 JPQL Builder다

---

## fetchCount, fetchResult

* 220806 기준 deprecated 된 기능
* 직접 fetch + count 하자

> 김영한님 조언 (https://inf.run/A8Gn)
>> fetchCount, fetchResult는 둘다 querydsl 내부에서 count용 쿼리를 만들어서 실행해야 하는데, 이때 작성한 select 쿼리를 기반으로 count 쿼리를 만들어냅니다. 그런데 이 기능이 select 구문을 단순히 count 처리하는 것으로 바꾸는 정도여서, 단순한 쿼리에서는 잘 동작하는데, 복잡한 쿼리에서는 잘 동작하지 않습니다. 이럴때는 명확하게 카운트 쿼리를 별도로 작성하고, 말씀하신 대로 fetch()를 사용해서 해결해야 합니다.

## 서브쿼리 한계

* JPA JPQL 에서는 from 절의 서브쿼리(인라인 뷰)는 지원하지 않는다.
  * 대안
    * 서브쿼리를 join으로 변경 (상황에 따라 가능여부가 다름)
    * 쿼리를 2번 이상 나눠서 수행
    * 최후의 수단으로 nativeSQL 사용
  * 다만 애초에 from 절 서브쿼리가 방대하다면 쿼리 한 방에 모든걸 해결하려고 했을 가능성이 높다.
  * 방대한 1방의 쿼리보다는 n번을 나눠서 수행하는게 전체 쿼리양도 줄어들고 재활용할 수 있을 가능성도 높다.
    * 내의견) 방대한 만능 쿼리 1개는 보통 특정 기능에 강결합되어있을 가능성이 높다. 너무 길다보니 만든사람도 다시 보면 해독하는데 한참이 걸리기도 한다.
    * 현대적인 애플리케이션에서는 쿼리는 단순하게 가져가고 복잡한 부분은 애플리케이션에서 풀어가는게 좋다고 본다.
  * 연관도서 : SQL AntiPatterns
* JPQ 표준은 select 절 서브쿼리도 안 되지만 하이버네이트는 지원한다.

## case 문

* 예제에서는 age에 따른 case문을 다뤘는데 `basicCase, complexCase`
* 기본적으로 쿼리에서 이런 비즈니스 적인 변환 작업을 하는건 좋지 않다.
* 어디까지나 rawdata 를 추출하는 관점에서 사용하고
* 비즈니스적으로 의미있는 변환작업은 애플리케이션에서 하자
* case 문 같은 건 애플리케이션에서 처리하는 것보다 확실한 효용이 있다고 판단될 때 사용해야한다.

## 동적 쿼리

* BooleanBuilder : 복잡해질수록 가독성이 낮아짐
* where 다중 파라미터 사용 : 높은 가독성. 만들어둔 조건용 메서드들을 조합하거나 재활용하기에도 용이함
  * 단점은 조건마다 메서드가 추가되야하는 점인데 한 번 만들어두면 잘 수정할일이 없는 부분이라 감수할만한하다.

## 사용자 정의 리포지토리

* QueryDSL이 제공해주는 스펙으로 사용자 정의 리포지토리를 만드는 게 기본방법
* 하지만 아예 별개로 명시적으로 별개의 Repository 를 정의해서 쓰는 것도 설계관점에서 고민
  * 특정 화면이나 API 에 종속적인 repository 라거나
  * 기본 리포지토리가 너무 비대해져서 쪼개고 싶다거나

## spring-data-jpa 가 제공하는 querydsl 기능

* 간단한 구조에서는 사용 가능하나 실무에서 활용하기에는 어려움
* 조인이 안 됨 (묵시적 조인은 가능하나 left join 불가능)
* 클라이언트가 QueryDsl에 의존해야함
* 컨트롤러에 querydsl 종속성이 생김
* QuerydslPredicateExecutor, QueryDsl Web 지원

## 직접 만들어보는 support 클래스

* Querydsl4RepositorySupport
* 5.x에서 fetchResults, fetchCount 가 deprecated 되었으므로 고쳐쓰거나 제한적으로 써야할듯.
* 개인적으로는 몇 줄 더 줄이기보다는 QueryDsl 기본 기술만으로 사용하는 MemberRepositoryImpl 쪽이 안정적이라고 생각함