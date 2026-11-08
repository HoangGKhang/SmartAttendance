-- Module 7.2-A
-- Thu hồi quyền quản trị không cần thiết
-- khỏi role authenticated.

REVOKE
    TRUNCATE,
    TRIGGER,
    REFERENCES,
    MAINTAIN
ON TABLE
    public.attendance_records,
    public.attendance_sessions,
    public.class_meetings,
    public.course_members,
    public.courses,
    public.profiles
FROM authenticated;
