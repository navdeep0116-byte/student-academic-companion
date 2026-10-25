package com.academiccompanion.backend.dto;

import java.util.List;

public class LevelEligibilityDto {

    private boolean eligible;
    private List<String> requiredInputs;
    private List<String> missingInputs;
    private String reason;

    public LevelEligibilityDto() {
    }

    public LevelEligibilityDto(boolean eligible, List<String> requiredInputs, List<String> missingInputs, String reason) {
        this.eligible = eligible;
        this.requiredInputs = requiredInputs;
        this.missingInputs = missingInputs;
        this.reason = reason;
    }

    public boolean isEligible() {
        return eligible;
    }

    public void setEligible(boolean eligible) {
        this.eligible = eligible;
    }

    public List<String> getRequiredInputs() {
        return requiredInputs;
    }

    public void setRequiredInputs(List<String> requiredInputs) {
        this.requiredInputs = requiredInputs;
    }

    public List<String> getMissingInputs() {
        return missingInputs;
    }

    public void setMissingInputs(List<String> missingInputs) {
        this.missingInputs = missingInputs;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
