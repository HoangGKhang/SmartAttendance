SELECT
    has_function_privilege(
        'anon',
        'public.close_attendance_session(uuid,uuid)',
        'EXECUTE'
    ) AS anon_execute,

    has_function_privilege(
        'authenticated',
        'public.close_attendance_session(uuid,uuid)',
        'EXECUTE'
    ) AS authenticated_execute,

    has_function_privilege(
        'service_role',
        'public.close_attendance_session(uuid,uuid)',
        'EXECUTE'
    ) AS service_execute;