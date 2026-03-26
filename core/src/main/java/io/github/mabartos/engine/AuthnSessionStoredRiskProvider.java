/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.mabartos.engine;

import io.github.mabartos.spi.level.ResultRisk;
import io.github.mabartos.spi.engine.StoredRiskProvider;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jboss.logging.Logger;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.KeycloakSession;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.StringUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Pure storage provider for risk data in authentication session attributes.
 * Contains no business logic - algorithms decide what to store and how to combine.
 */
public class AuthnSessionStoredRiskProvider implements StoredRiskProvider {
    private static final Logger logger = Logger.getLogger(AuthnSessionStoredRiskProvider.class);

    protected static final String PREFIX = "ADAPTIVE_AUTHN_";
    protected static final String OVERALL_SCORE_KEY = PREFIX + "OVERALL_SCORE";
    protected static final String OVERALL_REASON_KEY = PREFIX + "OVERALL_REASON";
    protected static final String MULTI_VALUE_DELIMITER = ";";
    // Tracks which attribute keys are stored for a phase, so we can reconstruct the full map
    protected static final String PHASE_KEYS_SUFFIX = "_KEYS";

    private final KeycloakSession session;

    public AuthnSessionStoredRiskProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void storePhaseAttributes(@Nonnull RiskEvaluator.EvaluationPhase phase, @Nonnull MultivaluedHashMap<String, String> attributes) {
        var authSession = getAuthSessionOrThrow();

        var keyNames = new StringBuilder();
        for (var entry : attributes.entrySet()) {
            var noteKey = phaseNoteKey(phase, entry.getKey());
            var serialized = String.join(MULTI_VALUE_DELIMITER, entry.getValue());
            authSession.setAuthNote(noteKey, serialized);

            if (!keyNames.isEmpty()) {
                keyNames.append(MULTI_VALUE_DELIMITER);
            }
            keyNames.append(entry.getKey());
        }

        // Store the list of keys so we can reconstruct the map later
        authSession.setAuthNote(phaseNoteKey(phase, PHASE_KEYS_SUFFIX), keyNames.toString());
    }

    @Override
    @Nonnull
    public MultivaluedHashMap<String, String> getPhaseAttributes(@Nonnull RiskEvaluator.EvaluationPhase phase) {
        var authSession = getAuthSessionOrNull();
        if (authSession == null) {
            return new MultivaluedHashMap<>();
        }

        var keysNote = authSession.getAuthNote(phaseNoteKey(phase, PHASE_KEYS_SUFFIX));
        if (StringUtil.isBlank(keysNote)) {
            return new MultivaluedHashMap<>();
        }

        var result = new MultivaluedHashMap<String, String>();
        for (var key : keysNote.split(MULTI_VALUE_DELIMITER)) {
            var noteValue = authSession.getAuthNote(phaseNoteKey(phase, key));
            if (noteValue != null) {
                result.put(key, Arrays.asList(noteValue.split(MULTI_VALUE_DELIMITER)));
            }
        }
        return result;
    }

    @Override
    public void storePhaseAttribute(@Nonnull RiskEvaluator.EvaluationPhase phase, @Nonnull String key, @Nonnull String value) {
        getAuthSessionOrThrow().setAuthNote(phaseNoteKey(phase, key), value);
    }

    @Override
    @Nullable
    public String getPhaseAttribute(@Nonnull RiskEvaluator.EvaluationPhase phase, @Nonnull String key) {
        var authSession = getAuthSessionOrNull();
        if (authSession == null) return null;

        var value = authSession.getAuthNote(phaseNoteKey(phase, key));
        if (StringUtil.isBlank(value)) return null;

        // Return the first value if multi-valued
        var delimiterIndex = value.indexOf(MULTI_VALUE_DELIMITER);
        return delimiterIndex >= 0 ? value.substring(0, delimiterIndex) : value;
    }

    @Override
    @Nonnull
    public List<String> getPhaseAttributeValues(@Nonnull RiskEvaluator.EvaluationPhase phase, @Nonnull String key) {
        var authSession = getAuthSessionOrNull();
        if (authSession == null) return Collections.emptyList();

        var value = authSession.getAuthNote(phaseNoteKey(phase, key));
        if (StringUtil.isBlank(value)) return Collections.emptyList();

        return Arrays.asList(value.split(MULTI_VALUE_DELIMITER));
    }

    @Override
    public void storeOverallRisk(@Nonnull ResultRisk risk) {
        if (!risk.isValid()) {
            logger.warnf("Cannot store the invalid risk score '%f'", risk.getScore());
            return;
        }

        var authSession = getAuthSessionOrThrow();
        authSession.setAuthNote(OVERALL_SCORE_KEY, Double.toString(risk.getScore()));
        authSession.setAuthNote(OVERALL_REASON_KEY, risk.getSummary().orElse(""));
    }

    @Override
    @Nonnull
    public ResultRisk getStoredOverallRisk() {
        try {
            return Optional.ofNullable(getAuthSessionOrNull())
                    .map(f -> {
                        var score = f.getAuthNote(OVERALL_SCORE_KEY);
                        if (StringUtil.isBlank(score) || score.equals("-1")) {
                            return ResultRisk.invalid();
                        }
                        var reason = f.getAuthNote(OVERALL_REASON_KEY);
                        return ResultRisk.of(Double.parseDouble(score), reason);
                    })
                    .filter(ResultRisk::isValid)
                    .orElse(ResultRisk.invalid());
        } catch (NumberFormatException e) {
            return ResultRisk.invalid();
        }
    }

    private String phaseNoteKey(@Nonnull RiskEvaluator.EvaluationPhase phase, @Nonnull String key) {
        return PREFIX + phase.name() + "_" + key;
    }

    @Nullable
    private AuthenticationSessionModel getAuthSessionOrNull() {
        return session.getContext().getAuthenticationSession();
    }

    private AuthenticationSessionModel getAuthSessionOrThrow() {
        return Optional.ofNullable(session.getContext().getAuthenticationSession())
                .orElseThrow(() -> new IllegalStateException("Authentication session is null"));
    }

    @Override
    public void close() {
    }
}
