package io.github.mabartos.engine;

import io.github.mabartos.spi.engine.RiskScoreAlgorithm;
import io.github.mabartos.spi.engine.StoredRiskProvider;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.level.AdvancedRiskLevels;
import io.github.mabartos.spi.level.ResultRisk;
import io.github.mabartos.spi.level.RiskLevel;
import io.github.mabartos.spi.level.SimpleRiskLevels;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static io.github.mabartos.engine.RiskEvaluationAuditPublisher.AUTH_NOTE_BEFORE_AUTHN_EVALUATORS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

class RiskEvaluationAuditPublisherRecordFromStoredTest {

    private static final SimpleRiskLevels STANDARD_BANDS = new SimpleRiskLevels(
            new RiskLevel(SimpleRiskLevels.LOW, 0.0, 0.33),
            new RiskLevel(SimpleRiskLevels.MEDIUM, 0.33, 0.66),
            new RiskLevel(SimpleRiskLevels.HIGH, 0.66, 1.0)
    );

    @Test
    void recordLoginEvaluationFromStored_skipsWhenAuditDisabled() throws Exception {
        var stored = storedRiskProvider();
        stored.storeRisk(ResultRisk.of(0.55), RiskEvaluator.EvaluationPhase.USER_KNOWN);

        var publisher = publisher(RealmModelTestStub.realm(false, "CUSTOM_REQUIRED_ACTION"), stored);

        publisher.recordLoginEvaluationFromStored(
                RealmModelTestStub.realm(false, "CUSTOM_REQUIRED_ACTION"),
                user("user-1"));

        assertThat(pendingEvents(publisher), is(empty()));
    }

    @Test
    void recordLoginEvaluationFromStored_skipsWhenStoredRisksInvalid() throws Exception {
        var stored = storedRiskProvider();
        var realm = RealmModelTestStub.realm(true, "CUSTOM_REQUIRED_ACTION");
        var publisher = publisher(realm, stored);

        publisher.recordLoginEvaluationFromStored(realm, user("user-1"));

        assertThat(pendingEvents(publisher), is(empty()));
    }

    @Test
    void recordLoginEvaluationFromStored_queuesLoginSnapshotFromStoredProvider() throws Exception {
        var stored = storedRiskProvider();
        stored.storeRisk(ResultRisk.of(0.20), RiskEvaluator.EvaluationPhase.BEFORE_AUTHN);
        stored.storeRisk(ResultRisk.of(0.55), RiskEvaluator.EvaluationPhase.USER_KNOWN);
        stored.storeOverallRisk(ResultRisk.of(0.15));
        stored.storeAdditionalData(AUTH_NOTE_BEFORE_AUTHN_EVALUATORS, "BrowserRiskEvaluator=NONE");

        var realm = RealmModelTestStub.realm(true, "CUSTOM_REQUIRED_ACTION");
        var publisher = publisher(realm, stored);

        publisher.recordLoginEvaluationFromStored(realm, user("user-1"));

        var pending = pendingEvents(publisher);
        assertThat(pending.size(), is(1));
        var login = pending.getFirst();
        assertThat(login.getClass().getSimpleName(), is("LoginAuditEvent"));
        assertThat(invokeOptionalString(login, "userKnownScore"), is("0.5500"));
        assertThat(invokeOptionalString(login, "userKnownLevel"), is("MEDIUM"));
        assertThat(invokeOptionalString(login, "beforeAuthnScore"), is("0.2000"));
        assertThat(invokeOptionalString(login, "beforeAuthnLevel"), is("LOW"));
        assertThat(invokeOptionalString(login, "overallScore"), is("0.1500"));
        assertThat(invokeOptionalString(login, "overallLevel"), is("LOW"));
        assertThat(invokeOptionalString(login, "beforeAuthnEvaluators"), is("BrowserRiskEvaluator=NONE"));
        assertThat(invokeOptional(login, "userKnownEvaluators", Optional.class), is(Optional.empty()));
    }

    @Test
    void recordLoginEvaluationFromStored_queuesWhenOnlyOverallRiskValid() throws Exception {
        var stored = storedRiskProvider();
        stored.storeOverallRisk(ResultRisk.of(0.72));

        var realm = RealmModelTestStub.realm(true, "CUSTOM_REQUIRED_ACTION");
        var publisher = publisher(realm, stored);

        publisher.recordLoginEvaluationFromStored(realm, user("user-1"));

        assertThat(pendingEvents(publisher).size(), is(1));
        var login = pendingEvents(publisher).getFirst();
        assertThat(invokeOptionalString(login, "overallScore"), is("0.7200"));
        assertThat(invokeOptionalString(login, "overallLevel"), is("HIGH"));
        assertThat(invokeOptional(login, "userKnownScore", Optional.class), is(Optional.empty()));
    }

