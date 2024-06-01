package com.final_10aeat.domain.member.service;

import com.final_10aeat.domain.member.dto.request.BuildingInfoRequestDto;
import com.final_10aeat.domain.member.dto.response.MyBuildingInfoResponseDto;
import com.final_10aeat.domain.member.dto.response.MyInfoResponseDto;
import com.final_10aeat.domain.member.entity.BuildingInfo;
import com.final_10aeat.domain.member.entity.Member;
import com.final_10aeat.domain.member.exception.BuildingInfoNotAssociatedException;
import com.final_10aeat.domain.member.exception.BuildingInfoNotFound;
import com.final_10aeat.domain.member.exception.DuplicateBuildingInfoException;
import com.final_10aeat.domain.member.exception.MinBuildingInfoRequiredException;
import com.final_10aeat.domain.member.exception.UserNotExistException;
import com.final_10aeat.domain.member.repository.BuildingInfoRepository;
import com.final_10aeat.domain.member.repository.MemberRepository;
import com.final_10aeat.domain.office.entity.Office;
import com.final_10aeat.domain.office.exception.OfficeNotFoundException;
import com.final_10aeat.domain.office.repository.OfficeRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final MemberRepository memberRepository;
    private final OfficeRepository officeRepository;
    private final BuildingInfoRepository buildingInfoRepository;

    public List<MyBuildingInfoResponseDto> getBuildingInfo(Member member) {
        Member loadedMember = memberRepository.findMemberByIdWithBuildingInfos(member.getId())
            .orElseThrow(UserNotExistException::new);
        Set<BuildingInfo> buildingInfos = loadedMember.getBuildingInfos();

        return buildingInfos.stream()
            .filter(bi -> bi.getOffice() != null && bi.getOffice().getOfficeName() != null)
            .sorted(Comparator.comparing(bi -> bi.getOffice().getOfficeName()))
            .map(this::toInfoDto)
            .toList();
    }

    private MyBuildingInfoResponseDto toInfoDto(BuildingInfo buildingInfo) {
        return MyBuildingInfoResponseDto.builder()
            .officeName(buildingInfo.getOffice().getOfficeName())
            .buildingInfoId(buildingInfo.getId())
            .dong(buildingInfo.getDong())
            .ho(buildingInfo.getHo())
            .build();
    }

    public MyInfoResponseDto getMyInfo(Member member) {
        String officeName = officeRepository.findById(member.getDefaultOffice())
            .map(Office::getOfficeName)
            .orElse("Unknown Office");

        return new MyInfoResponseDto(
            member.getName(),
            member.getRole().name(),
            officeName
        );
    }

    @Transactional
    public void addBuildingInfo(Member member, BuildingInfoRequestDto requestDto) {
        Long defaultOfficeId = member.getDefaultOffice();
        if (defaultOfficeId == null) {
            throw new OfficeNotFoundException();
        }

        Office office = officeRepository.findById(defaultOfficeId)
            .orElseThrow(OfficeNotFoundException::new);

        Member managedMember = memberRepository.findById(member.getId())
            .orElseThrow(UserNotExistException::new);

        Set<BuildingInfo> buildingInfos = managedMember.getBuildingInfos();
        if (buildingInfos.stream().anyMatch(
            info -> info.getDong().equals(requestDto.dong()) && info.getHo()
                .equals(requestDto.ho()))) {
            throw new DuplicateBuildingInfoException();
        }

        BuildingInfo buildingInfo = BuildingInfo.builder()
            .dong(requestDto.dong())
            .ho(requestDto.ho())
            .office(office)
            .build();

        buildingInfoRepository.save(buildingInfo);

        buildingInfos.add(buildingInfo);

        managedMember.setBuildingInfos(buildingInfos);
        memberRepository.save(managedMember);
    }

    @Transactional
    public void removeBuildingInfo(Member member, Long buildingInfoId) {
        Member managedMember = memberRepository.findById(member.getId())
            .orElseThrow(UserNotExistException::new);

        Set<BuildingInfo> buildingInfos = managedMember.getBuildingInfos();
        if (buildingInfos.size() <= 1) {
            throw new MinBuildingInfoRequiredException();
        }

        BuildingInfo buildingInfoToRemove = buildingInfoRepository.findById(buildingInfoId)
            .orElseThrow(BuildingInfoNotFound::new);

        if (!buildingInfos.contains(buildingInfoToRemove)) {
            throw new BuildingInfoNotAssociatedException();
        }

        buildingInfos.remove(buildingInfoToRemove);
        managedMember.setBuildingInfos(buildingInfos);
        memberRepository.save(managedMember);
    }
}
