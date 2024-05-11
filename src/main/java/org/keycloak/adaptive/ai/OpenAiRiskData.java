package org.keycloak.adaptive.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenAiRiskData(Double risk, String reason) {
}
