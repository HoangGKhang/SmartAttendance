
import { createAdminClient } from "../_shared/supabase.ts";
import type {
  CreateSessionInput,
  AttendanceSessionResponse,
} from "./types.ts";

const admin = createAdminClient();

export class SessionRepositoryError extends Error {
  constructor(
    public readonly code?: string,
    public readonly databaseMessage?: string,
  ) {
    super("Unable to create attendance session");
    this.name = "SessionRepositoryError";
  }
}

export async function insertAttendanceSession(
  userId: string,
  input: CreateSessionInput,
): Promise<AttendanceSessionResponse> {
  const { data, error } = await admin.rpc(
    "create_attendance_session",
    {
      p_user_id: userId,
      p_meeting_id: input.meetingId,
      p_duration_minutes: input.durationMinutes,
    },
  );

  if (error) {
    console.error("Create attendance session RPC failed", {
      code: error.code,
      message: error.message,
    });

    throw new SessionRepositoryError(
      error.code,
      error.message,
    );
  }

  if (!data || typeof data !== "object" ||
      Array.isArray(data)) {
    throw new SessionRepositoryError("EMPTY_RESULT");
  }

  // Chỉ lấy các trường được phép trả về.
  const session = data as Record<string, unknown>;

  return {
    id: session.id as string,
    meeting_id: session.meeting_id as string,
    created_by: session.created_by as string,
    status: session.status as "OPEN",
    opened_at: session.opened_at as string,
    expires_at: session.expires_at as string,
    closed_at: session.closed_at as string | null,
  };
}
