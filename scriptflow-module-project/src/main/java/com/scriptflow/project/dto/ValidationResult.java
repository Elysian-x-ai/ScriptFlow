package com.scriptflow.project.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ValidationResult {
    private boolean valid = true;
    private List<String> errors = new ArrayList<>();

    public static ValidationResult ok() {
        return new ValidationResult();
    }

    public static ValidationResult failed(List<String> errors) {
        ValidationResult r = new ValidationResult();
        r.setValid(false);
        r.setErrors(errors);
        return r;
    }
}
