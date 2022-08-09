package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QueryDslBasicTest {
    @Autowired
    EntityManager em;

    // 쓰레드 안전하므로 필드로 세팅해두고 쓰면됨
    JPAQueryFactory query;

    @BeforeEach
    void setupData() {
        query = new JPAQueryFactory(em);
        Team teamA = Team.of("teamA");
        Team teamB = Team.of("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = Member.of("member1", 10, teamA);
        Member member2 = Member.of("member2", 20, teamA);
        Member member3 = Member.of("member3", 30, teamB);
        Member member4 = Member.of("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL() {
        // find member1
        Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQueryDsl() {
        // 여기에 지정한 이름을 JPQL의 alias로 쓴다.
        // 같은 테이블을 조인할 때는 다른 이름의 QType 들을 만들어서 하면 된다.
        // 그 외에는 가독성을 위해 startQueryDsl2() 처럼 static import 해서 사용하자
        QMember m = new QMember("m");

        Member findMember = query
                .select(m)
                .from(m)
                .where(m.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQueryDsl2() {
        // 이처럼 Q Type에 static 정의된 alias를 사용하면 코드가 심플해진다.
        Member findMember = query
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() {
        Member findMember = query.selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam() {
        /*
        and 조건만 있는 경우 where 의 파라미터로 처리할 수 있다.
        이 방식의 장점은 파라미터가 null인 경우 생략되기 때문에
        동적 쿼리 작성에 유리하다.
         */
        Member findMember = query.selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void resultFetch() {
//        List<Member> fetch = query.selectFrom(member).fetch();
//        Member fetchOne = query.selectFrom(member).fetchOne();
//        Member fetchFirst = query.selectFrom(member).fetchFirst();

        // 강의 시점과 달리 220806 기준에서는 페이징 관련 기능은 deprecated
        // 자동생성되는 count 쿼리의 한계 때문이라고 함
        // javadoc 을 읽어보면 Blaze-Persistence 라는 querydsl 확장을 쓰라고 되어있음
        // 추가로 강사님도 자동생성 count 쿼리는 한계가 있어 복잡한 쿼리에서는 별도로 쿼리할 것을 권장했음.
        query.selectFrom(member).fetchResults();
        query.selectFrom(member).fetchCount();
    }

    /**
     * 1. 회원 나이 내림차순
     * 2. 회원 이름 올림차순
     * - 단, 회원 이름이 없으면 마지막에 출력
     */
    @Test
    void sort() {
        em.persist(Member.of(null, 100));
        em.persist(Member.of("member5", 100));
        em.persist(Member.of("member6", 100));

        List<Member> fetch = query
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member mem5 = fetch.get(0);
        Member mem6 = fetch.get(1);
        Member memNull = fetch.get(2);

        assertThat(mem5.getUsername()).isEqualTo("member5");
        assertThat(mem6.getUsername()).isEqualTo("member6");
        assertThat(memNull.getUsername()).isNull();
    }

    @Test
    void paging1() {
        List<Member> fetch = query.selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(fetch.size()).isEqualTo(2);
    }

    /**
     * deprecated 되었지만 강의대로 해봄
     */
    @Test
    void paging2() {
        QueryResults<Member> queryResults = query.selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
    }

    /**
     * fetch와 count를 직접 수행
     * spring-data의 Page 로 생성하여 동일 구조로 테스트
     */
    @Test
    void paging3() {
        List<Member> content = query.selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        Long count = query.select(member.count()).from(member).fetchOne();

        Page<Member> page = new PageImpl<>(content, PageRequest.of(1, 2), count);

        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(page.getPageable().getPageSize()).isEqualTo(2);
        assertThat(page.getPageable().getPageNumber()).isEqualTo(1);
        assertThat(page.getContent().size()).isEqualTo(2);
    }

    /**
     * <pre>
     * select
     *   count(member0_.member_id) as col_0_0_,
     *   sum(member0_.age) as col_1_0_,
     *   avg(
     *     cast(member0_.age as double)
     *   ) as col_2_0_,
     *   max(member0_.age) as col_3_0_,
     *   min(member0_.age) as col_4_0_
     * from
     *   member member0_;
     * </pre>
     */
    @Test
    public void aggregation() {
        // Tuple은 실무에서는 잘 안 쓴다.
        // mybatis에서 Hashmap으로 받아오는 느낌이라 지양하는게 좋아보임.
        List<Tuple> result = query
                .select(member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min())
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);

    }

    /**
     * 팀의 이름과 각 팀의 평균 연령
     */
    @Test
    public void group() {
        // arrange

        // action
        List<Tuple> result = query.select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        // assert
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo((10 + 20) / 2);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo((30 + 40) / 2);
    }

    /**
     * 팀 A에 소속됨 모든 회원
     */
    @Test
    public void join() {
        List<Member> result = query.selectFrom(member)
//                .join(member.team, team)
//                .innerJoin(member.team, team) // join == innerJoin
                .leftJoin(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

//        assertThat(result.get(0).getUsername()).isEqualTo("member1");
//        assertThat(result.get(1).getUsername()).isEqualTo("member2");

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 세타 조인 (카티션곱)
     * 회원의 이름이 팀 이름과 같은 회원 조인
     *
     * <pre>
     * select
     *   member0_.member_id as member_i1_1_,
     *   member0_.age as age2_1_,
     *   member0_.team_id as team_id4_1_,
     *   member0_.username as username3_1_
     * from
     *   member member0_ cross join team team1_
     * where
     *   member0_.username = team1_.name;
     * </pre>
     */
    @Test
    public void thetaJoin() {
        em.persist(Member.of("teamA"));
        em.persist(Member.of("teamB"));

        List<Member> result = query
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();
        result.forEach(System.out::println);
    }

    /**
     * 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL : select m, t from Member m left join m.team t on t.name = 'teamA'
     */
    @Test
    public void join_on_filtering() {
        // arrange

        // action
        List<Tuple> result = query
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                // 명시적으로 on절을 이용하는 건 보통 외부조인일때 사용
                // 내부조인에도 가능하지만 where 로도 대체가 가능하다.
//                .join(member.team, team).on(team.name.eq("teamA"))
//                .where(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println(tuple);
        }

        // assert
    }

    /**
     * 연관관계 없는 엔티티 외부 조인
     * 회원의 이름이 팀 이름과 같은 대상 외부 조인
     */
    @Test
    public void join_on_no_relation() {
        em.persist(Member.of("teamA"));
        em.persist(Member.of("teamB"));

        /*
        -- ...
        inner join team team1_ on (member0_.username=team1_.name);
         */
        List<Tuple> result = query
                .select(member, team)
                .from(member)
                // cross join 할 때는 join에 대상 테이블만 넣어준다
//                .leftJoin(member.team, team)
                .join(team)
                .on(member.username.eq(team.name))
                .fetch();

        result.forEach(System.out::println);
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    void fetchJoinNo() {
        em.flush();
        em.clear();

        Member findMember = query
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        System.out.println(findMember);

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    @Test
    void fetchJoinYes() {
        em.flush();
        em.clear();

        Member findMember = query
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        System.out.println(findMember);

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isTrue();
    }

    /**
     * 나이가 가장 많은 회원 조회
     * <pre>
     * select *
     * from member m
     * where m.age = (select max(age) from member)
     * </pre>
     */
    @Test
    void subQuery() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = query
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                )).fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);
    }

    /**
     * 나이가 평균 이상인 회원 조회
     * <pre>
     * select *
     * from member m
     * where m.age >= (select avg(age) from member)
     * </pre>
     */
    @Test
    void subQueryGoe() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = query
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                )).fetch();

        assertThat(result).extracting("age")
                .containsExactly(30, 40);
    }

    /**
     * in subquery
     * <pre>
     * select *
     * from member m
     * where m.age in (select age from member where age > 10)
     * </pre>
     */
    @Test
    void subQueryIn() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = query
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                )).fetch();

        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }

    @Test
    void selectSubquery() {
        QMember memberSub = new QMember("memberSub");

        List<Tuple> fetch = query
                .select(member.username,
                        select(memberSub.age.avg()).from(memberSub)
                )
                .from(member)
                .fetch();

        fetch.forEach(System.out::println);
    }

    @Test
    public void basicCase() {
        // action
        List<String> fetch = query
                .select(member.age
                        .when(10).then("열짤")
                        .when(20).then("20살")
                        .otherwise("많음"))
                .from(member)
                .fetch();

        fetch.forEach(System.out::println);
        // assert
    }

    @Test
    public void complexCase() {
        // action
        List<String> fetch = query.select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20")
                        .when(member.age.between(21, 30)).then("21~30")
                        .otherwise("많~음"))
                .from(member).fetch();
        // assert

        fetch.forEach(System.out::println);
    }

    /**
     * 실제 쿼리에는 상수 내용이 들어가지 않고
     * 결과에만 A가 붙는다. (QueryDSL이 붙여주는 걸로 보면될듯)
     */
    @Test
    public void constant() {
        // action
        List<Tuple> result = query
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }

    @Test
    public void concat() {
        /*
        stringValue 로 캐스팅
        dbms 에 따라서는 concat하면 자동캐스팅 되기도 하는 부분인데
        queryDSL에서는 컴파일 에러남
        stringValue 는 ENUM 처리에서도 사용된다고함

        select ((member0_.username || '_') || cast(member0_.age as character varying)) as col_0_0_
        -- ...
         */
        // action
        List<String> fetch = query.select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.age.eq(10))
                .fetch();
        fetch.forEach(System.out::println);
    }

    /**
     * 강사님 권장
     * Tuple은 querydsl 종속적인 low level 데이터 구조이므로
     * repository 레벨까지만 사용하고
     * 상위레벨로는 DTO 등으로 변환해서 넘기자
     */
    @Test
    public void tupleProjection() {
        // action
        List<Tuple> result = query.select(member.username, member.age)
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            int age = tuple.get(member.age);
            System.out.println(username + " / " + age);
        }
    }

    /**
     * JPQL의 DTO 조회는 패키지명까지 풀어써야하는 제약이 있음
     */
    @Test
    void findDtoByJPQL() {
        List<MemberDto> resultList = em.createQuery(
                "select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m",
                MemberDto.class).getResultList();

        resultList.forEach(System.out::println);
    }

    @Test
    public void findDtoBySetter() {
        // action
        List<MemberDto> result = query.select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }

    @Test
    public void findDtoByField() {
        // action
        List<MemberDto> result = query.select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }

    @Test
    public void findDtoByConstructor() {
        // action
        List<MemberDto> result = query.select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }

    /**
     * field, setter 방식은
     * DTO와 필드명이 맞지 않으면 기본적으로 null로 들어감
     * as() 로 필드명을 맞춰주면 됨
     * subquery 등 복잡한건 ExpressionUtils.as() 사용
     */
    @Test
    public void findUserDtoByField() {
        QMember memberSub = new QMember("memberSub");
        // action
        List<UserDto> result = query.select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        ExpressionUtils.as(JPAExpressions
                                        .select(memberSub.age.max())
                                        .from(memberSub)
                                , "age")))
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }

    /**
     * 반면에 생성자 매핑은 타입을 보고 들어가므로
     * 타입과 순서만 맞으면 호환됨
     */
    @Test
    public void findUserDtoByConstructor() {
        QMember memberSub = new QMember("memberSub");
        // action
        List<UserDto> result = query.select(Projections.constructor(UserDto.class,
                        member.username,
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)))
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }

    /**
     * @QueryProjection 을 사용
     * Projections 은 매핑을 잘못해줘도 런타임에러가나지만
     * 이 방식은 컴파일 타임에 매핑에러를 파악할 수 있음
     * <p>
     * 단점
     * - DTO도 Q File을 생성해야함
     * - 여러 레이어에 걸쳐 사용되는 DTO 가 queryDSL 라는 레포지토리 영역 기술에 종속성이 생김
     * <p>
     * 결론
     * - 애플리케이션 구조관점에서 DTO가 queryDSL 에 대한 종속을 가져도 될지 팀 표준을 결정하고
     * 그에 따라 @QueryProjection 사용여부를 결정하자
     */
    @Test
    public void findDtoByQueryProjection() {
        // action
        List<MemberDto> result = query
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();
        result.forEach(System.out::println);
    }

    /*
    동적 쿼리
     */

    @Test
    void dynamicQueryBooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }
        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return query.selectFrom(member)
                .where(builder)
                .fetch();
    }


    /**
     * 추가 메서드 생성이 많지만
     * 가독성과 재활용면에서 유리함
     */
    @Test
    void dynamicQueryWhereParam() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return query.selectFrom(member)
                // where에 여러개를 넣으면 and 로 묶인다
                // null 은 무시된다. (동적 쿼리를 만들 때 유용하다)
