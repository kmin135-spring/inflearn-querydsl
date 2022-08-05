package study.querydsl.entity;

import lombok.*;

import javax.persistence.*;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id", "username", "age"})
public class Member {
    @Id
    @GeneratedValue
    @Column(name = "member_id")
    private Long id;
    private String username;
    private int age;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    public static Member of(String username) {
        Member m = new Member();
        m.setUsername(username);
        return m;
    }
    public static Member of(String username, int age) {
        Member m = of(username);
        m.setAge(age);
        return m;
    }
    public static Member of(String username, int age, Team team) {
        Member m = of(username, age);
        if(team != null) {
            m.changeTeam(team);
        }
        return m;
    }

    public void changeTeam(Team team) {
        this.team = team;
        team.getMembers().add(this);
    }
}