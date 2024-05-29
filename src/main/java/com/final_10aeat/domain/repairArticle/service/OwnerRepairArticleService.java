package com.final_10aeat.domain.repairArticle.service;

import com.final_10aeat.common.dto.UserIdAndRole;
import com.final_10aeat.common.enumclass.ArticleCategory;
import com.final_10aeat.common.enumclass.Progress;
import com.final_10aeat.common.service.AuthenticationService;
import com.final_10aeat.domain.comment.repository.CommentRepository;
import com.final_10aeat.domain.repairArticle.dto.response.RepairArticleResponseDto;
import com.final_10aeat.domain.repairArticle.dto.response.RepairArticleSummaryDto;
import com.final_10aeat.domain.repairArticle.entity.RepairArticle;
import com.final_10aeat.domain.repairArticle.repository.RepairArticleRepository;
import com.final_10aeat.domain.save.repository.ArticleSaveRepository;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OwnerRepairArticleService {

    private final RepairArticleRepository repairArticleRepository;
    private final CommentRepository commentRepository;
    private final ArticleSaveRepository articleSaveRepository;
    private final AuthenticationService authenticationService;

    public RepairArticleSummaryDto getRepairArticleSummary(Long officeId) {
        long total = repairArticleRepository.countByOfficeId(officeId);
        long inProgressAndPending = repairArticleRepository.countByOfficeIdAndProgressIn(officeId,
            List.of(Progress.INPROGRESS, Progress.PENDING));
        long completed = repairArticleRepository.countByOfficeIdAndProgress(officeId,
            Progress.COMPLETE);

        return new RepairArticleSummaryDto(total, inProgressAndPending, completed);
    }

    public List<RepairArticleResponseDto> getAllRepairArticles(UserIdAndRole userIdAndRole,
        List<Progress> progresses, ArticleCategory category) {
        Long officeId = authenticationService.getUserOfficeId();
        List<RepairArticle> articles = repairArticleRepository.findByOfficeIdAndProgressInAndCategory(
            officeId, progresses, category);

        Set<Long> savedArticleIds;
        if (!userIdAndRole.isManager()) {
            List<Long> articleIds = articles.stream().map(RepairArticle::getId)
                .collect(Collectors.toList());
            savedArticleIds = articleSaveRepository.findSavedArticleIdsByMember(userIdAndRole.id(),
                articleIds);
        } else {
            savedArticleIds = new HashSet<>();
        }

        return articles.stream().map(article -> {
            boolean isNewArticle = article.getCreatedAt().plusDays(1).isAfter(LocalDateTime.now());
            int commentCount = (int) commentRepository.countByRepairArticleIdAndDeletedAtIsNull(
                article.getId());
            boolean isSaved = savedArticleIds.contains(article.getId());

            return new RepairArticleResponseDto(
                article.getId(),
                article.getCategory().name(),
                article.getManager().getName(),
                article.getProgress().name(),
                article.getTitle(),
                article.getStartConstruction(),
                article.getEndConstruction(),
                commentCount,
                200, // Dummy view count
                isSaved,
                false, // Dummy issue flag
                isNewArticle,
                article.getImages().isEmpty() ? null
                    : article.getImages().iterator().next().getImageUrl()
            );
        }).collect(Collectors.toList());
    }
}