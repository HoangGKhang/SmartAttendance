import type { VerifyGpsInput } from "./types.ts";

export class GpsValidationError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "GpsValidationError";
  }
}

const UUID_PATTERN =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

export function validateVerifyGps(
  body: unknown,
): VerifyGpsInput {
  if (
    body === null ||
    typeof body !== "object" ||
    Array.isArray(body)
  ) {
    throw new GpsValidationError("INVALID_REQUEST_BODY");
  }

  const data = body as Record<string, unknown>;

  if (
    typeof data.meetingId !== "string" ||
    !UUID_PATTERN.test(data.meetingId)
  ) {
    throw new GpsValidationError("INVALID_MEETING_ID");
  }

  if (
    typeof data.sessionId !== "string" ||
    !UUID_PATTERN.test(data.sessionId)
  ) {
    throw new GpsValidationError("INVALID_SESSION_ID");
  }

  if (
    typeof data.latitude !== "number" ||
    !Number.isFinite(data.latitude) ||
    data.latitude < -90 ||
    data.latitude > 90
  ) {
    throw new GpsValidationError("INVALID_LATITUDE");
  }

  if (
    typeof data.longitude !== "number" ||
    !Number.isFinite(data.longitude) ||
    data.longitude < -180 ||
    data.longitude > 180
  ) {
    throw new GpsValidationError("INVALID_LONGITUDE");
  }

  if (
    typeof data.accuracyMeters !== "number" ||
    !Number.isFinite(data.accuracyMeters) ||
    data.accuracyMeters < 0 ||
    data.accuracyMeters > 30
  ) {
    throw new GpsValidationError("INVALID_ACCURACY");
  }

  return {
    meetingId: data.meetingId,
    sessionId: data.sessionId,
    latitude: data.latitude,
    longitude: data.longitude,
    accuracyMeters: data.accuracyMeters,
  };
}