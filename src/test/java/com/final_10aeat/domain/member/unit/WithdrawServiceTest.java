package com.final_10aeat.domain.member.unit;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.final_10aeat.common.enumclass.MemberRole;
import com.final_10aeat.domain.member.dto.request.MemberRegisterRequestDto;
import com.final_10aeat.domain.member.dto.request.MemberWithdrawRequestDto;
import com.final_10aeat.domain.member.entity.BuildingInfo;
import com.final_10aeat.domain.member.entity.Member;
import com.final_10aeat.domain.member.exception.PasswordMissMatchException;
import com.final_10aeat.domain.member.exception.UserNotExistException;
import com.final_10aeat.domain.member.repository.BuildingInfoRepository;
import com.final_10aeat.domain.member.repository.MemberRepository;
import com.final_10aeat.domain.member.service.MemberService;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

public class WithdrawServiceTest {

    @InjectMocks
    private MemberService memberService;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private BuildingInfoRepository buildingInfoRepository;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private final String email = "spring@naver.com";
    private final String password = "spring";
    private final BuildingInfo buildingInfo = BuildingInfo.builder()
        .dong("102동")
        .ho("2212호")
        .office(null)
        .build();
    private final Member member = Member.builder()
        .id(1L)
        .email(email)
        .password(password)
        .name("spring")
        .role(MemberRole.TENANT)
        .buildingInfos(Set.of(buildingInfo))
        .build();
    private final MemberRegisterRequestDto requestDto = new MemberRegisterRequestDto(email,
        password, "spring", "102동", "2212호", MemberRole.TENANT, true);

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(email + ":info")).thenReturn(
            "{\"officeId\":1,\"dong\":\"102동\",\"ho\":\"2212호\"}");

        given(buildingInfoRepository.save(any(BuildingInfo.class))).willReturn(buildingInfo);

        memberService.register(requestDto);

        given(memberRepository.findByEmailAndDeletedAtIsNull(email)).willReturn(
            Optional.of(member));
        given(memberRepository.existsByEmailAndDeletedAtIsNull(email)).willReturn(false);
        when(passwordEncoder.matches(password, member.getPassword())).thenReturn(true);
    }

    @Nested
    @DisplayName("withdraw()는 ")
    class Context_Withdraw {

        @Test
        @DisplayName("회원 탈퇴를 성공한다.")
        void _willSuccess() {
            // given
            MemberWithdrawRequestDto memberRequest = new MemberWithdrawRequestDto(email, password);

            // when
            memberService.withdraw(memberRequest);

            // then
            verify(memberRepository).findByEmailAndDeletedAtIsNull(email);
            verify(memberRepository).save(any(Member.class));
        }

        @Test
        @DisplayName("비밀번호가 일치하지 않는 사용자의 탈퇴를 시도하여 실패한다.")
        void _willMissMatch() {
            String wrongPassword = "2222";
            MemberWithdrawRequestDto memberRequest = new MemberWithdrawRequestDto(email,
                wrongPassword);

            Assertions.assertThrows(PasswordMissMatchException.class,
                () -> memberService.withdraw(memberRequest));
        }

        @Test
        @DisplayName("회원 탈퇴하려는 계정이 존재하지 않아 실패한다.")
        void _willNotExist() {
            String wrongEmail = "2222@naver.com";
            MemberWithdrawRequestDto memberRequest = new MemberWithdrawRequestDto(wrongEmail,
                password);

            Assertions.assertThrows(UserNotExistException.class,
                () -> memberService.withdraw(memberRequest));
        }
    }
}
