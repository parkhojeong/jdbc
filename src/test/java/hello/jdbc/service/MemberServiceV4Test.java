package hello.jdbc.service;

import hello.jdbc.domain.Member;
import hello.jdbc.repository.MemberRepository;
import hello.jdbc.repository.MemberRepositoryV3;
import hello.jdbc.repository.MemberRepositoryV4_1;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * solve Exception leakage problem
 * remove "SQLException"
 */
@Slf4j
@SpringBootTest
class MemberServiceV4Test {

    public static final String MEMBER_A = "memberA";
    public static final String MEMBER_B = "memberB";
    public static final String MEMBER_EX = "ex";

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private MemberServiceV4 memberService;

    @TestConfiguration
    static class TestConfig {

        private final DataSource dataSource;

        TestConfig(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        @Bean
        MemberServiceV4 memberService(MemberRepository memberRepository) {
            return new MemberServiceV4(memberRepository);
        }

        @Bean
        MemberRepository memberRepository() {
            return new MemberRepositoryV4_1(dataSource);
        }
    }

    @AfterEach
    void tearDown() {
        memberRepository.deleteById(MEMBER_A);
        memberRepository.deleteById(MEMBER_B);
        memberRepository.deleteById(MEMBER_EX);
    }

    @Test
    void AopCheck() {
        log.info("memberService = {}", memberService.getClass());
        log.info("memberRepository = {}", memberRepository.getClass());
        Assertions.assertThat(AopUtils.isAopProxy(memberService)).isTrue();
        Assertions.assertThat(AopUtils.isAopProxy(memberRepository)).isFalse();
    }

    @Test
    void accountTransferNormal() {
        Member memberA = new Member(MEMBER_A, 10000);
        Member memberB = new Member(MEMBER_B, 10000);
        memberRepository.save(memberA);
        memberRepository.save(memberB);

        log.info("START TX");
        memberService.accountTransfer(memberA.getMemberId(), memberB.getMemberId(), 1000);
        log.info("END TX");

        Member memberA1 = memberRepository.findById(memberA.getMemberId());
        Member memberB1 = memberRepository.findById(memberB.getMemberId());
        Assertions.assertThat(memberA1.getMoney()).isEqualTo(9000);
        Assertions.assertThat(memberB1.getMoney()).isEqualTo(11000);
    }

    @Test
    void accountTransferException() {
        Member memberA = new Member(MEMBER_A, 10000);
        Member memberEx = new Member(MEMBER_EX, 10000);
        memberRepository.save(memberA);
        memberRepository.save(memberEx);

        Assertions.assertThatThrownBy(() -> memberService.accountTransfer(memberA.getMemberId(), memberEx.getMemberId(), 1000))
                .isInstanceOf(IllegalStateException.class);

        Member memberA1 = memberRepository.findById(memberA.getMemberId());
        Member memberB1 = memberRepository.findById(memberEx.getMemberId());
        Assertions.assertThat(memberA1.getMoney()).isEqualTo(10000);
        Assertions.assertThat(memberB1.getMoney()).isEqualTo(10000);

    }
}