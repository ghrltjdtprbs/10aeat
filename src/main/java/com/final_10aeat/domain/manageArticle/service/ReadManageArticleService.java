package com.final_10aeat.domain.manageArticle.service;

import static com.final_10aeat.domain.manageArticle.dto.request.GetYearListPageQuery.toQueryDto;
import static java.util.Optional.ofNullable;

import com.final_10aeat.common.enumclass.Progress;
import com.final_10aeat.common.exception.ArticleNotFoundException;
import com.final_10aeat.common.exception.UnauthorizedAccessException;
import com.final_10aeat.domain.articleIssue.entity.ArticleIssue;
import com.final_10aeat.domain.manageArticle.dto.request.GetMonthlyListWithYearQuery;
import com.final_10aeat.domain.manageArticle.dto.request.GetYearListQuery;
import com.final_10aeat.domain.manageArticle.dto.request.SearchManageArticleQuery;
import com.final_10aeat.domain.manageArticle.dto.response.DetailManageArticleResponse;
import com.final_10aeat.domain.manageArticle.dto.response.ListManageArticleResponse;
import com.final_10aeat.domain.manageArticle.dto.response.ManageArticleSummaryResponse;
import com.final_10aeat.domain.manageArticle.dto.response.SearchManagersManageResponse;
import com.final_10aeat.domain.manageArticle.dto.response.SummaryManageArticleResponse;
import com.final_10aeat.domain.manageArticle.dto.util.ScheduleConverter;
import com.final_10aeat.domain.manageArticle.entity.ManageArticle;
import com.final_10aeat.domain.manageArticle.entity.ManageSchedule;
import com.final_10aeat.domain.manageArticle.repository.ManageArticleRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReadManageArticleService {

    private final ManageArticleRepository manageArticleRepository;

    private final List<Progress> unCompleteProgress = List.of(
        Progress.PENDING,
        Progress.INPROGRESS
    );

    private final List<Progress> completeProgress = List.of(
        Progress.COMPLETE
    );

    public SummaryManageArticleResponse summary(Long id, Integer year) {
        List<ManageArticle> articles = manageArticleRepository
            .findAllByYear(GetYearListQuery.toQueryDto(year, id));

        return summaryArticleFrom(articles);
    }

    private SummaryManageArticleResponse summaryArticleFrom(List<ManageArticle> articles) {
        ArrayList<Long> issueList = new ArrayList<>();
        int complete = 0;
        int inprogress = 0;
        int pending = 0;

        for (ManageArticle article : articles) {
            Progress progress = article.getProgress();
            if (progress == Progress.COMPLETE) {
                complete++;
            }
            if (progress == Progress.INPROGRESS) {
                inprogress++;
            }
            if (progress == Progress.PENDING) {
                pending++;
            }
            article.getActiveIssue().ifPresent(issue -> issueList.add(issue.getId()));
        }

        return new SummaryManageArticleResponse(complete, inprogress, pending, issueList);
    }

    public DetailManageArticleResponse detailArticle(Long userOfficeId, Long articleId) {
        ManageArticle article = manageArticleRepository.findByIdAndDeletedAtNull(
                articleId)
            .orElseThrow(ArticleNotFoundException::new);

        if (!article.getOffice().getId().equals(userOfficeId)) {
            throw new UnauthorizedAccessException();
        }

        return detailArticleFrom(article);
    }

    private DetailManageArticleResponse detailArticleFrom(ManageArticle article) {
        return DetailManageArticleResponse.builder()
            .period(article.getPeriod())
            .periodCount(article.getPeriodCount())
            .title(article.getTitle())
            .issueId(article.getActiveIssue().map(ArticleIssue::getId).orElse(null))
            .progress(article.getProgress())
            .legalBasis(article.getLegalBasis())
            .target(article.getTarget())
            .responsibility(article.getResponsibility())
            .note(article.getResponsibility())
            .manageSchedule(
                article.getSchedules().stream()
                    .sorted(Comparator.comparing(ManageSchedule::getScheduleStart).reversed())
                    .map(ScheduleConverter::toScheduleResponse).toList()
            )
            .build();
    }

    public Page<ListManageArticleResponse> listArticleByProgress(
        Integer year, Long userOfficeId, Pageable pageRequest
    ) {
        return manageArticleRepository
            .searchByKeyword(SearchManageArticleQuery.builder()
                .year(year)
                .officeId(userOfficeId)
                .pageRequest(pageRequest)
                .build())
            .map(this::listArticleFrom);
    }

    public Page<ListManageArticleResponse> listArticleByProgress(
        Integer year, Long userOfficeId, Pageable pageRequest, Boolean complete
    ) {
        Page<ManageArticle> articles = manageArticleRepository
            .findAllByUnDeletedOfficeIdAndScheduleYearAndProgress(
                userOfficeId, year, pageRequest, complete ? completeProgress : unCompleteProgress
            );

        return articles.map(this::listArticleFrom);
    }

    private ListManageArticleResponse listArticleFrom(ManageArticle article) {
        return ListManageArticleResponse.builder()
            .id(article.getId())
            .period(article.getPeriod())
            .periodCount(article.getPeriodCount())
            .title(article.getTitle())
            .allSchedule(article.getSchedules().size())
            .completedSchedule(
                (int) article.getSchedules().stream()
                    .filter(ManageSchedule::isComplete).count()
            )
            .issueId(article.getActiveIssue().map(ArticleIssue::getId).orElse(null))
            .build();
    }

    public List<ManageArticleSummaryResponse> monthlySummary(Integer year, Long officeId) {
        List<ManageArticle> articles = manageArticleRepository.findAllByOfficeIdAndDeletedAtNull(
            officeId);

        return toSummaryDto(articles, year);
    }

    private List<ManageArticleSummaryResponse> toSummaryDto(List<ManageArticle> articles,
        Integer year) {
        List<ManageArticleSummaryResponse> monthly = new ArrayList<>();
        HashMap<Integer, Set<Long>> monthArticleIdMap = new HashMap<>();

        articles.forEach(
            article -> article.getSchedules().stream()
                .filter(schedule -> schedule.getYear().equals(year))
                .forEach(schedule -> {
                    Integer month = schedule.getMonth();
                    monthArticleIdMap.computeIfAbsent(month, k -> new HashSet<>())
                        .add(article.getId());
                })
        );

        monthArticleIdMap.forEach(
            (key, value) -> monthly.add(new ManageArticleSummaryResponse(key, value.size()))
        );
        return monthly;
    }

    public Page<ListManageArticleResponse> monthlyListArticle(
        Long userOfficeId, Integer year, Integer month, Pageable pageRequest
    ) {
        if (ofNullable(month).isEmpty()) {
            return manageArticleRepository
                .findAllByYear(toQueryDto(year, userOfficeId, pageRequest))
                .map(this::listArticleFrom);
        }

        return manageArticleRepository
            .findAllByYearAndMonthly(GetMonthlyListWithYearQuery
                .toQueryDto(year, month, userOfficeId, pageRequest)
            )
            .map(this::listArticleFrom);
    }

    public Page<ListManageArticleResponse> search(
        Long userOfficeId, String search, Pageable pageRequest
    ) {
        return manageArticleRepository.searchByOfficeIdAndText(userOfficeId, search, pageRequest)
            .map(this::listArticleFrom);
    }

    public Page<SearchManagersManageResponse> managerSearch(
        Long userOfficeId, Integer year, String keyword, Integer month, Pageable pageRequest,
        LocalDateTime now) {
        return manageArticleRepository.searchByKeyword(
                SearchManageArticleQuery.builder()
                    .now(now)
                    .keyword(keyword)
                    .year(year)
                    .month(month)
                    .officeId(userOfficeId)
                    .pageRequest(pageRequest)
                    .build()
            )
            .map(this::searchArticleFrom);
    }

    private SearchManagersManageResponse searchArticleFrom(ManageArticle article) {
        return SearchManagersManageResponse.builder()
            .id(article.getId())
            .period(article.getPeriod())
            .periodCount(article.getPeriodCount())
            .title(article.getTitle())
            .allSchedule(article.getSchedules().size())
            .completedSchedule(
                (int) article.getSchedules().stream()
                    .filter(ManageSchedule::isComplete).count()
            )
            .currentSchedules(getCurrentSchedules(article))
            .build();
    }

    private LocalDateTime getCurrentSchedules(ManageArticle article) {
        LocalDateTime now = LocalDateTime.now();
        return article.getSchedules().stream()
            .map(ManageSchedule::getScheduleStart)
            .filter(start -> !start.isBefore(now))
            .min(Comparator.naturalOrder())
            .orElse(null);
    }
}
