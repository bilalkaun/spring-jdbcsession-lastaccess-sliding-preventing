package com.bilalkaun.examples.jdbcsession.notimeadvancement.timeoutslidingpreventer;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.session.SessionRepository;
import org.springframework.session.jdbc.JdbcOperationsSessionRepository;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Resets session 'dirty' flags from an Aspect-point. Called before the controller-method
 * entry when annotated with @IgnoreTimeoutAdvancement
 */
@Aspect
@Component
public class IgnoreTimeoutAdvancementProcessor {
    private static final Method clearChangeFlagsMethod;
    private static final Method jdbcSessionIsNewMethod;
    private static final Method getSessionMethod;
    private final String CURRENT_SESSION_ATTR = HttpServletRequestWrapper.class.getName();
    private static final Logger log = LoggerFactory.getLogger(IgnoreTimeoutAdvancementProcessor.class);

    private HttpServletRequest httpRequest;
    private HttpSession httpSession;

    /*
        This bit of reflection is necessary to access the very instance of the JDBCSession riding inside the
        HttpServletRequest attribute, encapsulated in the HttpSessionWrapper class.

        Yes, it accesses internals of Spring JdbcSession library, and yes it may break without warning,
        but you gotta do what you gotta do, unless the Spring devs see value in providing a natural way of exempting
        controller methods from timeout sliding
     */
    static {
        Class<?>[] nestedInternalClasses = JdbcOperationsSessionRepository.class.getDeclaredClasses();
        Class<?> jdbcSessionClazz = Arrays.stream(nestedInternalClasses)
                .filter(clazz -> clazz.getName().endsWith("JdbcSession"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("JdbcSession Internal class reference required for timeout-slider prevention mechanism"));

        Method[] jdbcSessionClazzDeclaredMethods = jdbcSessionClazz.getDeclaredMethods();
        clearChangeFlagsMethod = Arrays.stream(jdbcSessionClazzDeclaredMethods)
                .filter(method -> method.getName().endsWith("clearChangeFlags"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("JdbcSession.clearChangeFlags() method reference required for timeout-slider prevention mechanism"));

        clearChangeFlagsMethod.setAccessible(true);

        jdbcSessionIsNewMethod = Arrays.stream(jdbcSessionClazzDeclaredMethods)
                .filter(method -> method.getName().endsWith("isNew"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("JdbcSession.isNew() method reference required for timeout-slider prevention mechanism"));

        jdbcSessionIsNewMethod.setAccessible(true);

        Class<?> expiringSessionHttpSessionClazz = null;
        try {
            expiringSessionHttpSessionClazz = SessionRepositoryFilter.class.getClassLoader().loadClass("org.springframework.session.web.http.ExpiringSessionHttpSession");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot start without class reference", e);
        }

        Method[] expiringSessionHttpSessionDeclaredMethods = expiringSessionHttpSessionClazz.getDeclaredMethods();

        getSessionMethod = Arrays.stream(expiringSessionHttpSessionDeclaredMethods)
                .filter(method -> method.getName().endsWith("getSession"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("ExpiringSessionHttpSession.getSession() method reference required for timeout-slider prevention mechanism"));

        getSessionMethod.setAccessible(true);

    }

    public IgnoreTimeoutAdvancementProcessor(HttpServletRequest httpRequest, HttpSession httpSession) {
        this.httpRequest = httpRequest;
        this.httpSession = httpSession;
    }

    @Around("@annotation(IgnoreTimeoutAdvancement)")
    public Object preventTimeoutSlider(ProceedingJoinPoint joinPoint) throws Throwable {

        // Before entering the annotated method, clear the dirty flags of Session
        doNotExtendSessionTimeoutForRequest();

        return joinPoint.proceed();
    }

    /**
     * If the session is not new, we don't want to save it to prevent "lastAccessedTime" from being updated in the db
     */
    private void doNotExtendSessionTimeoutForRequest()
    {
        if (httpSession == null)
            return;

        String sessionId = httpSession.getId();

        if (sessionId == null || sessionId.trim().isEmpty())
            return;

        Object wrappedSession = httpRequest.getAttribute(CURRENT_SESSION_ATTR);

        try {
            Object session = getSessionMethod.invoke(wrappedSession);
            Object isNew = jdbcSessionIsNewMethod.invoke(session);
            if (isNew != null && Boolean.class.isAssignableFrom(isNew.getClass()))
            {
                Boolean isNewRes = (Boolean) isNew;
                if (!isNewRes)
                {
                    clearChangeFlagsMethod.invoke(session);
                }
            }
        } catch (Exception e) {
            log.warn("Infinite session possible due to timeout-slider-prevention mechanism failure.", e);
        }
    }
}
