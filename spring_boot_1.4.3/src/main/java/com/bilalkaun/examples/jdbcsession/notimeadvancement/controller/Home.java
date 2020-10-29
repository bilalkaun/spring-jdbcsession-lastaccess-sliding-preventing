package com.bilalkaun.examples.jdbcsession.notimeadvancement.controller;

import com.bilalkaun.examples.jdbcsession.notimeadvancement.timeoutslidingpreventer.IgnoreTimeoutAdvancement;
import org.springframework.session.ExpiringSession;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/**
 * Example controller to demo timeout slider-advancement prevention
 */
@Controller
public class Home {

    private final HttpSession httpSession;
    private FindByIndexNameSessionRepository sessionRepository;
    private final static DateTimeFormatter dtFormatter = DateTimeFormatter.ofLocalizedDateTime( FormatStyle.FULL ).withZone( ZoneId.systemDefault() );

    public Home(HttpSession httpSession, FindByIndexNameSessionRepository sessionRepository) {
        this.httpSession = httpSession;
        this.sessionRepository = sessionRepository;
    }

    /**
     * This controller delivers session information out to the browser
     * @return
     */
    @GetMapping("/")
    @ResponseBody
    public String showSessionWithTimeAdvancement(HttpServletRequest httpServletRequest)
    {
        String sessionId = httpSession.getId();
        Instant lastAccessedTime = Instant.ofEpochMilli(httpSession.getLastAccessedTime());
        Instant creationTime = Instant.ofEpochMilli(httpSession.getCreationTime());

        return String.format("Session: %s, Created: %s, Last Accessed: %s",
                sessionId,
                dtFormatter.format(creationTime),
                dtFormatter.format(lastAccessedTime));
    }

    /**
     * This controller delivers session information out to the browser but not advance the time
     * @return
     */
    @GetMapping("/notimeadv")
    @IgnoreTimeoutAdvancement
    @ResponseBody
    public String showSessionWithNoTimeAdvancement()
    {
        String sessionId = httpSession.getId();
        ExpiringSession session = (ExpiringSession) sessionRepository.getSession(sessionId);

        if (session == null) {
            return "New session will be saved at end of call. Try refreshing again.";
        }

        Instant lastAccessedTime = Instant.ofEpochMilli(session.getLastAccessedTime());
        Instant creationTime = Instant.ofEpochMilli(session.getCreationTime());

        return  String.format("Session: %s, Created: %s, Last Accessed: %s",
                sessionId,
                dtFormatter.format(creationTime),
                dtFormatter.format(lastAccessedTime));
    }
}
