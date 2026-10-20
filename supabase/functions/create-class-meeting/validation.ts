
import type { CreateMeetingInput } from "./types.ts";

export class ValidationError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "ValidationError";
  }
}

// UUID validation
const uuidPattern =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

// Date validation: YYYY-MM-DD
function validDate(value: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    return false;
  }

  const date = new Date(`${value}T00:00:00Z`);

  return !Number.isNaN(date.getTime()) &&
    date.toISOString().slice(0, 10) === value;
}

// Optional string validation
function validOptionalString(
  value: unknown,
  maxLength: number,
): boolean {
  return value == null ||
    (typeof value === "string" &&
      value.length <= maxLength);
}

// Normalize time: HH:mm -> HH:mm:ss
function normalizeTime(value: unknown): string | null {
  if (value == null) {
    return null;
  }

  if (typeof value !== "string") {
    throw new ValidationError("Invalid time");
  }

  if (!/^([01]\d|2[0-3]):[0-5]\d$/.test(value)) {
    throw new ValidationError("Invalid time");
  }

  return `${value}:00`;
}

/**
 * Kiểm tra và chuẩn hóa dữ liệu tạo Meeting.
 */
export function validateCreateMeeting(
  body: unknown,
): CreateMeetingInput {

  if (
    !body ||
    typeof body !== "object" ||
    Array.isArray(body)
  ) {
    throw new ValidationError("Invalid JSON body");
  }

  const data = body as Record<string, unknown>;

  const {
    courseId,
    meetingNumber,
    meetingDate,
    title,
    startTime,
    endTime,
    room,
  } = data;

  // Required fields
  if (
    typeof courseId !== "string" ||
    !uuidPattern.test(courseId) ||
    !Number.isSafeInteger(meetingNumber) ||
    (meetingNumber as number) <= 0 ||
    typeof meetingDate !== "string" ||
    !validDate(meetingDate)
  ) {
    throw new ValidationError("Invalid meeting data");
  }

  // Optional fields
  if (
    !validOptionalString(title, 150) ||
    !validOptionalString(room, 100)
  ) {
    throw new ValidationError("Invalid title or room");
  }

  const start = normalizeTime(startTime);
  const end = normalizeTime(endTime);

  if (start && end && start >= end) {
    throw new ValidationError("End time must be later");
  }

  return {
    courseId,
    meetingNumber: meetingNumber as number,
    meetingDate,
    title: (title ?? null) as string | null,
    startTime: start,
    endTime: end,
    room: (room ?? null) as string | null,
  };
}
