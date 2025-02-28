package com.final_10aeat.domain.save.service;

import com.final_10aeat.common.exception.ArticleNotFoundException;
import com.final_10aeat.common.exception.UnauthorizedAccessException;
import com.final_10aeat.domain.member.entity.Member;
import com.final_10aeat.domain.member.exception.UserNotExistException;
import com.final_10aeat.domain.member.repository.MemberRepository;
import com.final_10aeat.domain.repairArticle.entity.RepairArticle;
import com.final_10aeat.domain.repairArticle.repository.RepairArticleRepository;
import com.final_10aeat.domain.save.entity.ArticleSave;
import com.final_10aeat.domain.save.exception.ArticleAlreadyLikedException;
import com.final_10aeat.domain.save.exception.ArticleNotLikedException;
import com.final_10aeat.domain.save.repository.ArticleSaveRepository;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.websocket.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ArticleSaveService {

    private final ArticleSaveRepository articleSaveRepository;
    private final RepairArticleRepository repairArticleRepository;
    private final MemberRepository memberRepository;

    public void saveArticle(Long repairArticleId, Long memberId) {
        RepairArticle repairArticle = getRepairArticle(repairArticleId);
        Member member = getMember(memberId);

        if (!repairArticle.getOffice().getId().equals(member.getDefaultOffice())) {
            throw new UnauthorizedAccessException();
        }

        if (articleSaveRepository.existsByRepairArticleAndMember(repairArticle, member)) {
            throw new ArticleAlreadyLikedException();
        }

        ArticleSave articleSave = ArticleSave.builder()
            .repairArticle(repairArticle)
            .member(member)
            .build();

        articleSaveRepository.save(articleSave);
    }

    public void unsaveArticle(Long repairArticleId, Long memberId) {
        RepairArticle repairArticle = getRepairArticle(repairArticleId);
        Member member = getMember(memberId);

        ArticleSave articleSave = articleSaveRepository.findByRepairArticleAndMember(repairArticle,
                member)
            .orElseThrow(ArticleNotLikedException::new);

        articleSaveRepository.delete(articleSave);
    }

    private RepairArticle getRepairArticle(Long repairArticleId) {
        return repairArticleRepository.findById(repairArticleId)
            .orElseThrow(ArticleNotFoundException::new);
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
            .orElseThrow(UserNotExistException::new);
    }
}
