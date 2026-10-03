package com.fc.v2.model.custom.linebook;

import java.math.BigDecimal;

/**
 * 批量判定里一位申报人的两头申报数（申请注明那一刻整批共用）。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class ApplicantInput {

    /** 申报人代号 */
    private String applicantCode;

    /** 本人从艺年数 */
    private BigDecimal yearsInput;

    /** 带徒人数 */
    private BigDecimal disciplesInput;

    public ApplicantInput() {
    }

    public ApplicantInput(String applicantCode, BigDecimal yearsInput, BigDecimal disciplesInput) {
        this.applicantCode = applicantCode;
        this.yearsInput = yearsInput;
        this.disciplesInput = disciplesInput;
    }

    public String getApplicantCode() {
        return applicantCode;
    }

    public void setApplicantCode(String applicantCode) {
        this.applicantCode = applicantCode;
    }

    public BigDecimal getYearsInput() {
        return yearsInput;
    }

    public void setYearsInput(BigDecimal yearsInput) {
        this.yearsInput = yearsInput;
    }

    public BigDecimal getDisciplesInput() {
        return disciplesInput;
    }

    public void setDisciplesInput(BigDecimal disciplesInput) {
        this.disciplesInput = disciplesInput;
    }
}
