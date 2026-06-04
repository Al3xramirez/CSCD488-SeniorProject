package com.cscd488seniorproject.syllabussyncproject.service;

import com.cscd488seniorproject.syllabussyncproject.entity.CalendarSubscriptionEntity;
import com.cscd488seniorproject.syllabussyncproject.entity.UserAccountEntity;
import com.cscd488seniorproject.syllabussyncproject.emailMeetingNotifications.EmailNotificationRepository;
import com.cscd488seniorproject.syllabussyncproject.meeting.MeetingRepository;
import com.cscd488seniorproject.syllabussyncproject.repository.AssistsRelationRepository;
import com.cscd488seniorproject.syllabussyncproject.repository.CalendarSubscriptionRepository;
import com.cscd488seniorproject.syllabussyncproject.repository.ClassEnrollmentRepository;
import com.cscd488seniorproject.syllabussyncproject.repository.EnrollRelationRepository;
import com.cscd488seniorproject.syllabussyncproject.repository.ExternalEventRepository;
import com.cscd488seniorproject.syllabussyncproject.repository.OfficeHoursExceptionRepository;
import com.cscd488seniorproject.syllabussyncproject.repository.OfficeHoursScheduleRepository;
import com.cscd488seniorproject.syllabussyncproject.repository.TARelationRepository;
import com.cscd488seniorproject.syllabussyncproject.repository.TeachesRelationRepository;
import com.cscd488seniorproject.syllabussyncproject.repository.UserAccountRepository;
import com.cscd488seniorproject.syllabussyncproject.notification.NotificationRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountDeletionService {

    private final UserAccountRepository userRepo;
    private final ClassEnrollmentRepository classEnrollmentRepo;
    private final EnrollRelationRepository enrollRelationRepo;
    private final TARelationRepository taRelationRepo;
    private final TeachesRelationRepository teachesRelationRepo;
    private final AssistsRelationRepository assistsRelationRepo;
    private final OfficeHoursScheduleRepository officeHoursScheduleRepo;
    private final OfficeHoursExceptionRepository officeHoursExceptionRepo;
    private final CalendarSubscriptionRepository calendarSubscriptionRepo;
    private final ExternalEventRepository externalEventRepo;
    private final MeetingRepository meetingRepo;
    private final NotificationRepository notificationRepo;
    private final EmailNotificationRepository emailNotificationRepo;

    public AccountDeletionService(
        UserAccountRepository userRepo,
        ClassEnrollmentRepository classEnrollmentRepo,
        EnrollRelationRepository enrollRelationRepo,
        TARelationRepository taRelationRepo,
        TeachesRelationRepository teachesRelationRepo,
        AssistsRelationRepository assistsRelationRepo,
        OfficeHoursScheduleRepository officeHoursScheduleRepo,
        OfficeHoursExceptionRepository officeHoursExceptionRepo,
        CalendarSubscriptionRepository calendarSubscriptionRepo,
        ExternalEventRepository externalEventRepo,
        MeetingRepository meetingRepo,
        NotificationRepository notificationRepo,
        EmailNotificationRepository emailNotificationRepo
    ) {
        this.userRepo = userRepo;
        this.classEnrollmentRepo = classEnrollmentRepo;
        this.enrollRelationRepo = enrollRelationRepo;
        this.taRelationRepo = taRelationRepo;
        this.teachesRelationRepo = teachesRelationRepo;
        this.assistsRelationRepo = assistsRelationRepo;
        this.officeHoursScheduleRepo = officeHoursScheduleRepo;
        this.officeHoursExceptionRepo = officeHoursExceptionRepo;
        this.calendarSubscriptionRepo = calendarSubscriptionRepo;
        this.externalEventRepo = externalEventRepo;
        this.meetingRepo = meetingRepo;
        this.notificationRepo = notificationRepo;
        this.emailNotificationRepo = emailNotificationRepo;
    }

    @Transactional
    public void deleteMyAccount(Authentication auth) {
        UserAccountEntity me = requireUser(auth);
        String userId = me.getUserId();
        String email = me.getEmail() == null ? "" : me.getEmail().trim().toLowerCase(Locale.ROOT);

        // External calendar data
        List<CalendarSubscriptionEntity> subs = calendarSubscriptionRepo.findByUserId(userId);
        for (CalendarSubscriptionEntity sub : subs) {
            if (sub != null && sub.getSubscriptionId() != null) {
                externalEventRepo.deleteBySubscription_SubscriptionId(sub.getSubscriptionId());
            }
        }
        if (!subs.isEmpty()) {
            calendarSubscriptionRepo.deleteAll(subs);
        }

        // Office hours
        officeHoursExceptionRepo.deleteAll(officeHoursExceptionRepo.findAllByUserId(userId));
        officeHoursScheduleRepo.deleteAll(officeHoursScheduleRepo.findAllByUserId(userId));

        // Course membership relations
        classEnrollmentRepo.deleteAll(classEnrollmentRepo.findByUserId(userId));
        enrollRelationRepo.deleteAll(enrollRelationRepo.findByUserId(userId));

        // Staff relations
        taRelationRepo.deleteAll(taRelationRepo.findAllByUserId(userId));
        teachesRelationRepo.deleteAll(teachesRelationRepo.findAllByUserId(userId));
        assistsRelationRepo.deleteAll(assistsRelationRepo.findByUserId(userId));

        // Email-keyed rows (so recreating the same email starts clean)
        if (!email.isBlank()) {
            meetingRepo.deleteByRequesterIdOrRecipientId(email, email);
            notificationRepo.deleteByRecipientEmail(email);
            emailNotificationRepo.deleteByRecipientEmail(email);
        }

        userRepo.delete(me);
    }

    private UserAccountEntity requireUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        String email = auth.getName() == null ? "" : auth.getName().trim().toLowerCase(Locale.ROOT);
        if (email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return userRepo
            .findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
    }
}
