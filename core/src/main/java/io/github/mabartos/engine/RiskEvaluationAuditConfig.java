package io.github.mabartos.engine;

import org.keycloak.events.EventType;
import org.keycloak.models.RealmModel;

import java.util.Optional;
import java.util.stream.Collectors;

import static io.github.mabartos.ui.RiskBasedPoliciesUiTab.AUDIT_EVENTS_ENABLED_CONFIG;

/**
 * Whether risk evaluation audit events are persisted uses the adaptive toggle in Risk-based policies
 * plus realm user events configuration (saved event types).
 *
 * <p>Keycloak does not allow extensions to register new {@link EventType} enum values. Until an upstream
 * {@code ADAPTIVE_RISK_EVALUATION} type exists, events use {@link EventType#CUSTOM_REQUIRED_ACTION} with
 * detail {@link RiskEvaluationAuditPublisher#DETAIL_SUBTYPE}={@link RiskEvaluationAuditPublisher#SUBTYPE_LOGIN}.
 */
public final class RiskEvaluationAuditConfig {

  /**
   * Event type saved in realm {@code enabledEventTypes} to activate risk evaluation audit.
   * Operators enable it in Admin Console: Realm settings → Events → User events settings → Saved event types.
   */
  public static final EventType AUDIT_EVENT_TYPE = EventType.CUSTOM_REQUIRED_ACTION;

  public static final String AUDIT_EVENT_TYPE_NAME = AUDIT_EVENT_TYPE.name();

  private RiskEvaluationAuditConfig() {
  }

  /**
   * Whether risk evaluation audit events should be persisted for this realm.
   *
   * <p>Returns {@code true} only when:
   * <ol>
   *   <li>{@link AUDIT_EVENTS_ENABLED_CONFIG} is {@code true} on the realm (Risk-based policies tab),</li>
   *   <li>realm user events are enabled, and</li>
   *   <li>{@link #AUDIT_EVENT_TYPE_NAME} is explicitly listed under saved event types.</li>
   * </ol>
   * An empty saved-types list does not enable audit.
   *
   * @param realm realm configuration
   * @return {@code true} if audit events should be emitted
   */
  public static boolean isAuditEnabled(RealmModel realm) {
    if (!isAdaptiveAuditEnabled(realm)) {
      return false;
    }
    if (!realm.isEventsEnabled()) {
      return false;
    }
    var enabledTypes = realm.getEnabledEventTypesStream().collect(Collectors.toSet());
    if (enabledTypes.isEmpty()) {
      return false;
    }
    return enabledTypes.contains(AUDIT_EVENT_TYPE_NAME);
  }

  static boolean isAdaptiveAuditEnabled(RealmModel realm) {
    return Optional.ofNullable(realm.getAttribute(AUDIT_EVENTS_ENABLED_CONFIG))
        .map(Boolean::parseBoolean)
        .orElse(false);
  }
}
