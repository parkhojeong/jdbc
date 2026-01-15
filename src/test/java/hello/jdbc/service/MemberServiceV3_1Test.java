package hello.jdbc.service;

import hello.jdbc.connection.ConnectionConst;
import hello.jdbc.domain.Member;
import hello.jdbc.repository.MemberRepositoryV2;
import hello.jdbc.repository.MemberRepositoryV3;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.SQLException;

@Slf4j
class MemberServiceV3_1Test {

    public static final String MEMBER_A = "memberA";
    public static final String MEMBER_B = "memberB";
    public static final String MEMBER_EX = "ex";

    private MemberRepositoryV3 memberRepository;
    private MemberServiceV3_1 memberService;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(ConnectionConst.URL, ConnectionConst.USERNAME, ConnectionConst.PASSWORD);
        memberRepository = new MemberRepositoryV3(dataSource);
        memberService = new MemberServiceV3_1(new DataSourceTransactionManager(dataSource), memberRepository);
    }

    @AfterEach
    void tearDown() throws SQLException {
        memberRepository.deleteById(MEMBER_A);
        memberRepository.deleteById(MEMBER_B);
        memberRepository.deleteById(MEMBER_EX);
    }

    @Test
    void accountTransferNormal() throws SQLException {
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
    void accountTransferException() throws SQLException {
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