
CREATE OR REPLACE FUNCTION public.close_attendance_session(
    p_user_id uuid,
    p_session_id uuid
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = ''
AS $$
DECLARE
    v_meeting_id uuid;
    v_course_id uuid;
    v_session public.attendance_sessions%ROWTYPE;
BEGIN
    -- 1. Kiểm tra đầu vào.
    IF p_user_id IS NULL OR p_session_id IS NULL THEN
        RAISE EXCEPTION 'INVALID_SESSION_DATA'
            USING ERRCODE = 'P0001';
    END IF;

    -- 2. Tìm Meeting của Session cần đóng.
    SELECT s.meeting_id
    INTO v_meeting_id
    FROM public.attendance_sessions s
    WHERE s.id = p_session_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'SESSION_NOT_FOUND'
            USING ERRCODE = 'P0001';
    END IF;

    -- 3. Khóa Meeting để đồng bộ với luồng tạo Session.
    SELECT m.course_id
    INTO v_course_id
    FROM public.class_meetings m
    WHERE m.id = v_meeting_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'MEETING_NOT_FOUND'
            USING ERRCODE = 'P0001';
    END IF;

    -- 4. Kiểm tra người dùng là Lecturer đang hoạt động.
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

    -- 5. Khóa và đọc Session hiện tại.
    SELECT s.*
    INTO v_session
    FROM public.attendance_sessions s
    WHERE s.id = p_session_id
      AND s.meeting_id = v_meeting_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'SESSION_NOT_FOUND'
            USING ERRCODE = 'P0001';
    END IF;

    -- 6. Kiểm tra trạng thái.
    IF v_session.status = 'CLOSED' THEN
        RAISE EXCEPTION 'SESSION_ALREADY_CLOSED'
            USING ERRCODE = 'P0001';
    END IF;

    IF v_session.status <> 'OPEN' THEN
        RAISE EXCEPTION 'SESSION_NOT_OPEN'
            USING ERRCODE = 'P0001';
    END IF;

    -- 7. Cập nhật Session thành CLOSED.
    UPDATE public.attendance_sessions
    SET
        status = 'CLOSED',
        closed_at = now(),
        updated_at = now()
    WHERE id = p_session_id
      AND status = 'OPEN'
    RETURNING *
    INTO v_session;

    -- 8. Trả về dữ liệu an toàn.
    RETURN jsonb_build_object(
        'session', jsonb_build_object(
            'id', v_session.id,
            'meeting_id', v_session.meeting_id,
            'created_by', v_session.created_by,
            'status', v_session.status,
            'opened_at', v_session.opened_at,
            'expires_at', v_session.expires_at,
            'closed_at', v_session.closed_at
        )
    );
END;
$$;

-- Chỉ cho phép Backend service_role gọi RPC.
REVOKE ALL ON FUNCTION
public.close_attendance_session(uuid, uuid)
FROM PUBLIC, anon, authenticated;

GRANT EXECUTE ON FUNCTION
public.close_attendance_session(uuid, uuid)
TO service_role;
