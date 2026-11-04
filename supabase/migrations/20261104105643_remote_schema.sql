SET local check_function_bodies = off;

CREATE SCHEMA "private";

CREATE TABLE "public"."attendance_records" (
  "id"                  uuid                     NOT NULL DEFAULT gen_random_uuid(),
  "meeting_id"          uuid                     NOT NULL,
  "student_id"          uuid                     NOT NULL,
  "session_id"          uuid,
  "status"              character varying(20)    NOT NULL DEFAULT 'ABSENT'::character varying,
  "checked_in_at"       timestamp with time zone,
  "qr_verified"         boolean                  NOT NULL DEFAULT false,
  "gps_verified"        boolean                  NOT NULL DEFAULT false,
  "face_verified"       boolean                  NOT NULL DEFAULT false,
  "gps_latitude"        double precision,
  "gps_longitude"       double precision,
  "gps_distance_meters" double precision,
  "face_score"          double precision,
  "created_at"          timestamp with time zone NOT NULL DEFAULT now(),
  "updated_at"          timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT "attendance_records_distance_check" CHECK (((gps_distance_meters IS NULL) OR (gps_distance_meters >= (0)::double precision))),
  CONSTRAINT "attendance_records_face_score_check" CHECK (((face_score IS NULL) OR ((face_score >= ('-1'::integer)::double precision) AND (face_score <= (1)::double precision)))),
  CONSTRAINT "attendance_records_latitude_check"
    CHECK (((gps_latitude IS NULL) OR ((gps_latitude >= ('-90'::integer)::double precision) AND (gps_latitude <= (90)::double precision)))),
  CONSTRAINT "attendance_records_longitude_check"
    CHECK (((gps_longitude IS NULL) OR ((gps_longitude >= ('-180'::integer)::double precision) AND (gps_longitude <= (180)::double precision)))),
  CONSTRAINT "attendance_records_meeting_student_unique" UNIQUE (meeting_id, student_id),
  CONSTRAINT "attendance_records_pkey" PRIMARY KEY (id),
  CONSTRAINT "attendance_records_status_check" CHECK (((status)::text = ANY ((ARRAY['PRESENT'::character varying, 'ABSENT'::character varying])::text[])))
);

ALTER TABLE "public"."attendance_records"
  ENABLE ROW LEVEL SECURITY;

REVOKE ALL ON TABLE "public"."attendance_records" FROM "anon";

CREATE TABLE "public"."attendance_sessions" (
  "id"                 uuid                     NOT NULL DEFAULT gen_random_uuid(),
  "meeting_id"         uuid                     NOT NULL,
  "created_by"         uuid                     NOT NULL,
  "status"             character varying(20)    NOT NULL DEFAULT 'DRAFT'::character varying,
  "opened_at"          timestamp with time zone,
  "closed_at"          timestamp with time zone,
  "expires_at"         timestamp with time zone,
  "qr_secret"          text                     NOT NULL DEFAULT encode(extensions.gen_random_bytes(32), 'hex'::text),
  "qr_refresh_seconds" integer                  NOT NULL DEFAULT 10,
  "latitude"           double precision,
  "longitude"          double precision,
  "radius_meters"      integer,
  "require_qr"         boolean                  NOT NULL DEFAULT true,
  "require_gps"        boolean                  NOT NULL DEFAULT true,
  "require_face"       boolean                  NOT NULL DEFAULT true,
  "created_at"         timestamp with time zone NOT NULL DEFAULT now(),
  "updated_at"         timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT "attendance_sessions_latitude_check" CHECK (((latitude IS NULL) OR ((latitude >= ('-90'::integer)::double precision) AND (latitude <= (90)::double precision)))),
  CONSTRAINT "attendance_sessions_longitude_check" CHECK (((longitude IS NULL) OR ((longitude >= ('-180'::integer)::double precision) AND (longitude <= (180)::double precision)))),
  CONSTRAINT "attendance_sessions_pkey" PRIMARY KEY (id),
  CONSTRAINT "attendance_sessions_qr_refresh_seconds_check" CHECK ((qr_refresh_seconds >= 5)),
  CONSTRAINT "attendance_sessions_radius_meters_check" CHECK ((radius_meters > 0)),
  CONSTRAINT "attendance_sessions_status_check"
    CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'OPEN'::character varying, 'CLOSED'::character varying])::text[]))),
  CONSTRAINT "attendance_sessions_time_check" CHECK (((opened_at IS NULL) OR (closed_at IS NULL) OR (opened_at <= closed_at)))
);

