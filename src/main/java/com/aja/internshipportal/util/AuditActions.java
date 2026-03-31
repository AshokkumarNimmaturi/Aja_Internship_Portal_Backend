package com.aja.internshipportal.util;

// Constants for all audit log action names
// use these everywhere instead of raw strings
// prevents typos like "USER_CREATE" vs "USER_CREATED"
public class AuditActions {

    private AuditActions() {}

    // User actions
    public static final String USER_REGISTERED     = "USER_REGISTERED";
    public static final String USER_CREATED        = "USER_CREATED";
    public static final String USER_UPDATED        = "USER_UPDATED";
    public static final String USER_DEACTIVATED    = "USER_DEACTIVATED";
    public static final String USER_LOGIN          = "USER_LOGIN";
    public static final String PASSWORD_CHANGED    = "PASSWORD_CHANGED";
    public static final String PASSWORD_RESET      = "PASSWORD_RESET";
    public static final String FORGOT_PASSWORD     = "FORGOT_PASSWORD";

    // Question actions
    public static final String QUESTION_SUBMITTED  = "QUESTION_SUBMITTED";
    public static final String QUESTION_APPROVED   = "QUESTION_APPROVED";
    public static final String QUESTION_REJECTED   = "QUESTION_REJECTED";

    // Answer actions
    public static final String ANSWER_ADDED        = "ANSWER_ADDED";
    public static final String ANSWER_UPVOTED      = "ANSWER_UPVOTED";

    // Payment actions
    public static final String PAYMENT_ORDER_CREATED = "PAYMENT_ORDER_CREATED";
    public static final String PAYMENT_SUCCESS     = "PAYMENT_SUCCESS";
    public static final String PAYMENT_FAILED      = "PAYMENT_FAILED";

    // Subscription actions
    public static final String SUBSCRIPTION_CREATED = "SUBSCRIPTION_CREATED";
}