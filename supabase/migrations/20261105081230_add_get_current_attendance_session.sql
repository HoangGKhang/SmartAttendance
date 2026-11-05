CREATE OR REPLACE FUNCTION
public.get_current_attendance_session(
    p_user_id uuid,
    p_meeting_id uuid
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = ''
AS $$
DECLARE
    v_course_id uuid;
    v_session_id uuid;
    v_created_by uuid;
    v_status text;
    v_opened_at timestamptz;
    v_expires_at timestamptz;
    v_closed_at timestamptz;
BEGIN
    -- 1. Kiểm tra đầu vào
    IF p_user_id IS NULL OR p_meeting_id IS NULL THEN
        RAISE EXCEPTION 'INVALID_SESSION_DATA'
            USING ERRCODE = 'P0001';
    END IF;

    -- 2. Kiểm tra Meeting tồn tại
    SELECT m.course_id
    INTO v_course_id
    FROM public.class_meetings m
    WHERE m.id = p_meeting_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'MEETING_NOT_FOUND'
            USING ERRCODE = 'P0001';
    END IF;

    -- 3. Kiểm tra quyền Lecturer
    IF NOT EXISTS (
        SELECT 1
        FROM public.courses c
        JOIN public.course_members cm
            ON cm.course_id = c.id
        WHERE c.id = v_course_id
          AND c.is_active = true
          AND cm.user_id = p_user_id
          AND cm.member_role = 'LECTURER'
          AND cm.is_active = true
    ) THEN
        RAISE EXCEPTION 'NOT_COURSE_LECTURER'
            USING ERRCODE = 'P0001';
    END IF;

    -- 4. Tìm Session OPEN
    SELECT
        s.id,
        s.created_by,
        s.status,
        s.opened_at,
        s.expires_at,
        s.closed_at
    INTO
        v_session_id,
        v_created_by,
        v_status,
        v_opened_at,
        v_expires_at,
        v_closed_at
    FROM public.attendance_sessions s
    WHERE s.meeting_id = p_meeting_id
      AND s.status = 'OPEN';

    -- 5. Chưa có Session
    IF NOT FOUND THEN
        RETURN jsonb_build_object(
            'session', NULL
        );
    END IF;

    -- 6. Trả về dữ liệu an toàn
    RETURN jsonb_build_object(
        'session', jsonb_build_object(
            'id', v_session_id,
            'meeting_id', p_meeting_id,
            'created_by', v_created_by,
            'status', v_status,
            'opened_at', v_opened_at,
            'expires_at', v_expires_at,
            'closed_at', v_closed_at
        )
    );
END;
$$;

-- Chỉ backend service role được gọi RPC
REVOKE ALL ON FUNCTION
public.get_current_attendance_session(uuid, uuid)
FROM PUBLIC, anon, authenticated;

GRANT EXECUTE ON FUNCTION
public.get_current_attendance_session(uuid, uuid)
TO service_role;