    private static RiskEvaluationAuditPublisher publisher(RealmModel realm, StoredRiskProvider stored) {
        var algorithm = testAlgorithm();
        var session = (KeycloakSession) Proxy.newProxyInstance(
                RiskEvaluationAuditPublisherRecordFromStoredTest.class.getClassLoader(),
                new Class[] {KeycloakSession.class},
                new SessionStubInvocationHandler(realm, stored, algorithm));
        return new RiskEvaluationAuditPublisher(session);
    }

    private static StoredRiskProvider storedRiskProvider() {
        return new InMemoryStoredRiskProvider();
    }

    private static UserModel user(String id) {
        return (UserModel) Proxy.newProxyInstance(
                RiskEvaluationAuditPublisherRecordFromStoredTest.class.getClassLoader(),
                new Class[] {UserModel.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getId" -> id;
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "UserModelStub(" + id + ")";
                    default -> null;
                });
    }

    private static RiskScoreAlgorithm testAlgorithm() {
        return new RiskScoreAlgorithm() {
            @Override
            @Nonnull
            public String getId() {
                return "test-algorithm";
            }

            @Override
            @Nonnull
            public SimpleRiskLevels getSimpleRiskLevels() {
                return STANDARD_BANDS;
            }

            @Override
            @Nonnull
            public AdvancedRiskLevels getAdvancedRiskLevels() {
                throw new UnsupportedOperationException();
            }

            @Override
            public ResultRisk evaluateRisk(
                    @Nonnull Set<RiskEvaluator> evaluators,
                    @Nonnull RiskEvaluator.EvaluationPhase phase,
                    @Nonnull RealmModel realm,
                    UserModel knownUser) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ResultRisk getOverallRisk() {
                return ResultRisk.invalid();
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static List<Object> pendingEvents(RiskEvaluationAuditPublisher publisher) throws Exception {
        var field = RiskEvaluationAuditPublisher.class.getDeclaredField("pending");
        field.setAccessible(true);
        return (List<Object>) field.get(publisher);
    }

    private static String invokeOptionalString(Object target, String methodName) throws Exception {
        return ((Optional<String>) invokeOptional(target, methodName, Optional.class)).orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static <T> T invokeOptional(Object target, String methodName, Class<T> returnType) throws Exception {
        var method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return (T) method.invoke(target);
    }

    private static final class SessionStubInvocationHandler implements InvocationHandler {
        private final RealmModel realm;
        private final StoredRiskProvider storedRiskProvider;
        private final RiskScoreAlgorithm algorithm;

        private SessionStubInvocationHandler(
                RealmModel realm,
                StoredRiskProvider storedRiskProvider,
                RiskScoreAlgorithm algorithm) {
            this.realm = realm;
            this.storedRiskProvider = storedRiskProvider;
            this.algorithm = algorithm;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, args);
            }
            return switch (method.getName()) {
                case "getProvider" -> provider((Class<?>) args[0]);
                case "getContext" -> context();
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "KeycloakSessionStub";
                default -> null;
            };
        }

        private Object provider(Class<?> providerClass) {
            if (providerClass == StoredRiskProvider.class) {
                return storedRiskProvider;
            }
            if (providerClass == RiskScoreAlgorithm.class) {
                return algorithm;
            }
            return null;
        }

        private KeycloakContext context() {
            return (KeycloakContext) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class[] {KeycloakContext.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getRealm" -> realm;
                        case "getConnection" -> null;
                        case "getAuthenticationSession" -> null;
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "KeycloakContextStub";
                        default -> null;
                    });
        }
    }

    private static final class InMemoryStoredRiskProvider implements StoredRiskProvider {
        private final Map<RiskEvaluator.EvaluationPhase, ResultRisk> byPhase = new EnumMap<>(RiskEvaluator.EvaluationPhase.class);
        private ResultRisk overallRisk = ResultRisk.invalid();
        private final Map<String, String> additionalData = new HashMap<>();

        @Override
        public ResultRisk getStoredOverallRisk() {
            return overallRisk;
        }

        @Override
        public ResultRisk getStoredRisk(@Nonnull RiskEvaluator.EvaluationPhase phase) {
            return byPhase.getOrDefault(phase, ResultRisk.invalid());
        }

        @Override
        public void storeOverallRisk(@Nonnull ResultRisk risk) {
            this.overallRisk = risk;
        }

        @Override
        public void storeRisk(@Nonnull ResultRisk risk, @Nonnull RiskEvaluator.EvaluationPhase phase) {
            byPhase.put(phase, risk);
        }

        @Override
        public void storeAdditionalData(String key, String value) {
            additionalData.put(key, value);
        }

        @Override
        public Optional<String> getAdditionalData(String key) {
            return Optional.ofNullable(additionalData.get(key));
        }

        @Override
        public void close() {
        }
    }
}
