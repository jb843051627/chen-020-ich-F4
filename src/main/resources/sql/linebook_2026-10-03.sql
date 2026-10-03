-- ============================================================================
-- 线册判据（linebook）增量建表脚本
-- 日期：2026-10-03
--
-- 头一条：给线册 t_ich_pre_line 添"管项"一栏，分清这条线管的是
--         本人从艺年数（YEARS）还是带徒人数（DISCIPLES）。
--         旧线此栏留空：老的"按线代号取值"那条路不看这一栏，照旧不动；
--         新判定出口只取两栏管项的线，空管项旧线不进两头判定。
-- 第二条：结论落库表 t_ich_line_verdict——判定出口给的回话原样钉死，
--         只插不改；同申请人同申请注明时刻重问只回放旧结论，不回算、不覆盖。
-- ============================================================================

ALTER TABLE `t_ich_pre_line`
    ADD COLUMN `line_kind` VARCHAR(16) NULL COMMENT '管项代号 YEARS从艺年数 DISCIPLES带徒人数（空值仅走按代号取值老路）'
    AFTER `priority`;


CREATE TABLE IF NOT EXISTS `t_ich_line_verdict` (
    `id`                BIGINT(20)     NOT NULL COMMENT '主键',
    `applicant_code`    VARCHAR(64)    NOT NULL COMMENT '申报人代号',
    `apply_at`          DATETIME       NOT NULL COMMENT '申请注明的那一刻（按此刻回看线册，不用手里最新那条顶替）',

    `verdict_status`    VARCHAR(16)    NOT NULL COMMENT '判定回话 PASS两头都不越 FAIL有头越了或申报数不像话 NO_LINE此刻够不着任何一条不给结论',

    `years_input`       DECIMAL(10,2)  NULL COMMENT '本人从艺年数（申报原数）',
    `years_state`       VARCHAR(16)    NULL COMMENT '从艺年数这头落点 JUDGED INPUT_BAD NO_LINE LINE_BAD',
    `years_line_code`   VARCHAR(64)    NULL COMMENT '从艺年数那一头依哪条线判（钉线代号）',
    `years_eff_start`   DATETIME       NULL COMMENT '从艺年数那条线的启用之日（钉启用之日）',
    `years_level`       INT(4)         NULL COMMENT '从艺年数落在哪一层 1起分段 2过线段 3优线段 4越过优线',

    `disciples_input`   DECIMAL(10,2)  NULL COMMENT '带徒人数（申报原数）',
    `disciples_state`   VARCHAR(16)    NULL COMMENT '带徒人数这头落点 JUDGED INPUT_BAD NO_LINE LINE_BAD',
    `disciples_line_code` VARCHAR(64)  NULL COMMENT '带徒人数那一头依哪条线判（钉线代号）',
    `disciples_eff_start` DATETIME     NULL COMMENT '带徒人数那条线的启用之日（钉启用之日）',
    `disciples_level`   INT(4)         NULL COMMENT '带徒人数落在哪一层 1起分段 2过线段 3优线段 4越过优线',

    `fail_detail`       VARCHAR(512)   NULL COMMENT '哪头越了就把那一头的数落在哪；没线/数不像话也逐头写明，不许只回一句没通过',

    `del_flag`          INT(1)         NOT NULL DEFAULT 0 COMMENT '删除标记 0正常 1删除',
    `create_by`         VARCHAR(64)    NULL COMMENT '创建者',
    `create_time`       DATETIME       NULL COMMENT '创建时间',
    `update_by`         VARCHAR(64)    NULL COMMENT '更新者',
    `update_time`       DATETIME       NULL COMMENT '更新时间',
    `remark`            VARCHAR(255)   NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_applicant_apply` (`applicant_code`, `apply_at`),
    KEY `idx_apply_at` (`apply_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf88mb4 COMMENT='申报准入判定结论快照（线代号与启用之日随结论钉死，换线不回头改）';
