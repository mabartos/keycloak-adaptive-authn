package io.github.mabartos.spi.level;

/**
 * Specific category representing a risk score in a specified range, in order to react on the risk scores
 *
 * @param name              name of the risk level category
 * @param lowestRiskValue   lowest risk score value for the category in range (0,1>
 * @param highestRiskValue  highest risk score value for the category in range (0,1>
 */
public record RiskLevel(String name, double lowestRiskValue, double highestRiskValue) {

    /**
     * Check whether the provided `riskValue` matches the risk level category
     *
     * @param riskValue risk score to be checked
     * @return true if the `riskValue` complies with the risk level score range
     */
    public boolean matchesRisk(double riskValue) {
        if (lowestRiskValue() == 0.0f && riskValue == lowestRiskValue()) return true;
        return riskValue > lowestRiskValue() && riskValue <= highestRiskValue();
    }
}
