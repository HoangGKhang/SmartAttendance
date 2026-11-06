
-- =====================================================
-- MODULE 6.6.4
-- TỰ ĐỘNG ĐÓNG ATTENDANCE SESSION HẾT HẠN
-- =====================================================

-- 1. Kích hoạt pg_cron nếu chưa có.
CREATE EXTENSION IF NOT EXISTS pg_cron
WITH SCHEMA pg_catalog;

-- 2. Đăng ký job chạy mỗi phút.
SELECT cron.schedule(
    'smartattendance-expire-sessions',
    '* * * * *',
    $cron$
        UPDATE public.attendance_sessions
        SET
            status = 'CLOSED',
            closed_at = expires_at,
            updated_at = statement_timestamp()
        WHERE status = 'OPEN'
          AND expires_at IS NOT NULL
          AND expires_at <= statement_timestamp();
    $cron$
);