ALTER TABLE "public"."attendance_sessions"
  ENABLE ROW LEVEL SECURITY;

REVOKE ALL ON TABLE "public"."attendance_sessions" FROM "anon";

CREATE TABLE "public"."class_meetings" (
  "id"             uuid                     NOT NULL DEFAULT gen_random_uuid(),
  "course_id"      uuid                     NOT NULL,
  "meeting_number" integer                  NOT NULL,
  "meeting_date"   date                     NOT NULL,
  "title"          character varying(150),
  "start_time"     time without time zone,
  "end_time"       time without time zone,
  "room"           character varying(100),
  "status"         character varying(20)    NOT NULL DEFAULT 'SCHEDULED'::character varying,
  "created_at"     timestamp with time zone NOT NULL DEFAULT now(),
  "updated_at"     timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT "class_meetings_course_number_unique" UNIQUE (course_id, meeting_number),
  CONSTRAINT "class_meetings_pkey" PRIMARY KEY (id),
  CONSTRAINT "class_meetings_status_check"
    CHECK (((status)::text = ANY ((ARRAY['SCHEDULED'::character varying, 'COMPLETED'::character varying, 'CANCELLED'::character varying])::text[]))),
  CONSTRAINT "class_meetings_time_check" CHECK (((start_time IS NULL) OR (end_time IS NULL) OR (start_time < end_time)))
);

ALTER TABLE "public"."class_meetings"
  ENABLE ROW LEVEL SECURITY;

REVOKE ALL ON TABLE "public"."class_meetings" FROM "anon";

CREATE TABLE "public"."course_members" (
  "id"          uuid                     NOT NULL DEFAULT gen_random_uuid(),
  "course_id"   uuid                     NOT NULL,
  "user_id"     uuid                     NOT NULL,
  "member_role" character varying(20)    NOT NULL,
  "is_active"   boolean                  NOT NULL DEFAULT true,
  "joined_at"   timestamp with time zone NOT NULL DEFAULT now(),
  "created_at"  timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT "course_members_course_user_unique" UNIQUE (course_id, user_id),
  CONSTRAINT "course_members_member_role_check" CHECK (((member_role)::text = ANY ((ARRAY['STUDENT'::character varying, 'LECTURER'::character varying])::text[]))),
  CONSTRAINT "course_members_pkey" PRIMARY KEY (id)
);

ALTER TABLE "public"."course_members"
  ENABLE ROW LEVEL SECURITY;

REVOKE ALL ON TABLE "public"."course_members" FROM "anon";

CREATE TABLE "public"."courses" (
  "id"               uuid                     NOT NULL DEFAULT gen_random_uuid(),
  "moodle_course_id" bigint,
  "course_code"      character varying(20)    NOT NULL,
  "class_code"       character varying(50)    NOT NULL,
  "name"             character varying(200)   NOT NULL,
  "semester"         character varying(20)    NOT NULL,
  "academic_year"    character varying(20)    NOT NULL,
  "start_date"       date,
  "end_date"         date,
  "is_active"        boolean                  NOT NULL DEFAULT true,
  "created_at"       timestamp with time zone NOT NULL DEFAULT now(),
  "updated_at"       timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT "courses_class_semester_year_unique" UNIQUE (class_code, semester, academic_year),
  CONSTRAINT "courses_date_check" CHECK (((start_date IS NULL) OR (end_date IS NULL) OR (start_date <= end_date))),
  CONSTRAINT "courses_moodle_course_id_key" UNIQUE (moodle_course_id),
  CONSTRAINT "courses_pkey" PRIMARY KEY (id)
);

ALTER TABLE "public"."courses"
  ENABLE ROW LEVEL SECURITY;

