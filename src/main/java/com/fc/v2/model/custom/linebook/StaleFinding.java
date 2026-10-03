package com.fc.v2.model.custom.linebook;

/**
 * 一条"来历不对"的旧结论：屏上挂着某线代号，线册里却查不到申请注明那一天
 * 窗口罩得住的同名线（代号已删，或那天它根本不在场窗口内）。
 *
 * <p>注意：那条线如今<b>已停用</b>不算来历不对——当年它在场期间判的旧卷要能复算，
 * "按下不等于撕掉"，{@link LineBook#windowCovers} 不看眼下停用栏。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class StaleFinding {

    /** 来历不对的缘由 */
    public enum Reason {
        /** 线册里已查无此代号 */
        CODE_GONE,
        /** 代号还在，但启用/交棒窗口罩不住申请注明那一天 */
        OUT_OF_WINDOW
    }

    /** 结论快照主键 */
    private final Long verdictId;

    /** 申报人代号 */
    private final String applicantCode;

    /** 哪一头挂着这个来历不对的线代号 */
    private final LineSide side;

    /** 屏上挂着的线代号 */
    private final String lineCode;

    /** 缘由 */
    private final Reason reason;

    public StaleFinding(Long verdictId, String applicantCode, LineSide side,
                        String lineCode, Reason reason) {
        this.verdictId = verdictId;
        this.applicantCode = applicantCode;
        this.side = side;
        this.lineCode = lineCode;
        this.reason = reason;
    }

    public Long getVerdictId() {
        return verdictId;
    }

    public String getApplicantCode() {
        return applicantCode;
    }

    public LineSide getSide() {
        return side;
    }

    public String getLineCode() {
        return lineCode;
    }

    public Reason getReason() {
        return reason;
    }
}
