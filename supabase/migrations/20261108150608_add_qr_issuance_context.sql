
BEGIN;

CREATE OR REPLACE FUNCTION
public.get_qr_issuance_context(
    p_user_id uuid,
    p_session_id uuid
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = ''
AS $$
DECLARE
    v_session public.attendance_sessions%ROWTYPE;

    v_course_id uuid;
    v_meeting_status text;
    v_course_active boolean;

    v_now timestamptz;

BEGIN

    -- 1. Kiểm tra đầu vào.
    IF p_user_id IS NULL
       OR p_session_id IS NULL
    THEN
        RAISE EXCEPTION 'INVALID_QR_REQUEST'
            USING ERRCODE = 'P0001';
    END IF;


    -- 2. Tìm Session.
    SELECT s.*
    INTO v_session
    FROM public.attendance_sessions s
    WHERE s.id = p_session_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'SESSION_NOT_FOUND'
            USING ERRCODE = 'P0001';
    END IF;


    -- 3. Tìm Course và trạng thái Meeting.
    SELECT
        m.course_id,
        m.status,
        c.is_active
    INTO
        v_course_id,
        v_meeting_status,
        v_course_active
    FROM public.class_meetings m
    JOIN public.courses c
        ON c.id = m.course_id
    WHERE m.id = v_session.meeting_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'MEETING_NOT_FOUND'
            USING ERRCODE = 'P0001';
    END IF;


    -- 4. Kiểm tra Lecturer thuộc Course.
    IF NOT EXISTS (
        SELECT 1
        FROM public.course_members cm
        WHERE cm.course_id = v_course_id
          AND cm.user_id = p_user_id
          AND cm.member_role = 'LECTURER'
          AND cm.is_active = true
    ) THEN
        RAISE EXCEPTION 'NOT_COURSE_LECTURER'
            USING ERRCODE = 'P0001';
    END IF;


    -- 5. Lấy thời gian hiện tại của server.
    v_now := clock_timestamp();


    -- 6. Kiểm tra hiệu lực Session.
    IF v_course_active IS DISTINCT FROM true
       OR v_meeting_status <> 'SCHEDULED'
       OR v_session.status <> 'OPEN'
       OR v_session.require_qr IS DISTINCT FROM true
       OR v_session.opened_at IS NULL
       OR v_session.expires_at IS NULL
       OR v_session.opened_at > v_now
       OR v_session.expires_at <= v_now
    THEN
        RAISE EXCEPTION 'QR_NOT_AVAILABLE'
            USING ERRCODE = 'P0001';
    END IF;


    -- 7. Kiểm tra cấu hình bí mật QR.
    IF v_session.qr_secret !~ '^[0-9a-f]{64}$'
       OR v_session.qr_refresh_seconds < 5
    THEN
        RAISE EXCEPTION 'INVALID_QR_CONFIGURATION'
            USING ERRCODE = 'P0001';
    END IF;


    -- 8. Chỉ trả dữ liệu cho backend đặc quyền.
    RETURN jsonb_build_object(
        'session_id', v_session.id,
        'qr_secret', v_session.qr_secret,
        'qr_refresh_seconds',
            v_session.qr_refresh_seconds,
        'session_expires_at',
            v_session.expires_at,
        'server_time', v_now
    );

END;
$$;


-- 9. Không cho client thường gọi RPC.
REVOKE ALL ON FUNCTION
public.get_qr_issuance_context(uuid, uuid)
FROM PUBLIC, anon, authenticated;


-- 10. Chỉ backend service_role được gọi.
GRANT EXECUTE ON FUNCTION
public.get_qr_issuance_context(uuid, uuid)
TO service_role;

COMMIT;
