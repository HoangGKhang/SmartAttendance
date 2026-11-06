
-- MODULE 6.6: Lazy Expiration khi tạo Session

CREATE OR REPLACE FUNCTION public.create_attendance_session(
    p_user_id uuid,
    p_meeting_id uuid,
    p_duration_minutes integer DEFAULT 15
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path TO ''
AS $function$
DECLARE
    v_meeting_status text;
    v_course_id uuid;
    v_opened_at timestamptz;
    v_now timestamptz;
    v_session public.attendance_sessions%ROWTYPE;
BEGIN
    -- 1. Kiểm tra đầu vào.
    IF p_user_id IS NULL
       OR p_meeting_id IS NULL
       OR p_duration_minutes IS NULL
       OR p_duration_minutes < 1
       OR p_duration_minutes > 180
    THEN
        RAISE EXCEPTION 'INVALID_SESSION_DATA'
            USING ERRCODE = 'P0001';
    END IF;

    -- 2. Khóa Meeting để đồng bộ thao tác.
    SELECT m.course_id, m.status
    INTO v_course_id, v_meeting_status
    FROM public.class_meetings m
    WHERE m.id = p_meeting_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'MEETING_NOT_FOUND'
            USING ERRCODE = 'P0001';
    END IF;

    -- 3. Chỉ cho phép Meeting SCHEDULED.
    IF v_meeting_status <> 'SCHEDULED' THEN
        RAISE EXCEPTION 'MEETING_NOT_SCHEDULED'
            USING ERRCODE = 'P0001';
    END IF;

    -- 4. Xác minh quyền Lecturer.
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

    -- 5. MỚI: Đóng Session OPEN đã hết hạn.
    v_now := clock_timestamp();

    UPDATE public.attendance_sessions s
    SET
        status = 'CLOSED',
        closed_at = s.expires_at,
        updated_at = v_now
    WHERE s.meeting_id = p_meeting_id
      AND s.status = 'OPEN'
      AND s.expires_at IS NOT NULL
      AND s.expires_at <= v_now;

    -- 6. Kiểm tra còn Session OPEN không.
    IF EXISTS (
        SELECT 1
        FROM public.attendance_sessions s
        WHERE s.meeting_id = p_meeting_id
          AND s.status = 'OPEN'
    ) THEN
        RAISE EXCEPTION 'SESSION_ALREADY_OPEN'
            USING ERRCODE = 'P0001';
    END IF;

    -- 7. Tạo Session mới.
    v_opened_at := clock_timestamp();

    INSERT INTO public.attendance_sessions (
        meeting_id,
        created_by,
        status,
        opened_at,
        expires_at
    )
    VALUES (
        p_meeting_id,
        p_user_id,
        'OPEN',
        v_opened_at,
        v_opened_at
            + make_interval(mins => p_duration_minutes)
    )
    RETURNING * INTO v_session;

    -- 8. Giữ nguyên cấu trúc Response cũ.
    RETURN jsonb_build_object(
        'id', v_session.id,
        'meeting_id', v_session.meeting_id,
        'created_by', v_session.created_by,
        'status', v_session.status,
        'opened_at', v_session.opened_at,
        'expires_at', v_session.expires_at,
        'closed_at', v_session.closed_at
    );
END;
$function$;

-- Chỉ Backend service_role được thực thi.
REVOKE ALL ON FUNCTION
public.create_attendance_session(uuid,uuid,integer)
FROM PUBLIC, anon, authenticated;

GRANT EXECUTE ON FUNCTION
public.create_attendance_session(uuid,uuid,integer)
TO service_role;


-- ====================================================
-- MODULE 6.6.3 — GET CURRENT ACTIVE SESSION
-- ====================================================

CREATE OR REPLACE FUNCTION
public.get_current_attendance_session(
    p_user_id uuid,
    p_meeting_id uuid
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path TO ''
AS $function$
DECLARE
    v_course_id uuid;
    v_now timestamptz;
    v_session public.attendance_sessions%ROWTYPE;
BEGIN
    -- 1. Kiểm tra đầu vào.
    IF p_user_id IS NULL OR p_meeting_id IS NULL THEN
        RAISE EXCEPTION 'INVALID_SESSION_DATA'
            USING ERRCODE = 'P0001';
    END IF;

    -- 2. Kiểm tra Meeting tồn tại.
    SELECT m.course_id
    INTO v_course_id
    FROM public.class_meetings m
    WHERE m.id = p_meeting_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'MEETING_NOT_FOUND'
            USING ERRCODE = 'P0001';
    END IF;

    -- 3. Kiểm tra quyền Lecturer.
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

    -- 4. Lấy thời gian hiện tại của Server.
    v_now := clock_timestamp();

    -- 5. Chỉ lấy Session OPEN còn hiệu lực.
    SELECT s.*
    INTO v_session
    FROM public.attendance_sessions s
    WHERE s.meeting_id = p_meeting_id
      AND s.status = 'OPEN'
      AND s.opened_at <= v_now
      AND s.expires_at > v_now;

    -- 6. Không tìm thấy phiên còn hiệu lực.
    IF NOT FOUND THEN
        RETURN jsonb_build_object(
            'session', NULL
        );
    END IF;

    -- 7. Trả về thông tin Session.
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
$function$;

-- Chỉ cho phép backend service_role thực thi.
REVOKE ALL ON FUNCTION
public.get_current_attendance_session(uuid,uuid)
FROM PUBLIC, anon, authenticated;

GRANT EXECUTE ON FUNCTION
public.get_current_attendance_session(uuid,uuid)
TO service_role;