REVOKE ALL ON TABLE "public"."courses" FROM "anon";

CREATE TABLE "public"."moodle_user_provisioning" (
  "id"               uuid                     NOT NULL DEFAULT gen_random_uuid(),
  "moodle_user_id"   bigint                   NOT NULL,
  "supabase_user_id" uuid,
  "status"           character varying(30)    NOT NULL DEFAULT 'PENDING'::character varying,
  "attempt_count"    integer                  NOT NULL DEFAULT 0,
  "last_error"       text,
  "created_at"       timestamp with time zone NOT NULL DEFAULT now(),
  "updated_at"       timestamp with time zone NOT NULL DEFAULT now(),
  "synced_at"        timestamp with time zone,
  CONSTRAINT "moodle_user_provisioning_moodle_user_id_key" UNIQUE (moodle_user_id),
  CONSTRAINT "moodle_user_provisioning_pkey" PRIMARY KEY (id),
  CONSTRAINT "moodle_user_provisioning_supabase_user_id_key" UNIQUE (supabase_user_id),
  CONSTRAINT "provisioning_attempt_check" CHECK ((attempt_count >= 0)),
  CONSTRAINT "provisioning_linked_user_check" CHECK ((((status)::text <> 'LINKED'::text) OR (supabase_user_id IS NOT NULL))),
  CONSTRAINT "provisioning_status_check"
    CHECK
    (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'AUTH_CREATED'::character varying, 'LINKED'::character varying, 'FAILED'::character varying,
    'REVIEW_REQUIRED'::character varying])::text[])))
);

ALTER TABLE "public"."moodle_user_provisioning"
  ENABLE ROW LEVEL SECURITY;

REVOKE ALL ON TABLE "public"."moodle_user_provisioning" FROM "anon", "authenticated";

CREATE TABLE "public"."profiles" (
  "id"             uuid                     NOT NULL,
  "student_code"   character varying(20),
  "full_name"      character varying(150)   NOT NULL,
  "role"           character varying(20)    NOT NULL,
  "moodle_user_id" bigint,
  "created_at"     timestamp with time zone NOT NULL DEFAULT now(),
  "updated_at"     timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT "profiles_moodle_user_id_key" UNIQUE (moodle_user_id),
  CONSTRAINT "profiles_pkey" PRIMARY KEY (id),
  CONSTRAINT "profiles_role_check" CHECK (((role)::text = ANY ((ARRAY['STUDENT'::character varying, 'LECTURER'::character varying, 'ADMIN'::character varying])::text[]))),
  CONSTRAINT "profiles_student_code_key" UNIQUE (student_code)
);

ALTER TABLE "public"."profiles"
  ENABLE ROW LEVEL SECURITY;

REVOKE ALL ON TABLE "public"."profiles" FROM "anon";

CREATE OR REPLACE FUNCTION private.can_access_meeting (
  p_meeting_id uuid
)
  RETURNS boolean
  LANGUAGE sql
  STABLE
  SECURITY DEFINER
  SET search_path TO ''
  AS $function$
    select exists (
        select 1
        from public.class_meetings m
        join public.course_members cm
          on cm.course_id = m.course_id
        where m.id = p_meeting_id
          and cm.user_id = (select auth.uid())
          and cm.is_active = true
    );
$function$;

CREATE OR REPLACE FUNCTION private.is_course_lecturer (
  p_course_id uuid
)
  RETURNS boolean
  LANGUAGE sql
  STABLE
  SECURITY DEFINER
  SET search_path TO ''
  AS $function$
    select exists (
        select 1
        from public.course_members cm
        where cm.course_id = p_course_id
          and cm.user_id = (select auth.uid())
          and cm.member_role = 'LECTURER'
          and cm.is_active = true
    );
$function$;

CREATE OR REPLACE FUNCTION private.is_course_member (
  p_course_id uuid
)
  RETURNS boolean
  LANGUAGE sql
  STABLE
  SECURITY DEFINER
  SET search_path TO ''
  AS $function$
    select exists (
        select 1
        from public.course_members cm
        where cm.course_id = p_course_id
          and cm.user_id = (select auth.uid())
          and cm.is_active = true
    );
