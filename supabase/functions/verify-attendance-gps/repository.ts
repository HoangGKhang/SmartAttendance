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

  if (error) {
    throw new GpsRepositoryError(
      error.code,
      error.message,
    );
  }

  if (
    data === null ||
    typeof data !== "object" ||
    Array.isArray(data) ||
    typeof data.verified !== "boolean"
  ) {
    throw new GpsRepositoryError("INVALID_RPC_RESPONSE");
  }

  return {
    verified: data.verified,
  };
}