package study.querydsl.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;

import java.util.List;

public interface MemberRepositoryCustom {

    List<MemberTeamDto> search(MemberSearchCondition cond);
    Page<MemberTeamDto> searchPageSimple(MemberSearchCondition cond, Pageable pageable);
    Page<MemberTeamDto> searchPageComplex(MemberSearchCondition cond, Pageable pageable);

}
