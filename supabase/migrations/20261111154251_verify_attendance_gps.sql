
-- MODULE 8.5.1
-- Verify Student GPS against Attendance Session.
-- Backend-only RPC. Does not update attendance_records.

CREATE OR REPLACE FUNCTION public.verify_attendance_gps(
    p_user_id uuid,
    p_meeting_id uuid,
    p_session_id uuid,
    p_latitude double precision,
    p_longitude double precision,
    p_accuracy_meters double precision
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = ''
AS $$
DECLARE
    v_session public.attendance_sessions%ROWTYPE;
    v_course_id uuid;
    v_now timestamptz;

    v_lat1 double precision;
    v_lat2 double precision;
    v_delta_lat double precision;
    v_delta_lon double precision;

    v_a double precision;
    v_distance_meters double precision;

    v_earth_radius_meters constant double precision := 6371000.0;
BEGIN
    -- 1. Validate incoming data.
    IF p_user_id IS NULL
       OR p_meeting_id IS NULL
       OR p_session_id IS NULL
       OR p_latitude IS NULL
       OR p_longitude IS NULL
       OR p_accuracy_meters IS NULL
       OR p_latitude NOT BETWEEN -90 AND 90
       OR p_longitude NOT BETWEEN -180 AND 180
       OR p_accuracy_meters NOT BETWEEN 0 AND 30
    THEN
        RAISE EXCEPTION 'INVALID_GPS_DATA'
            USING ERRCODE = 'P0001';
    END IF;

    -- 2. Load the exact Session and Meeting.
    SELECT s.*
    INTO v_session
    FROM public.attendance_sessions s
    WHERE s.id = p_session_id
      AND s.meeting_id = p_meeting_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'SESSION_NOT_FOUND'
            USING ERRCODE = 'P0001';
    END IF;

    -- 3. Validate Session state using server time.
    v_now := clock_timestamp();

    IF v_session.status <> 'OPEN'
       OR v_session.opened_at IS NULL
       OR v_session.expires_at IS NULL
       OR v_session.opened_at > v_now
       OR v_session.expires_at <= v_now
    THEN
        RAISE EXCEPTION 'SESSION_UNAVAILABLE'
            USING ERRCODE = 'P0001';
    END IF;

    -- 4. Fail closed if GPS configuration is missing.
    IF v_session.require_gps IS DISTINCT FROM TRUE
       OR v_session.latitude IS NULL
       OR v_session.longitude IS NULL
       OR v_session.radius_meters IS NULL
       OR v_session.latitude NOT BETWEEN -90 AND 90
       OR v_session.longitude NOT BETWEEN -180 AND 180
       OR v_session.radius_meters NOT BETWEEN 10 AND 200
    THEN
        RAISE EXCEPTION 'GPS_NOT_CONFIGURED'
            USING ERRCODE = 'P0001';
    END IF;

    -- 5. Validate Meeting and Course.
    SELECT m.course_id
    INTO v_course_id
    FROM public.class_meetings m
    JOIN public.courses c
        ON c.id = m.course_id
    WHERE m.id = p_meeting_id
      AND m.status = 'SCHEDULED'
      AND c.is_active = true;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'MEETING_UNAVAILABLE'
            USING ERRCODE = 'P0001';
    END IF;

    -- 6. Student must be an active Course member.
    IF NOT EXISTS (
        SELECT 1
        FROM public.course_members cm
        WHERE cm.course_id = v_course_id
          AND cm.user_id = p_user_id
          AND cm.member_role = 'STUDENT'
          AND cm.is_active = true
    ) THEN
        RAISE EXCEPTION 'NOT_ENROLLED'
            USING ERRCODE = 'P0001';
    END IF;

    -- 7. Convert latitude/longitude from degrees to radians.
    v_lat1 := radians(v_session.latitude);
    v_lat2 := radians(p_latitude);

    v_delta_lat :=
        radians(p_latitude - v_session.latitude);

    v_delta_lon :=
        radians(p_longitude - v_session.longitude);

    -- 8. Haversine calculation.
    v_a :=
        power(sin(v_delta_lat / 2), 2)
        + cos(v_lat1) * cos(v_lat2)
        * power(sin(v_delta_lon / 2), 2);

    -- Clamp for floating-point rounding safety.
    v_a := least(1.0, greatest(0.0, v_a));

    v_distance_meters :=
        2 * v_earth_radius_meters * asin(sqrt(v_a));

    -- 9. Return verification only.
    -- No database write and no PRESENT update.
    RETURN jsonb_build_object(
        'verified',
        v_distance_meters <= v_session.radius_meters
    );
END;
$$;

-- 10. Restrict direct RPC execution.
REVOKE ALL ON FUNCTION public.verify_attendance_gps(
    uuid, uuid, uuid,
    double precision, double precision, double precision
)
FROM PUBLIC, anon, authenticated;

GRANT EXECUTE ON FUNCTION public.verify_attendance_gps(
    uuid, uuid, uuid,
    double precision, double precision, double precision
)
TO service_role;
