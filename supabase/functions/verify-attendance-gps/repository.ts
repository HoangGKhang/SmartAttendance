import { createAdminClient } from "../_shared/supabase.ts";

import type {
  VerifyGpsInput,
  VerifyGpsResult,
} from "./types.ts";

export class GpsRepositoryError extends Error {
  constructor(
    public readonly code?: string,
    public readonly databaseMessage?: string,
  ) {
    super("GPS verification database error");
    this.name = "GpsRepositoryError";
  }
}

export async function verifyGpsRepository(
  userId: string,
  input: VerifyGpsInput,
): Promise<VerifyGpsResult> {

  const admin = createAdminClient();

  // 1. Call PostgreSQL RPC.
  const { data, error } = await admin.rpc(
    "verify_attendance_gps",
    {
      p_user_id: userId,
      p_meeting_id: input.meetingId,
      p_session_id: input.sessionId,
      p_latitude: input.latitude,
      p_longitude: input.longitude,
      p_accuracy_meters: input.accuracyMeters,
    },
  );

  // 2. Handle database errors.
  if (error) {
    throw new GpsRepositoryError(
      error.code,
      error.message,
    );
  }

  // 3. Validate RPC response.
  if (
    data === null ||
    typeof data !== "object" ||
    Array.isArray(data) ||
    typeof data.verified !== "boolean"
  ) {
    throw new GpsRepositoryError(
      "INVALID_RPC_RESPONSE",
    );
  }

  // 4. Read attendance status.
  const attendanceStatus =
    data.attendanceStatus;

  // 5. GPS PASS must include attendance status.
  if (
    data.verified === true &&
    attendanceStatus !== "PRESENT" &&
    attendanceStatus !== "ABSENT"
  ) {
    throw new GpsRepositoryError(
      "INVALID_RPC_RESPONSE",
    );
  }

  // 6. Return public verification result.
  return {
    verified: data.verified,

    attendanceStatus:
      attendanceStatus === "PRESENT" ||
      attendanceStatus === "ABSENT"
        ? attendanceStatus
        : null,
  };
}