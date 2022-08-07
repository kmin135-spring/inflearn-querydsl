package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import java.util.List;

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

    /** deprecated 되었지만 강의대로 해봄 */
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
     * */
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
        assertThat(teamA.get(member.age.avg())).isEqualTo((10+20)/2);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo((30+40)/2);
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
}