$function$;

CREATE OR REPLACE FUNCTION private.lecturer_can_access_meeting (
  p_meeting_id uuid
)
  RETURNS boolean
  LANGUAGE sql
  STABLE
  SECURITY DEFINER
  SET search_path TO ''
  AS $function$
    select exists (
        select 1
        from public.class_meetings m
        join public.course_members cm
          on cm.course_id = m.course_id
        where m.id = p_meeting_id
          and cm.user_id = (select auth.uid())
          and cm.member_role = 'LECTURER'
          and cm.is_active = true
    );
$function$;

CREATE OR REPLACE FUNCTION private.lecturer_can_view_profile (
  p_profile_id uuid
)
  RETURNS boolean
  LANGUAGE sql
  STABLE
  SECURITY DEFINER
  SET search_path TO ''
  AS $function$
    select exists (
        select 1
        from public.course_members target_member
        join public.course_members lecturer_member
          on lecturer_member.course_id = target_member.course_id
        where target_member.user_id = p_profile_id
          and target_member.is_active = true

          and lecturer_member.user_id = (select auth.uid())
          and lecturer_member.member_role = 'LECTURER'
          and lecturer_member.is_active = true
    );
$function$;

CREATE OR REPLACE FUNCTION public.create_attendance_session (
  p_user_id          uuid,
  p_meeting_id       uuid,
  p_duration_minutes integer DEFAULT 15
)
  RETURNS jsonb
  LANGUAGE plpgsql
  SET search_path TO ''
  AS $function$
DECLARE
    v_meeting_status text;
    v_course_id uuid;
    v_opened_at timestamptz;
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

    -- 2. Lấy Meeting và khóa bản ghi
    -- để tuần tự hóa các thao tác mở phiên
    -- cùng một Meeting qua RPC này.
    SELECT m.course_id, m.status
    INTO v_course_id, v_meeting_status
    FROM public.class_meetings m
    WHERE m.id = p_meeting_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'MEETING_NOT_FOUND'
            USING ERRCODE = 'P0001';
    END IF;

    -- 3. Chỉ Meeting đang SCHEDULED
    -- mới cho phép mở Session.
    IF v_meeting_status <> 'SCHEDULED' THEN
        RAISE EXCEPTION 'MEETING_NOT_SCHEDULED'
            USING ERRCODE = 'P0001';
    END IF;

    -- 4. Kiểm tra quyền giảng viên
    -- và khóa học còn active.
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

    -- 5. Không cho mở phiên thứ hai.
    IF EXISTS (
        SELECT 1
        FROM public.attendance_sessions s
        WHERE s.meeting_id = p_meeting_id
          AND s.status = 'OPEN'
    ) THEN
        RAISE EXCEPTION 'SESSION_ALREADY_OPEN'
            USING ERRCODE = 'P0001';
    END IF;

    -- 6. Tạo Session.
    v_opened_at := now();

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

    -- 7. Chỉ trả thông tin an toàn.
    -- KHÔNG trả qr_secret.
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

REVOKE ALL ON FUNCTION "public"."create_attendance_session"(uuid, uuid, integer) FROM PUBLIC, "anon", "authenticated";

CREATE OR REPLACE FUNCTION public.create_meeting_with_records (
  p_user_id        uuid,
  p_course_id      uuid,
  p_meeting_number integer,
  p_meeting_date   date,
  p_title          text                   DEFAULT NULL::text,
  p_start_time     time without time zone DEFAULT NULL::time WITHOUT time zone,
  p_end_time       time without time zone DEFAULT NULL::time WITHOUT time zone,
  p_room           text                   DEFAULT NULL::text
)
  RETURNS public.class_meetings
  LANGUAGE plpgsql
  SET search_path TO ''
  AS $function$
DECLARE
    v_meeting public.class_meetings%ROWTYPE;
