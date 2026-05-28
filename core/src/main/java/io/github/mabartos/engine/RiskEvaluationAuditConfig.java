package io.github.mabartos.engine;

import org.keycloak.events.EventType;
import org.keycloak.models.RealmModel;

import java.util.stream.Collectors;

/**
 * Whether risk evaluation audit events are persisted uses the same mechanism as other user events:
 * realm events enabled and the event type listed under saved event types in Realm settings → Events.
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
   * <p>Returns {@code true} only when realm user events are enabled and
   * {@link #AUDIT_EVENT_TYPE_NAME} is explicitly listed under saved event types
   * (Realm settings → Events → Saved event types). An empty saved-types list
   * does not enable audit: operators must opt in by adding
   * {@code Custom required action}, regardless of Keycloak's
   * {@link EventType#isSaveByDefault()} for that type.
   *
   * @param realm realm configuration
   * @return {@code true} if audit events should be emitted
   */
  public static boolean isAuditEnabled(RealmModel realm) {
    if (!realm.isEventsEnabled()) {
      return false;
    }
    var enabledTypes = realm.getEnabledEventTypesStream().collect(Collectors.toSet());
    if (enabledTypes.isEmpty()) {
      return false;
    }
    return enabledTypes.contains(AUDIT_EVENT_TYPE_NAME);
  }
}
