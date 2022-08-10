package study.querydsl.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;

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
}