package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;

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
}
