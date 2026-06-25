# Privacy and Data Anonymization

## Overview

When using AI services (Claude, OpenAI, IBM Granite) for risk evaluation, this extension sends authentication context data to third-party APIs. To protect user privacy and comply with regulations like GDPR and CCPA, **data anonymization is enabled by default**.

## Why Anonymization Matters

### Privacy Risks Without Anonymization

Sending raw user data to AI services poses several risks:

1. **GDPR Violation** - Article 5 requires data minimization
2. **Third-party Data Processing** - AI provider becomes a data processor
3. **Data Retention** - Unknown how long AI providers retain request data
4. **Potential Re-identification** - IP addresses can reveal user identity/location
5. **No User Consent** - Users didn't explicitly agree to AI analysis

### Real-World Example

**Without anonymization:**
```
IP: 203.0.113.42 (reveals: San Francisco, CA, USA)
Device: Chrome 120.0.6099.129 on Windows 11 Pro Build 22621
```
→ Potentially identifying, violates GDPR data minimization

**With anonymization:**
```
IP: ip_a3f5b2c1 (hash - no location revealed)
Device: Desktop Device on Windows
```
→ Privacy-preserving, pattern detection still works

## How Anonymization Works

### IP Address Hashing

IP addresses are hashed using SHA-256 with a configurable salt:

```
Real IP: 192.0.2.100
→ Hash: SHA256("192.0.2.100" + salt)
→ Truncate: a3f5b2c1
→ Result: "ip_a3f5b2c1"
```

**Benefits:**
- ✅ **Same IP = Same hash** - Pattern detection works
- ✅ **Irreversible** - Cannot recover original IP
- ✅ **Salted** - Prevents rainbow table attacks
- ✅ **Truncated** - Shorter hashes in prompts

### Device Information Generalization

Device fingerprints are reduced to generic categories:

| Before (Identifying) | After (Anonymous) |
|---------------------|-------------------|
| Chrome 120.0 on Windows 11 Pro | Desktop Device on Windows |
| Safari 17.2 on iPhone 15 Pro Max iOS 17.3 | Mobile Device on iOS |
| Firefox 121.0 on Ubuntu 22.04 LTS | Desktop Device on Linux |

### What Gets Anonymized

| Data Type | Original | Anonymized | Detection Works? |
|-----------|----------|------------|------------------|
| IP Address | `203.0.113.42` | `ip_a3f5b2c1` | ✅ Same IP = Same hash |
| Device | `Chrome 120 on Win 11` | `Desktop on Windows` | ✅ Device type changes detected |
| Geolocation | City/Street | ❌ Not sent | ⚠️ Use IP hash patterns |
| Username | ❌ Never sent | ❌ Never sent | N/A |
| Email | ❌ Never sent | ❌ Never sent | N/A |

## Configuration

### Enable/Disable Anonymization

```bash
# Enabled by default for privacy protection
export AI_ANONYMIZE_ENABLED=true

# Disable only for testing/development
export AI_ANONYMIZE_ENABLED=false
```

Or in Keycloak configuration:
```properties
ai.anonymize.enabled=true
```

### Configure Salt (Required for Production!)

**⚠️ CRITICAL:** Set a strong, unique salt in production:

```bash
# Generate a strong random salt
export AI_ANONYMIZE_SALT=$(openssl rand -base64 32)

# Or use a custom value
export AI_ANONYMIZE_SALT=your-unique-production-salt-here
```

**Why salt matters:**
- Without salt, attackers could build rainbow tables
- Same salt across systems allows cross-correlation
- **Never commit salt to source control!**

## GDPR Compliance

### Data Controller Responsibilities

When using AI-powered risk evaluation:

1. **✅ Data Minimization** - Anonymization ensures minimal data sent
2. **✅ Purpose Limitation** - Data used only for fraud detection
3. **⚠️ Data Processing Agreement** - Establish DPA with AI provider (Anthropic/OpenAI)
4. **⚠️ Privacy Policy** - Inform users about AI-based authentication
5. **⚠️ Data Subject Rights** - Implement deletion/access procedures

### Recommended Privacy Notice

Add to your privacy policy:

