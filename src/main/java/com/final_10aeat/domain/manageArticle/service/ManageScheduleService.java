package com.final_10aeat.domain.manageArticle.service;

import com.final_10aeat.common.exception.UnauthorizedAccessException;
import com.final_10aeat.domain.manageArticle.dto.request.ScheduleRequestDto;
import com.final_10aeat.domain.manageArticle.dto.util.ScheduleConverter;
import com.final_10aeat.domain.manageArticle.entity.ManageArticle;
import com.final_10aeat.domain.manageArticle.entity.ManageSchedule;
import com.final_10aeat.domain.manageArticle.exception.ScheduleMustHaveOneException;
import com.final_10aeat.domain.manageArticle.exception.ScheduleNotFoundException;
import com.final_10aeat.domain.manageArticle.repository.ManageArticleRepository;
import com.final_10aeat.domain.manageArticle.repository.ManageScheduleRepository;
import com.final_10aeat.domain.manager.entity.Manager;
import com.final_10aeat.domain.repairArticle.exception.ArticleAlreadyDeletedException;
import com.final_10aeat.domain.repairArticle.exception.ArticleNotFoundException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ManageScheduleService {

    private final ManageArticleRepository manageArticleRepository;
    private final ManageScheduleRepository manageScheduleRepository;

    public void register(Long manageArticleId, ScheduleRequestDto request, Manager manager) {
        ManageArticle article = manageArticleRepository.findById(manageArticleId)
            .orElseThrow(ArticleNotFoundException::new);

        if (article.isDeleted()) {
            throw new ArticleAlreadyDeletedException();
        }

        if (!article.getOffice().getId().equals(manager.getOffice().getId())) {
            throw new UnauthorizedAccessException();
        }

        article.addSchedule(
            ScheduleConverter.toSchedule(request, article)
        );

        article.checkSchedules();
    }

    public void complete(Long manageScheduleId, Manager manager) {
        ManageSchedule manageSchedule = manageScheduleRepository.
            findById(manageScheduleId)
            .orElseThrow(ScheduleNotFoundException::new);

        ManageArticle article = Optional.ofNullable(manageSchedule.getManageArticle())
            .orElseThrow(ArticleNotFoundException::new);

        if (article.isDeleted()) {
            throw new ArticleAlreadyDeletedException();
        }

        if (!article.getOffice().getId().equals(manager.getOffice().getId())) {
            throw new UnauthorizedAccessException();
        }

        manageSchedule.complete();

        article.checkSchedules();
    }

    public void update(
        Long manageScheduleId, ScheduleRequestDto request, Manager manager
    ) {

        ManageSchedule manageSchedule = manageScheduleRepository.
            findById(manageScheduleId)
            .orElseThrow(ScheduleNotFoundException::new);

        ManageArticle article = Optional.ofNullable(manageSchedule.getManageArticle())
            .orElseThrow(ArticleNotFoundException::new);

        if (article.isDeleted()) {
            throw new ArticleAlreadyDeletedException();
        }

        if (!article.getOffice().getId().equals(manager.getOffice().getId())) {
            throw new UnauthorizedAccessException();
        }

        if (article.isDeleted()) {
            throw new ArticleAlreadyDeletedException();
        }

        if (!article.getOffice().getId().equals(manager.getOffice().getId())) {
            throw new UnauthorizedAccessException();
        }

        updateSchedule(manageSchedule, request);

        article.checkSchedules();
    }

    private void updateSchedule(ManageSchedule manageSchedule, ScheduleRequestDto request) {
        manageSchedule.setScheduleStart(request.scheduleStart());
        manageSchedule.setScheduleEnd(request.scheduleEnd());
    }

    public void delete(Long manageScheduleId, Manager manager) {
        ManageSchedule manageSchedule = manageScheduleRepository.
            findById(manageScheduleId)
            .orElseThrow(ScheduleNotFoundException::new);

        ManageArticle article = manageSchedule.getManageArticle();

        if (article.isDeleted()) {
            throw new ArticleAlreadyDeletedException();
        }

        if (!article.getOffice().getId().equals(manager.getOffice().getId())) {
            throw new UnauthorizedAccessException();
        }

        if (article.getSchedules().size() == 1) {
            throw new ScheduleMustHaveOneException();
        }

        article.deleteSchedule(manageSchedule);

        manageScheduleRepository.deleteById(manageSchedule.getId());

        article.checkSchedules();
    }
}
