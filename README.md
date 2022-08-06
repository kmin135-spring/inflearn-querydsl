# 개요

* 김영한 - QueryDSL

# 메모

* Q 파일 생성하도록 빌드 스크립트 설정하는게 핵심
  * 자동코드 생성을 이용한 기술인 만큼 버전따라 설정이 달라지기도 하니 다소 삽질이 필요한 부분
  * vcs에 포함되지 않도록 주의
  * gradle은 `compileQuerydsl` task 로 빌드하면 Q파일 생성됨

---

* JPQL과 QueryDSL의 가장 큰 차이점은 QueryDSL은 Q Type을 활용해서 컴파일 타임에 문법 오류를 잡을 수 있다는 점