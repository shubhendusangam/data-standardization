package com.datastd.quality.entity;

/**
 * Severity of a validation rule.
 * ERROR violations block the job; WARNING violations allow the job but annotate it.
 */
public enum Severity {
    ERROR,
    WARNING
}