BEGIN
    -- 1. Kiểm tra khóa học tồn tại và đang hoạt động
    IF NOT EXISTS (
        SELECT 1
        FROM public.courses c
        WHERE c.id = p_course_id
          AND c.is_active = true
    ) THEN
        RAISE EXCEPTION 'COURSE_NOT_ACTIVE'
            USING ERRCODE = 'P0001';
    END IF;

    -- 2. Kiểm tra quyền giảng viên
    IF NOT EXISTS (
        SELECT 1
        FROM public.course_members cm
        WHERE cm.course_id = p_course_id
          AND cm.user_id = p_user_id
          AND cm.member_role = 'LECTURER'
          AND cm.is_active = true
    ) THEN
        RAISE EXCEPTION 'NOT_COURSE_LECTURER'
            USING ERRCODE = 'P0001';
    END IF;

    -- 3. Validate dữ liệu Meeting
    IF p_meeting_number IS NULL
       OR p_meeting_number <= 0
       OR p_meeting_date IS NULL
       OR length(p_title) > 150
       OR length(p_room) > 100
       OR (
           p_start_time IS NOT NULL
           AND p_end_time IS NOT NULL
           AND p_start_time >= p_end_time
       )
    THEN
        RAISE EXCEPTION 'INVALID_MEETING_DATA'
            USING ERRCODE = 'P0001';
    END IF;

    -- 4. Tạo Meeting
    INSERT INTO public.class_meetings (
        course_id,
        meeting_number,
        meeting_date,
        title,
        start_time,
        end_time,
        room,
        status
    )
    VALUES (
        p_course_id,
        p_meeting_number,
        p_meeting_date,
        p_title,
        p_start_time,
        p_end_time,
        p_room,
        'SCHEDULED'
    )
    RETURNING * INTO v_meeting;

    -- 5. Khởi tạo Attendance Records
    INSERT INTO public.attendance_records (
        meeting_id,
        student_id,
        status
    )
    SELECT
        v_meeting.id,
        cm.user_id,
        'ABSENT'
    FROM public.course_members cm
    WHERE cm.course_id = p_course_id
      AND cm.member_role = 'STUDENT'
      AND cm.is_active = true;

    -- 6. Trả về Meeting
    RETURN v_meeting;
END;
$function$;

REVOKE ALL
  ON FUNCTION "public"."create_meeting_with_records"(uuid, uuid, integer, date, text, time WITHOUT time zone, time WITHOUT time zone, text)
  FROM PUBLIC, "anon", "authenticated";

CREATE OR REPLACE FUNCTION public.set_updated_at()
  RETURNS TRIGGER
  LANGUAGE plpgsql
  AS $function$
begin
    new.updated_at = now();
    return new;
end;
$function$;

ALTER TABLE "public"."attendance_records"
  ADD CONSTRAINT "attendance_records_session_id_fkey" FOREIGN KEY (session_id) REFERENCES public.attendance_sessions(id) ON DELETE SET NULL;

ALTER TABLE "public"."attendance_records"
  ADD CONSTRAINT "attendance_records_meeting_id_fkey" FOREIGN KEY (meeting_id) REFERENCES public.class_meetings(id) ON DELETE CASCADE;

ALTER TABLE "public"."attendance_sessions"
  ADD CONSTRAINT "attendance_sessions_meeting_id_fkey" FOREIGN KEY (meeting_id) REFERENCES public.class_meetings(id) ON DELETE CASCADE;

ALTER TABLE "public"."class_meetings"
  ADD CONSTRAINT "class_meetings_course_id_fkey" FOREIGN KEY (course_id) REFERENCES public.courses(id) ON DELETE CASCADE;

ALTER TABLE "public"."course_members"
  ADD CONSTRAINT "course_members_course_id_fkey" FOREIGN KEY (course_id) REFERENCES public.courses(id) ON DELETE CASCADE;

ALTER TABLE "public"."moodle_user_provisioning"
  ADD CONSTRAINT "moodle_user_provisioning_supabase_user_id_fkey" FOREIGN KEY (supabase_user_id) REFERENCES auth.users(id) ON DELETE SET NULL;

ALTER TABLE "public"."profiles"
  ADD CONSTRAINT "profiles_id_fkey" FOREIGN KEY (id) REFERENCES auth.users(id) ON DELETE CASCADE;

