package study.querydsl.entity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Rollback(value = false)
class MemberTest {
    @Autowired
    private EntityManager em;

    @Test
    void testEntity() {
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

        em.flush();
        em.clear();

        List<Member> members = em.createQuery("select m from Member m", Member.class).getResultList();

        for(Member member : members) {
            System.out.println(member);
        }
    }
}