package study.querydsl.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.repository.support.Querydsl4RepositorySupport;

import java.util.List;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.team;

@Repository
public class MemberTestRepository extends Querydsl4RepositorySupport {
    public MemberTestRepository() {
        super(Member.class);
    }

    public List<Member> basicSelect() {
        return select(member)
                .from(member)
                .fetch();
    }

    public List<Member> basicSelectFrom() {
        return selectFrom(member)
                .fetch();
    }

    public Page<Member> searchPageByApplyPage(MemberSearchCondition cond, Pageable pageable) {
        JPAQuery<Member> query = selectFrom(member)
                .leftJoin(member.team, team)
                .where(usernameEq(cond.getUsername()),
                        teamNameEq(cond.getTeamName()),
                        ageBetween(cond.getAgeLoe(), cond.getAgeGoe())
                );

        List<Member> content = getQuerydsl().applyPagination(pageable, query)
                .fetch();

        return PageableExecutionUtils.getPage(content, pageable, () -> query.fetchCount());
    }

    public Page<Member> applyPagination(MemberSearchCondition cond, Pageable pageable) {
        return applyPagination(pageable,
                query -> query.selectFrom(member)
                        .leftJoin(member.team, team)
                        .where(usernameEq(cond.getUsername()),
                                teamNameEq(cond.getTeamName()),
                                ageBetween(cond.getAgeLoe(), cond.getAgeGoe())
                        ));
    }

    public Page<Member> applyPagination2(MemberSearchCondition cond, Pageable pageable) {
        return applyPagination(pageable,
                query -> query.selectFrom(member)
                        .leftJoin(member.team, team)
                        .where(usernameEq(cond.getUsername()),
                                teamNameEq(cond.getTeamName()),
                                ageBetween(cond.getAgeLoe(), cond.getAgeGoe())
                        ),
                countQuery -> countQuery.select(member.id)
                        .from(member)
                        .leftJoin(member.team, team)
                        .where(usernameEq(cond.getUsername()),
                                teamNameEq(cond.getTeamName()),
                                ageBetween(cond.getAgeLoe(), cond.getAgeGoe())
                        ));
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
