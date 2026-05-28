package io.github.mabartos.engine;

import org.junit.jupiter.api.Test;
import org.keycloak.events.EventType;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class RiskEvaluationAuditConfigTest {

    @Test
    void auditEventTypeUsesCustomRequiredActionUntilUpstreamTypeExists() {
        assertThat(RiskEvaluationAuditConfig.AUDIT_EVENT_TYPE, is(EventType.CUSTOM_REQUIRED_ACTION));
        assertThat(RiskEvaluationAuditConfig.AUDIT_EVENT_TYPE_NAME, is("CUSTOM_REQUIRED_ACTION"));
    }

    @Test
    void isAuditEnabled_falseWhenRealmEventsDisabled() {
        assertThat(
                RiskEvaluationAuditConfig.isAuditEnabled(
                        RealmModelTestStub.realm(false, "CUSTOM_REQUIRED_ACTION")),
                is(false));
    }

    @Test
    void isAuditEnabled_falseWhenSavedEventTypesEmpty() {
        assertThat(
                RiskEvaluationAuditConfig.isAuditEnabled(RealmModelTestStub.realm(true)),
                is(false));
    }

    @Test
    void isAuditEnabled_falseWhenAuditTypeNotListed() {
        assertThat(
                RiskEvaluationAuditConfig.isAuditEnabled(RealmModelTestStub.realm(true, "LOGIN")),
                is(false));
    }

    @Test
    void isAuditEnabled_trueWhenCustomRequiredActionListed() {
        assertThat(
                RiskEvaluationAuditConfig.isAuditEnabled(
                        RealmModelTestStub.realm(true, "CUSTOM_REQUIRED_ACTION")),
                is(true));
        assertThat(
                RiskEvaluationAuditConfig.isAuditEnabled(
                        RealmModelTestStub.realm(true, "LOGIN", "CUSTOM_REQUIRED_ACTION")),
                is(true));
    }
}
