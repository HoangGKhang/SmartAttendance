
-- MODULE 8.3.1
-- Create Attendance Session with GPS
-- Chỉ Backend service_role được phép thực thi.

CREATE OR REPLACE FUNCTION
public.create_attendance_session_with_gps(
    p_user_id uuid,
    p_meeting_id uuid,
    p_duration_minutes integer,
    p_latitude double precision,
    p_longitude double precision,
    p_radius_meters integer
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = ''
AS $function$

DECLARE
    v_meeting_status text;
    v_course_id uuid;
    v_opened_at timestamptz;
    v_now timestamptz;
    v_session public.attendance_sessions%ROWTYPE;

BEGIN

    -- 1. Kiểm tra dữ liệu đầu vào.
    IF p_user_id IS NULL
       OR p_meeting_id IS NULL
       OR p_duration_minutes IS NULL
       OR p_duration_minutes NOT BETWEEN 1 AND 180
       OR p_latitude IS NULL
       OR p_longitude IS NULL
       OR p_radius_meters IS NULL
       OR p_latitude NOT BETWEEN -90 AND 90
       OR p_longitude NOT BETWEEN -180 AND 180
       OR p_radius_meters NOT BETWEEN 10 AND 200
    THEN
        RAISE EXCEPTION 'INVALID_SESSION_DATA'
            USING ERRCODE = 'P0001';
    END IF;

    -- 2. Khóa Meeting để tránh tạo Session đồng thời.
    SELECT m.course_id, m.status
    INTO v_course_id, v_meeting_status
    FROM public.class_meetings m
    WHERE m.id = p_meeting_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'MEETING_NOT_FOUND'
            USING ERRCODE = 'P0001';
    END IF;

    -- 3. Meeting phải đang SCHEDULED.
    IF v_meeting_status <> 'SCHEDULED' THEN
        RAISE EXCEPTION 'MEETING_NOT_SCHEDULED'
            USING ERRCODE = 'P0001';
    END IF;

    -- 4. Xác minh Lecturer và Course.
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

    -- 5. Đóng Session OPEN đã hết hạn.
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

    -- 6. Không cho mở hai Session cùng Meeting.
    IF EXISTS (
        SELECT 1
        FROM public.attendance_sessions s
        WHERE s.meeting_id = p_meeting_id
          AND s.status = 'OPEN'
    ) THEN
        RAISE EXCEPTION 'SESSION_ALREADY_OPEN'
            USING ERRCODE = 'P0001';
    END IF;

    -- 7. Tạo Session và lưu GPS trong một transaction.
    v_opened_at := clock_timestamp();

    INSERT INTO public.attendance_sessions (
        meeting_id,
        created_by,
        status,
        opened_at,
        expires_at,
        require_gps,
        latitude,
        longitude,
        radius_meters
    )
    VALUES (
        p_meeting_id,
        p_user_id,
        'OPEN',
        v_opened_at,
        v_opened_at
            + make_interval(mins => p_duration_minutes),
        true,
        p_latitude,
        p_longitude,
        p_radius_meters
    )
    RETURNING * INTO v_session;

    -- 8. Trả thông tin Session.
    -- Không trả qr_secret.
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

-- 9. Giới hạn quyền thực thi.
REVOKE ALL ON FUNCTION
public.create_attendance_session_with_gps(
    uuid, uuid, integer,
    double precision, double precision, integer
)
FROM PUBLIC, anon, authenticated;

GRANT EXECUTE ON FUNCTION
public.create_attendance_session_with_gps(
    uuid, uuid, integer,
    double precision, double precision, integer
)
TO service_role;
