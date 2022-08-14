package study.querydsl.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;

import javax.persistence.EntityManager;
import java.util.List;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;


/**
커스텀 Repo 구현체의 이름 규칙
인터페이스이름 + Impl
 */
public class MemberRepositoryImpl implements MemberRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    public MemberRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
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

    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition cond, Pageable pageable) {
        QueryResults<MemberTeamDto> results = queryFactory
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
                        usernameEq(cond.getUsername()),
                        teamNameEq(cond.getTeamName()),
                        ageBetween(cond.getAgeLoe(), cond.getAgeGoe())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();

        List<MemberTeamDto> content = results.getResults();
        long total = results.getTotal();

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 리스트, 카운트 쿼리 분리
     * 1. 카운트를 얻을 때 불필요한 join을 제거한다던가 쿼리 최적화 가능
     * 2. 카운트 쿼리를 먼저 돌리고 0건이면 list를 안 돌린다던가 추가적인 튜닝 가능
     * */
    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition cond, Pageable pageable) {
        JPAQuery<Long> countQuery = queryFactory.select(member.count())
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(cond.getUsername()),
                        teamNameEq(cond.getTeamName()),
                        ageBetween(cond.getAgeLoe(), cond.getAgeGoe())
                );

        List<MemberTeamDto> content = queryFactory
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
                        usernameEq(cond.getUsername()),
                        teamNameEq(cond.getTeamName()),
                        ageBetween(cond.getAgeLoe(), cond.getAgeGoe())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();


//        return new PageImpl<>(content, pageable, total);

        /*
        이 방식을 이용하면 count 쿼리가 불필요한 경우 count 쿼리를 수행하지 않고 쿼리를 수행해준다.
        1. 첫페이지 조회했는데 컨텐츠 크기가 요청크기보다 더 작으면 컨텐츠 크기 == total 이고
        2. 조회했더니 마지막 페이지라면 offset 에 컨텐츠 크기를 더하면 == total 인점을 활용한 튜닝.
        세부 구현은 코드를 직접 확인해보자
         */
        return PageableExecutionUtils.getPage(content, pageable, () -> countQuery.fetchOne());
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
