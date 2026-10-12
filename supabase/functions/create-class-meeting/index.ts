import { createClient } from "npm:@supabase/supabase-js@2";

const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

const admin = createClient(supabaseUrl, serviceRoleKey, {
  auth: {
    persistSession: false,
    autoRefreshToken: false,
  },
});

const reply = (data: unknown, status = 200) =>
  Response.json(data, { status });

const uuidPattern =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

function validDate(value: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;

  const date = new Date(`${value}T00:00:00Z`);

  return !Number.isNaN(date.getTime()) &&
    date.toISOString().slice(0, 10) === value;
}

Deno.serve(async (req) => {
  if (req.method !== "POST") {
    return reply({ error: "Method not allowed" }, 405);
  }

  try {
    // 1. Lấy JWT của người gọi.
    const authHeader = req.headers.get("Authorization");

    if (!authHeader?.startsWith("Bearer ")) {
      return reply({ error: "Unauthorized" }, 401);
    }

    const token = authHeader.slice(7);

    // 2. Xác thực JWT với Supabase Auth.
    const { data: authData, error: authError } =
      await admin.auth.getUser(token);

    if (authError || !authData.user) {
      return reply({ error: "Invalid session" }, 401);
    }

    const userId = authData.user.id;

    // 3. Đọc dữ liệu đầu vào.
    let body: Record<string, unknown>;

    try {
      body = await req.json();

      if (!body || typeof body !== "object" ||
          Array.isArray(body)) {
        return reply({ error: "Invalid JSON body" }, 400);
      }
    } catch {
      return reply({ error: "Invalid JSON body" }, 400);
    }

    const {
      courseId,
      meetingNumber,
      meetingDate,
      title,
      startTime,
      endTime,
      room,
    } = body;

    // 4. Validate dữ liệu.
    if (
      typeof courseId !== "string" ||
      !uuidPattern.test(courseId) ||
      !Number.isSafeInteger(meetingNumber) ||
      (meetingNumber as number) <= 0 ||
      typeof meetingDate !== "string" ||
      !validDate(meetingDate)
    ) {
      return reply({ error: "Invalid meeting data" }, 400);
    }

    const validOptionalString = (
      value: unknown,
      maxLength: number
    ) =>
      value == null ||
      (typeof value === "string" &&
        value.length <= maxLength);

    if (
      !validOptionalString(title, 150) ||
      !validOptionalString(room, 100)
    ) {
      return reply({ error: "Invalid title or room" }, 400);
    }

    const normalizeTime = (value: unknown): string | null => {
      if (value == null) return null;
      if (typeof value !== "string") {
        throw new Error("Invalid time");
      }

      if (!/^([01]\d|2[0-3]):[0-5]\d$/.test(value)) {
        throw new Error("Invalid time");
      }

      return `${value}:00`;
    };

    let start: string | null;
    let end: string | null;

    try {
      start = normalizeTime(startTime);
      end = normalizeTime(endTime);
    } catch {
      return reply({ error: "Invalid time" }, 400);
    }

    if (start && end && start >= end) {
      return reply({ error: "End time must be later" }, 400);
    }

    // 5. Kiểm tra quyền giảng viên trong Course.
    const { data: membership, error: memberError } =
      await admin
        .from("course_members")
        .select("id")
        .eq("course_id", courseId)
        .eq("user_id", userId)
        .eq("member_role", "LECTURER")
        .eq("is_active", true)
        .maybeSingle();

    if (memberError) {
      console.error("Membership lookup failed", memberError);
      return reply({ error: "Internal server error" }, 500);
    }

    if (!membership) {
      return reply({ error: "Forbidden" }, 403);
    }

    // 6. Thêm Meeting.
    const { data: meeting, error: insertError } =
      await admin
        .from("class_meetings")
        .insert({
          course_id: courseId,
          meeting_number: meetingNumber,
          meeting_date: meetingDate,
          title: title ?? null,
          start_time: start,
          end_time: end,
          room: room ?? null,
          status: "SCHEDULED",
        })
        .select()
        .single();

    if (insertError) {
      if (insertError.code === "23505") {
        return reply({
          error: "Meeting number already exists",
        }, 409);
      }

      console.error("Meeting insert failed", insertError);

      return reply({ error: "Unable to create meeting" }, 500);
    }

    return reply({ meeting }, 201);

  } catch (error) {
    console.error("Unexpected error", error);

    return reply({ error: "Internal server error" }, 500);
  }
});