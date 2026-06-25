package io.github.mabartos.evaluator.login;

import io.github.mabartos.context.user.LoginEventsContext;
import io.github.mabartos.spi.level.Risk;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.keycloak.common.util.Time;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static io.github.mabartos.spi.level.Risk.Score.HIGH;
import static io.github.mabartos.spi.level.Risk.Score.MEDIUM;
import static io.github.mabartos.spi.level.Risk.Score.NONE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class FailedLoginPatternRiskEvaluatorTest {

    @Test
    void detectsDistributedAttackFromManyIps() {
        long now = Time.currentTimeMillis();
        List<Event> failures = List.of(
                loginError(now - Duration.ofMinutes(10).toMillis(), "10.0.0.1"),
                loginError(now - Duration.ofMinutes(8).toMillis(), "10.0.0.2"),
                loginError(now - Duration.ofMinutes(6).toMillis(), "10.0.0.3"),
                loginError(now - Duration.ofMinutes(4).toMillis(), "10.0.0.4"),
                loginError(now - Duration.ofMinutes(2).toMillis(), "10.0.0.5")
        );
        List<Event> successes = List.of(
                login(now - Duration.ofMinutes(30).toMillis(), "10.0.0.1")
        );

        FailedLoginPatternRiskEvaluator evaluator = evaluator(successes, failures);
        Risk risk = evaluator.evaluate(null, anyUser());

        assertThat(risk.getScore(), is(HIGH));
    }

    @Test
    void detectsModerateMultiIpPattern() {
        long now = Time.currentTimeMillis();
        List<Event> failures = List.of(
                loginError(now - Duration.ofMinutes(10).toMillis(), "10.0.0.1"),
                loginError(now - Duration.ofMinutes(6).toMillis(), "10.0.0.2"),
                loginError(now - Duration.ofMinutes(2).toMillis(), "10.0.0.3")
        );
        List<Event> successes = List.of(
                login(now - Duration.ofMinutes(30).toMillis(), "10.0.0.1")
        );

        FailedLoginPatternRiskEvaluator evaluator = evaluator(successes, failures);
        Risk risk = evaluator.evaluate(null, anyUser());

        assertThat(risk.getScore(), is(MEDIUM));
    }

    @Test
    void singleIpFailuresReturnNone() {
        long now = Time.currentTimeMillis();
        List<Event> failures = List.of(
                loginError(now - Duration.ofMinutes(10).toMillis(), "10.0.0.1"),
                loginError(now - Duration.ofMinutes(7).toMillis(), "10.0.0.1"),
                loginError(now - Duration.ofMinutes(3).toMillis(), "10.0.0.1"),
                loginError(now - Duration.ofMinutes(2).toMillis(), "10.0.0.1"),
                loginError(now - Duration.ofMinutes(1).toMillis(), "10.0.0.1")
        );
        List<Event> successes = List.of(
                login(now - Duration.ofMinutes(30).toMillis(), "10.0.0.1")
        );

        FailedLoginPatternRiskEvaluator evaluator = evaluator(successes, failures);
        Risk risk = evaluator.evaluate(null, anyUser());

        assertThat(risk.getScore(), is(NONE));
    }

    @Test
    void detectsBotLikeTimingPattern() {
        long now = Time.currentTimeMillis();
        long interval = 5000;
        List<Event> failures = List.of(
                loginError(now - interval * 5 + 10, "10.0.0.1"),
                loginError(now - interval * 4 + 20, "10.0.0.1"),
                loginError(now - interval * 3 + 5, "10.0.0.1"),
                loginError(now - interval * 2 + 15, "10.0.0.1"),
                loginError(now - interval, "10.0.0.1")
        );
        List<Event> successes = List.of(
                login(now - Duration.ofMinutes(30).toMillis(), "10.0.0.1")
        );

        FailedLoginPatternRiskEvaluator evaluator = evaluator(successes, failures);
        Risk risk = evaluator.evaluate(null, anyUser());

        assertThat(risk.getScore(), is(MEDIUM));
    }

    @Test
    void irregularTimingDoesNotTrigger() {
        long now = Time.currentTimeMillis();
        List<Event> failures = List.of(
                loginError(now - Duration.ofMinutes(50).toMillis(), "10.0.0.1"),
                loginError(now - Duration.ofMinutes(30).toMillis(), "10.0.0.1"),
                loginError(now - Duration.ofMinutes(28).toMillis(), "10.0.0.1"),
                loginError(now - Duration.ofMinutes(5).toMillis(), "10.0.0.1"),
                loginError(now - Duration.ofMinutes(1).toMillis(), "10.0.0.1")
        );
        List<Event> successes = List.of(
                login(now - Duration.ofMinutes(55).toMillis(), "10.0.0.1")
        );

        FailedLoginPatternRiskEvaluator evaluator = evaluator(successes, failures);
        Risk risk = evaluator.evaluate(null, anyUser());

        assertThat(risk.getScore(), is(NONE));
    }

    @Test
    void returnsNoneWhenOnlySuccessfulLoginsAreLoaded() {
        long now = Time.currentTimeMillis();
        List<Event> successes = List.of(
                login(now - Duration.ofHours(2).toMillis(), "10.0.0.1"),
                login(now - Duration.ofHours(1).toMillis(), "10.0.0.1"),
                login(now - Duration.ofMinutes(30).toMillis(), "10.0.0.1")
        );

        FailedLoginPatternRiskEvaluator evaluator = evaluator(successes, List.of());
        Risk risk = evaluator.evaluate(null, anyUser());

        assertThat(risk.getScore(), is(NONE));
    }

    private static FailedLoginPatternRiskEvaluator evaluator(List<Event> successes, List<Event> failures) {
        return new FailedLoginPatternRiskEvaluator(fixedEvents(successes), fixedEvents(failures));
    }

    private static LoginEventsContext fixedEvents(List<Event> events) {
        return new LoginEventsContext(null) {
            @Override
            public EventType[] eventTypes() {
                return new EventType[0];
            }

            @Override
            public Optional<List<Event>> initData(@Nonnull RealmModel realm, @Nullable UserModel knownUser) {
                return Optional.of(events);
            }

            @Override
            public Optional<List<Event>> getData(@Nonnull RealmModel realm, @Nullable UserModel knownUser) {
                return Optional.of(events);
            }
        };
    }

    private static UserModel anyUser() {
        return (UserModel) Proxy.newProxyInstance(
                UserModel.class.getClassLoader(),
                new Class[]{UserModel.class},
                (proxy, method, args) -> {
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) {
                        return false;
                    }
                    if (returnType == int.class) {
                        return 0;
                    }
                    if (returnType == long.class) {
                        return 0L;
                    }
                    return null;
                });
    }

    private static Event loginError(long time, String ip) {
        return event(EventType.LOGIN_ERROR, time, ip);
    }

    private static Event login(long time, String ip) {
        return event(EventType.LOGIN, time, ip);
    }

    private static Event event(EventType type, long time, String ip) {
        Event event = new Event();
        event.setType(type);
        event.setTime(time);
        event.setIpAddress(ip);
        return event;
    }
}
