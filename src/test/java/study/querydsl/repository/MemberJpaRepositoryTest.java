package study.querydsl.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {
    @Autowired
    EntityManager em;

    @Autowired
    MemberJpaRepository jpaRepo;

    @Test
    void basicTest() {
        Member member = Member.of("member1", 10);
        jpaRepo.save(member);

        Member findMember = jpaRepo.findById(member.getId()).get();
        assertThat(findMember).isSameAs(member);

        List<Member> list = jpaRepo.findAll();
        assertThat(list).containsExactly(member);

        List<Member> list2 = jpaRepo.findByUsername("member1");
        assertThat(list2).containsExactly(member);
    }

    @Test
    void basicQueryDslTest() {
        Member member = Member.of("member1", 10);
        jpaRepo.save(member);

        Member findMember = jpaRepo.findById(member.getId()).get();
        assertThat(findMember).isSameAs(member);

        List<Member> list = jpaRepo.findAllQueryDsl();
        assertThat(list).containsExactly(member);

        List<Member> list2 = jpaRepo.findByUsernameQueryDsl("member1");
        assertThat(list2).containsExactly(member);
    }

    @Test
    public void searchByBuilder() {
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

        MemberSearchCondition cond = new MemberSearchCondition();
        cond.setAgeGoe(35);
        cond.setAgeLoe(40);
        cond.setTeamName("teamB");

        List<MemberTeamDto> results = jpaRepo.searchByBuilder(cond);
        assertThat(results.size()).isEqualTo(1);
        assertThat(results).extracting("username").containsExactly("member4");
    }

    @Test
    public void searchByWhere() {
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

        MemberSearchCondition cond = new MemberSearchCondition();
        cond.setAgeGoe(35);
        cond.setAgeLoe(40);
        cond.setTeamName("teamB");

        List<MemberTeamDto> results = jpaRepo.search(cond);
        assertThat(results.size()).isEqualTo(1);
        assertThat(results).extracting("username").containsExactly("member4");
    }
}