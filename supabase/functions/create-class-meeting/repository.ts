
import { createAdminClient } from "../_shared/supabase.ts";
import type { CreateMeetingInput } from "./types.ts";

const admin = createAdminClient();

export class RepositoryError extends Error {
  constructor(
    public readonly operation: string,
    public readonly code?: string,
    public readonly databaseMessage?: string,
  ) {
    super(`Database operation failed: ${operation}`);
    this.name = "RepositoryError";
  }
}


/**
 * Kiểm tra người dùng có membership giảng viên
 * đang hoạt động trong khóa học hay không.
 */
export async function isActiveLecturer(
  userId: string,
  courseId: string,
): Promise<boolean> {
  const { data, error } = await admin
    .from("course_members")
    .select("id")
    .eq("course_id", courseId)
    .eq("user_id", userId)
    .eq("member_role", "LECTURER")
    .eq("is_active", true)
    .maybeSingle();

  if (error) {
    console.error("Membership lookup failed", error);
    throw new RepositoryError(
      "lookup lecturer membership",
      error.code,
    );
  }

  return data !== null;
}

/**
 * Tạo bản ghi Meeting trên Supabase.
 *
 * Chưa sinh Attendance Records trong bước này.
 */

/**
 * Tạo Meeting cùng Attendance Records
 * bằng PostgreSQL RPC Transaction.
 *
 * userId lấy từ JWT đã xác thực.
 */
export async function createMeetingWithRecords(
  userId: string,
  input: CreateMeetingInput,
) {
  const { data, error } = await admin.rpc(
    "create_meeting_with_records",
    {
      p_user_id: userId,
      p_course_id: input.courseId,
      p_meeting_number: input.meetingNumber,
      p_meeting_date: input.meetingDate,
      p_title: input.title,
      p_start_time: input.startTime,
      p_end_time: input.endTime,
      p_room: input.room,
    },
  );

  if (error) {
    console.error("Create meeting RPC failed", error);

    throw new RepositoryError(
      "create meeting with records",
      error.code,
      error.message,
    );
  }

  if (!data) {
    throw new RepositoryError(
      "create meeting with records",
      "EMPTY_RESULT",
    );
  }

  return data;
}