ALTER TABLE "public"."attendance_records"
  ADD CONSTRAINT "attendance_records_student_id_fkey" FOREIGN KEY (student_id) REFERENCES public.profiles(id);

ALTER TABLE "public"."attendance_sessions"
  ADD CONSTRAINT "attendance_sessions_created_by_fkey" FOREIGN KEY (created_by) REFERENCES public.profiles(id);

ALTER TABLE "public"."course_members"
  ADD CONSTRAINT "course_members_user_id_fkey" FOREIGN KEY (user_id) REFERENCES public.profiles(id) ON DELETE CASCADE;

CREATE UNIQUE INDEX attendance_sessions_one_open_per_meeting ON public.attendance_sessions USING btree (meeting_id)
  WHERE ((status)::text = 'OPEN'::text);

CREATE INDEX idx_attendance_records_meeting_id ON public.attendance_records USING btree (meeting_id);

CREATE INDEX idx_attendance_records_meeting_status ON public.attendance_records USING btree (meeting_id, status);

CREATE INDEX idx_attendance_records_session_id ON public.attendance_records USING btree (session_id);

CREATE INDEX idx_attendance_records_student_id ON public.attendance_records USING btree (student_id);

CREATE INDEX idx_attendance_sessions_created_by ON public.attendance_sessions USING btree (created_by);

CREATE INDEX idx_attendance_sessions_meeting_id ON public.attendance_sessions USING btree (meeting_id);

CREATE INDEX idx_attendance_sessions_status ON public.attendance_sessions USING btree (status);

CREATE INDEX idx_class_meetings_course_date ON public.class_meetings USING btree (course_id, meeting_date);

CREATE INDEX idx_class_meetings_course_id ON public.class_meetings USING btree (course_id);

CREATE INDEX idx_course_members_active_user ON public.course_members USING btree (user_id, is_active);

CREATE INDEX idx_course_members_course_id ON public.course_members USING btree (course_id);

CREATE INDEX idx_course_members_user_id ON public.course_members USING btree (user_id);

CREATE TRIGGER set_attendance_records_updated_at
  BEFORE UPDATE ON public.attendance_records
  FOR EACH ROW
  EXECUTE FUNCTION public.set_updated_at();

CREATE TRIGGER set_attendance_sessions_updated_at
  BEFORE UPDATE ON public.attendance_sessions
  FOR EACH ROW
  EXECUTE FUNCTION public.set_updated_at();

CREATE TRIGGER set_class_meetings_updated_at
  BEFORE UPDATE ON public.class_meetings
  FOR EACH ROW
  EXECUTE FUNCTION public.set_updated_at();

CREATE TRIGGER set_courses_updated_at
  BEFORE UPDATE ON public.courses
  FOR EACH ROW
  EXECUTE FUNCTION public.set_updated_at();

CREATE TRIGGER set_profiles_updated_at
  BEFORE UPDATE ON public.profiles
  FOR EACH ROW
  EXECUTE FUNCTION public.set_updated_at();

CREATE POLICY "attendance_records_select" ON "public"."attendance_records"
  FOR SELECT
  TO "authenticated"
  USING (((( SELECT auth.uid() AS uid) = student_id) OR private.lecturer_can_access_meeting(meeting_id)));

CREATE POLICY "attendance_sessions_select" ON "public"."attendance_sessions"
  FOR SELECT
  TO "authenticated"
  USING (private.can_access_meeting(meeting_id));

CREATE POLICY "class_meetings_select" ON "public"."class_meetings"
  FOR SELECT
  TO "authenticated"
  USING (private.is_course_member(course_id));

CREATE POLICY "course_members_select" ON "public"."course_members"
  FOR SELECT
  TO "authenticated"
  USING (((( SELECT auth.uid() AS uid) = user_id) OR private.is_course_lecturer(course_id)));

CREATE POLICY "courses_select_member" ON "public"."courses"
  FOR SELECT
  TO "authenticated"
  USING (private.is_course_member(id));