> **AI-Powered Fraud Detection**
>
> To protect your account from unauthorized access, we use AI-powered risk analysis.
> When you log in, anonymized authentication data (hashed IP addresses, generic device type)
> is sent to our AI service provider for fraud pattern detection. No personally identifiable
> information is shared. This processing is necessary for the security of your account
> (GDPR Article 6(1)(f) - legitimate interest).

## Impact on Detection Accuracy

### What Still Works

✅ **Pattern Detection** - AI can still detect:
- Multiple logins from different IP hashes in short time
- Device type changes (mobile → desktop)
- Unusual timing patterns
- Suspicious action sequences

### What's Reduced

⚠️ **Geolocation Precision** - Cannot detect:
- Specific cities (only IP hash patterns)
- "Impossible travel" requires IP hash correlation over time

### Mitigation

Use separate geolocation evaluators that don't send data to AI:
- `KnownLocationRiskEvaluator` - Compares to known safe locations
- `InitLocationRiskEvaluator` - Detects first-time locations

These run locally and don't send data to third-party APIs.

### Device fingerprinting

`KnownDeviceRiskEvaluator` (FingerprintJS extension) uses the open-source [FingerprintJS](https://github.com/fingerprintjs/fingerprintjs) library (MIT, v5.2.0) bundled in the extension theme. Fingerprinting runs entirely in the browser; only a hashed visitor identifier is posted back to Keycloak on login.

- The library is loaded from the Keycloak server (`fingerprintjs-v5.min.js`).
- `monitoring: false` is set when initializing FingerprintJS (no telemetry to FingerprintJS).
- Known device IDs are stored on the user profile (`adaptive-device-known` attribute) with last-seen timestamp in epoch seconds.
- Entries past the configured TTL are excluded from the active set, if no active devices remain, the next login is scored like a first tracked device (`VERY_SMALL`) until the device is registered again on successful login.
- If fingerprinting fails on a user's first login, risk stays low (`VERY_SMALL`), same as first-time location. After devices are tracked, a missing fingerprint is treated as an unknown device and may increase risk.

## Security Considerations

### Salt Management

**DO:**
- ✅ Use a strong random salt (32+ characters)
- ✅ Store salt in secure configuration (Vault, secrets manager)
- ✅ Rotate salt periodically (invalidates old hashes)
- ✅ Use different salts per environment (dev/staging/prod)

**DON'T:**
- ❌ Use default salt in production
- ❌ Commit salt to Git
- ❌ Reuse salts across systems
- ❌ Share salt with AI provider

### Disabling Anonymization

**Only disable for:**
- Local development/testing
- Compliance-exempt environments
- When you have explicit DPA with AI provider

**Never disable for:**
- Production with EU/California users
- Healthcare/financial data
- Public-facing applications

## Troubleshooting

### Check Anonymization Status

Enable debug logging:
```bash
--log-level=io.github.mabartos:debug
```

You'll see:
```
AI Account Takeover prompt for user john.doe:
PRIVACY NOTE: All IP addresses and device info have been anonymized...
IP Identifier: ip_a3f5b2c1
```

### Verify Configuration

Check if anonymization is properly configured:
```java
boolean configured = AiDataAnonymizer.isProperlyConfigured();
// false = using default salt (warning)
// true = custom salt configured or disabled
```

### Performance Impact

Anonymization overhead:
- **Hashing:** ~0.1ms per IP address
- **Device generalization:** Negligible
- **Overall impact:** < 1% of AI API latency

## Alternative Approaches

If anonymization doesn't meet your needs:

1. **On-Premise AI** - Run AI models locally (no data leaves infrastructure)
2. **Rule-Based Only** - Disable AI evaluators, use deterministic rules
3. **DPA with AI Provider** - Establish formal data processing agreement
4. **Aggregate Analysis** - Batch anonymized data for offline analysis

## Further Reading

- [GDPR Article 5 - Data Minimization](https://gdpr-info.eu/art-5-gdpr/)
- [CCPA Data Minimization Requirements](https://oag.ca.gov/privacy/ccpa)
- [Anthropic Privacy Policy](https://www.anthropic.com/legal/privacy)
- [OpenAI Data Usage Policy](https://openai.com/policies/usage-policies)
