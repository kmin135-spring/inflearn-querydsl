package study.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@Repository
//@RequiredArgsConstructor
public class MemberJpaRepository {
    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    // 이렇게 만들어서 쓰면 tc 작성이 좀 편하고
    public MemberJpaRepository(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }

    // 이건 lombok 의 도움을 받을 수 있지만 외부에서 JPAQueryFactory Bean을 생성해둬야함.
//    public MemberJpaRepository(EntityManager em, JPAQueryFactory queryFactory) {
//        this.em = em;
//        this.queryFactory = queryFactory;
//    }

    public void save(Member member) {
        em.persist(member);
    }

    public Optional<Member> findById(Long id) {
        Member member = em.find(Member.class, id);
        return Optional.ofNullable(member);
    }

    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }

    public List<Member> findAllQueryDsl() {
        return queryFactory.selectFrom(member).fetch();
    }

    public List<Member> findByUsername(String username) {
        return em.createQuery("select m from Member m where m.username = :username")
                .setParameter("username", username)
                .getResultList();
    }

    public List<Member> findByUsernameQueryDsl(String username) {
        return queryFactory
                .selectFrom(member)
                .where(member.username.eq(username))
                .fetch();
    }

    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition cond) {
        /*
        이런 동적 조건은 모든 조건이 null이면 전체 select가 나가므로
        항상 있는 기본조건을 달아주던가
        paging 과 함께 사용하던가에 대한 고민이 필요하다
         */
        BooleanBuilder builder = new BooleanBuilder();
        if (hasText(cond.getUsername())) {
            builder.and(member.username.eq(cond.getUsername()));
        }
        if (hasText(cond.getTeamName())) {
            builder.and(team.name.eq(cond.getTeamName()));
        }
        if(cond.getAgeGoe() != null) {
            builder.and(member.age.goe(cond.getAgeGoe()));
        }
        if(cond.getAgeLoe() != null) {
            builder.and(member.age.loe(cond.getAgeLoe()));
        }


        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName"))
                )
                .from(member)
                .leftJoin(member.team, team)
                .where(builder)
                .fetch();
    }

    public List<MemberTeamDto> search(MemberSearchCondition search) {
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName"))
                )
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(search.getUsername()),
                        teamNameEq(search.getTeamName()),
//                        ageGoe(search.getAgeGoe()),
//                        ageLoe(search.getAgeLoe())
                        ageBetween(search.getAgeLoe(), search.getAgeGoe())
                )
                .fetch();
    }

    private BooleanExpression ageBetween(Integer ageLoe, Integer ageGoe) {
        return ageLoe != null && ageGoe != null ?
                ageLoe(ageLoe).and(ageGoe(ageGoe)) :
                null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;

    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return hasText(teamName) ? team.name.eq(teamName) : null;
    }

    private BooleanExpression usernameEq(String username) {
        return hasText(username) ? member.username.eq(username) : null;
    }
}