CREATE POLICY "profiles_select" ON "public"."profiles"
  FOR SELECT
  TO "authenticated"
  USING (((( SELECT auth.uid() AS uid) = id) OR private.lecturer_can_view_profile(id)));

REVOKE ALL ON FUNCTION "private"."can_access_meeting"(uuid) FROM PUBLIC;

GRANT EXECUTE ON FUNCTION "private"."can_access_meeting"(uuid) TO "authenticated";

REVOKE ALL ON FUNCTION "private"."is_course_lecturer"(uuid) FROM PUBLIC;

GRANT EXECUTE ON FUNCTION "private"."is_course_lecturer"(uuid) TO "authenticated";

REVOKE ALL ON FUNCTION "private"."is_course_member"(uuid) FROM PUBLIC;

GRANT EXECUTE ON FUNCTION "private"."is_course_member"(uuid) TO "authenticated";

REVOKE ALL ON FUNCTION "private"."lecturer_can_access_meeting"(uuid) FROM PUBLIC;

GRANT EXECUTE ON FUNCTION "private"."lecturer_can_access_meeting"(uuid) TO "authenticated";

REVOKE ALL ON FUNCTION "private"."lecturer_can_view_profile"(uuid) FROM PUBLIC;

GRANT EXECUTE ON FUNCTION "private"."lecturer_can_view_profile"(uuid) TO "authenticated";

REVOKE ALL ON FUNCTION "public"."create_attendance_session"(uuid, uuid, integer) FROM "postgres";

GRANT EXECUTE ON FUNCTION "public"."create_attendance_session"(uuid, uuid, integer) TO "postgres";

GRANT EXECUTE ON FUNCTION "public"."create_attendance_session"(uuid, uuid, integer) TO "service_role";

REVOKE ALL ON FUNCTION "public"."create_meeting_with_records"(uuid, uuid, integer, date, text, time WITHOUT time zone, time WITHOUT time zone, text) FROM "postgres";

GRANT EXECUTE ON FUNCTION "public"."create_meeting_with_records"(uuid, uuid, integer, date, text, time WITHOUT time zone, time WITHOUT time zone, text) TO "postgres";

GRANT EXECUTE ON FUNCTION "public"."create_meeting_with_records"(uuid, uuid, integer, date, text, time WITHOUT time zone, time WITHOUT time zone, text) TO "service_role";

GRANT EXECUTE ON FUNCTION "public"."set_updated_at"() TO PUBLIC, "anon", "authenticated";

REVOKE ALL ON FUNCTION "public"."set_updated_at"() FROM "postgres";

GRANT EXECUTE ON FUNCTION "public"."set_updated_at"() TO "postgres";

GRANT EXECUTE ON FUNCTION "public"."set_updated_at"() TO "service_role";

GRANT USAGE ON SCHEMA "private" TO "authenticated";

REVOKE ALL ON TABLE "public"."attendance_records" FROM "authenticated";

GRANT MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE ON TABLE "public"."attendance_records" TO "authenticated";

REVOKE ALL ON TABLE "public"."attendance_records" FROM "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."attendance_records" TO "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."attendance_records" TO "service_role";

REVOKE ALL ON TABLE "public"."attendance_sessions" FROM "authenticated";

REVOKE ALL ("closed_at") ON TABLE "public"."attendance_sessions" FROM "authenticated";

GRANT SELECT ("closed_at") ON TABLE "public"."attendance_sessions" TO "authenticated";

REVOKE ALL ("created_at") ON TABLE "public"."attendance_sessions" FROM "authenticated";

GRANT SELECT ("created_at") ON TABLE "public"."attendance_sessions" TO "authenticated";

REVOKE ALL ("created_by") ON TABLE "public"."attendance_sessions" FROM "authenticated";

GRANT SELECT ("created_by") ON TABLE "public"."attendance_sessions" TO "authenticated";

REVOKE ALL ("expires_at") ON TABLE "public"."attendance_sessions" FROM "authenticated";

GRANT SELECT ("expires_at") ON TABLE "public"."attendance_sessions" TO "authenticated";

REVOKE ALL ("id") ON TABLE "public"."attendance_sessions" FROM "authenticated";