//                .where(usernameEq(usernameCond), ageEq(ageCond))
                .where(allEq(usernameCond, ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    /**
    * 이처럼 조건을 조합할 수도 있다.
    * 다만 여기서는 생략했지만 null 처리를 잘 해줘야함 (NPE 주의)
     */
    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    /*
    벌크 연산
     */

    @Test
    void bulkUpdate() {
        // 벌크연산은 영속성 컨텍스트 무시하고 바로 DB에 요청한다. (변경감지와는 다름에 주의)
        long affected = query.update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        assertThat(affected).isEqualTo(2);

        // 영속성 컨텍스트값을 우선하므로 벌크연산결과가 반영되어있지 않음 (1차 캐시를 통한 REPEATABLE READ 같은 효과)
        List<Member> fetch = query.selectFrom(member).fetch();
        fetch.forEach(System.out::println);

        em.flush();
        em.clear();

        // 명시적으로 영속성 컨텍스트 초기화한 뒤에는 정상적으로 얻어옴
        System.out.println("## 영속성 컨텍스트 초기화 ##");
        fetch = query.selectFrom(member).fetch();
        fetch.forEach(System.out::println);
    }

    @Test
    void bulkAdd() {
        // update member set age=age+1;
        query.update(member)
                .set(member.age, member.age.add(1))
                .execute();
        // update member set age=age*2;
        query.update(member)
                .set(member.age, member.age.multiply(2))
                .execute();
    }

    @Test
    void bulkDelete() {
        // delete from member where age>18;
        query.delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    /**
     * db 함수 호출방법 기본 dialect에 포함되지 않는 함수들은
     * JPQL 때와 마찬가지로 커스텀 dialect 생성 후 선언해서 사용해야함
     */
    @Test
    void sqlFunction() {
        query.select(Expressions
                    .stringTemplate("function('replace', {0}, {1}, {2})",
                            member.username, "member", "M"))
                .from(member)
                .fetch();

    }

    /**
     * lower, upper 같은 표준 함수들은 메서드로 제공해주기도한다.
     */
    @Test
    void sqlFunction2() {
        // 둘 다 동일한 결과를 얻음
        // ... where member0_.username=lower(member0_.username);
        List<String> result = query.select(member.username)
                .from(member)
//                .where(member.username.eq(
//                        Expressions.stringTemplate("function('lower', {0})", member.username)))
                .where(member.username.eq(member.username.lower()))
                .fetch();

        result.forEach(System.out::println);
    }
}
