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
}