GRANT SELECT ("id") ON TABLE "public"."attendance_sessions" TO "authenticated";

REVOKE ALL ("latitude") ON TABLE "public"."attendance_sessions" FROM "authenticated";

GRANT SELECT ("latitude") ON TABLE "public"."attendance_sessions" TO "authenticated";

REVOKE ALL ("longitude") ON TABLE "public"."attendance_sessions" FROM "authenticated";

GRANT SELECT ("longitude") ON TABLE "public"."attendance_sessions" TO "authenticated";

REVOKE ALL ("meeting_id") ON TABLE "public"."attendance_sessions" FROM "authenticated";

GRANT SELECT ("meeting_id") ON TABLE "public"."attendance_sessions" TO "authenticated";

REVOKE ALL ("opened_at") ON TABLE "public"."attendance_sessions" FROM "authenticated";

GRANT SELECT ("opened_at") ON TABLE "public"."attendance_sessions" TO "authenticated";

REVOKE ALL ("qr_refresh_seconds") ON TABLE "public"."attendance_sessions" FROM "authenticated";

GRANT SELECT ("qr_refresh_seconds") ON TABLE "public"."attendance_sessions" TO "authenticated";

REVOKE ALL ("radius_meters") ON TABLE "public"."attendance_sessions" FROM "authenticated";

GRANT SELECT ("radius_meters") ON TABLE "public"."attendance_sessions" TO "authenticated";

REVOKE ALL ("require_face") ON TABLE "public"."attendance_sessions" FROM "authenticated";

GRANT SELECT ("require_face") ON TABLE "public"."attendance_sessions" TO "authenticated";

REVOKE ALL ("require_gps") ON TABLE "public"."attendance_sessions" FROM "authenticated";

GRANT SELECT ("require_gps") ON TABLE "public"."attendance_sessions" TO "authenticated";

REVOKE ALL ("require_qr") ON TABLE "public"."attendance_sessions" FROM "authenticated";

GRANT SELECT ("require_qr") ON TABLE "public"."attendance_sessions" TO "authenticated";

REVOKE ALL ("status") ON TABLE "public"."attendance_sessions" FROM "authenticated";

GRANT SELECT ("status") ON TABLE "public"."attendance_sessions" TO "authenticated";

REVOKE ALL ("updated_at") ON TABLE "public"."attendance_sessions" FROM "authenticated";

GRANT SELECT ("updated_at") ON TABLE "public"."attendance_sessions" TO "authenticated";

GRANT MAINTAIN, REFERENCES, TRIGGER, TRUNCATE ON TABLE "public"."attendance_sessions" TO "authenticated";

REVOKE ALL ON TABLE "public"."attendance_sessions" FROM "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."attendance_sessions" TO "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."attendance_sessions" TO "service_role";

REVOKE ALL ON TABLE "public"."class_meetings" FROM "authenticated";

GRANT MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE ON TABLE "public"."class_meetings" TO "authenticated";

REVOKE ALL ON TABLE "public"."class_meetings" FROM "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."class_meetings" TO "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."class_meetings" TO "service_role";

REVOKE ALL ON TABLE "public"."course_members" FROM "authenticated";

GRANT MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE ON TABLE "public"."course_members" TO "authenticated";

REVOKE ALL ON TABLE "public"."course_members" FROM "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."course_members" TO "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."course_members" TO "service_role";

REVOKE ALL ON TABLE "public"."courses" FROM "authenticated";

GRANT MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE ON TABLE "public"."courses" TO "authenticated";

REVOKE ALL ON TABLE "public"."courses" FROM "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."courses" TO "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."courses" TO "service_role";

REVOKE ALL ON TABLE "public"."moodle_user_provisioning" FROM "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."moodle_user_provisioning" TO "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."moodle_user_provisioning" TO "service_role";

REVOKE ALL ON TABLE "public"."profiles" FROM "authenticated";

GRANT MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE ON TABLE "public"."profiles" TO "authenticated";

REVOKE ALL ON TABLE "public"."profiles" FROM "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."profiles" TO "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."profiles" TO "service_role";

