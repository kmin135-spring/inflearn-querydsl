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
