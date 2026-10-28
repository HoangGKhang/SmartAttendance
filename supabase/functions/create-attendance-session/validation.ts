
import type { CreateSessionInput } from "./types.ts";

export class ValidationError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "ValidationError";
  }
}

const uuidPattern =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

export function validateCreateSession(
  body: unknown,
): CreateSessionInput {
  if (
    body === null ||
    typeof body !== "object" ||
    Array.isArray(body)
  ) {
    throw new ValidationError("Invalid JSON body");
  }

  const data = body as Record<string, unknown>;

  if (
    typeof data.meetingId !== "string" ||
    !uuidPattern.test(data.meetingId)
  ) {
    throw new ValidationError("Invalid meeting ID");
  }

  const duration = data.durationMinutes ?? 15;

  if (
    typeof duration !== "number" ||
    !Number.isSafeInteger(duration) ||
    duration < 1 ||
    duration > 180
  ) {
    throw new ValidationError("Invalid session duration");
  }

  return {
    meetingId: data.meetingId,
    durationMinutes: duration,
  };
}
