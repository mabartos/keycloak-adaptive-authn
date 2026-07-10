package io.github.mabartos.audit.admin;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class RiskPoliciesSettingsSnapshotTest {

    @Test
    void diff_returnsOnlyChangedKeysWithOldNewValues() {
        var before = Map.of(
                "adaptive-engine-enabled", "true",
                "adaptive-engine-timeout", "2500"
        );
        var after = Map.of(
                "adaptive-engine-enabled", "false",
                "adaptive-engine-timeout", "2500"
        );

        var changes = RiskPoliciesSettingsSnapshot.diff(before, after);

        assertThat(changes.size(), is(1));
        assertThat(changes.get("adaptive-engine-enabled"), is("true > false"));
    }

    @Test
    void diff_emptyWhenNothingChanged() {
        var snapshot = Map.of("adaptive-engine-enabled", "true");
        assertThat(RiskPoliciesSettingsSnapshot.diff(snapshot, snapshot).isEmpty(), is(true));
    }
}
