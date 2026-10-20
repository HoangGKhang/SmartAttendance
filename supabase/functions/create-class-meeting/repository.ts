
import { createAdminClient } from "../_shared/supabase.ts";
import type { CreateMeetingInput } from "./types.ts";

const admin = createAdminClient();

export class RepositoryError extends Error {
  constructor(
    public readonly operation: string,
    public readonly code?: string,
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
export async function insertMeeting(
  input: CreateMeetingInput,
) {
  const { data, error } = await admin
    .from("class_meetings")
    .insert({
      course_id: input.courseId,
      meeting_number: input.meetingNumber,
      meeting_date: input.meetingDate,
      title: input.title,
      start_time: input.startTime,
      end_time: input.endTime,
      room: input.room,
      status: "SCHEDULED",
    })
    .select()
    .single();

  if (error) {
    console.error("Meeting insert failed", error);

    throw new RepositoryError(
      "insert meeting",
      error.code,
    );
  }

  return data;
}
