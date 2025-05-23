package com.team2.jobscanner;

import com.team2.jobscanner.edu.Member;
import com.team2.jobscanner.edu.MemberRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class MemberRepositoryTests {
    @Autowired
    MemberRepository memberRepository;

    @Test
    @Transactional   //java jpa transaction을 활성화 해야됌
    @Rollback(false)
    public void testMember() throws Exception{
    //given
    Member member = new Member();
    member.setUsername("memberA");
    //when
    Long savedId = memberRepository.save(member);
    Member findMember = memberRepository.find(savedId);
    //then
    Assertions.assertThat(findMember.getId()).isEqualTo(member.getId());
    Assertions.assertThat(findMember.getUsername()).isEqualTo(member.getUsername());
    Assertions.assertThat(findMember).isEqualTo(member);
    System.out.println("findMember == member" + (findMember == member));
    }
   
}
