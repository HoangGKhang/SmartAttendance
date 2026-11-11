
import type { CreateSessionInput } from "./types.ts";

export class ValidationError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "ValidationError";
  }
}

const uuidPattern =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

/**
 * Kiểm tra giá trị là số hữu hạn
 * và nằm trong khoảng cho phép.
 */
function isFiniteNumberInRange(
  value: unknown,
  min: number,
  max: number,
): value is number {
  return (
    typeof value === "number" &&
    Number.isFinite(value) &&
    value >= min &&
    value <= max
  );
}

export function validateCreateSession(
  body: unknown,
): CreateSessionInput {

  // 1. JSON phải là object.
  if (
    body === null ||
    typeof body !== "object" ||
    Array.isArray(body)
  ) {
    throw new ValidationError("Invalid JSON body");
  }

  const data = body as Record<string, unknown>;

  // 2. Kiểm tra Meeting ID.
  if (
    typeof data.meetingId !== "string" ||
    !uuidPattern.test(data.meetingId)
  ) {
    throw new ValidationError("Invalid meeting ID");
  }

  // 3. Kiểm tra thời lượng Session.
  const duration = data.durationMinutes ?? 15;

  if (
    typeof duration !== "number" ||
    !Number.isSafeInteger(duration) ||
    duration < 1 ||
    duration > 180
  ) {
    throw new ValidationError("Invalid session duration");
  }

  // 4. Kiểm tra latitude.
  const latitude = data.latitude;

  if (!isFiniteNumberInRange(latitude, -90, 90)) {
    throw new ValidationError("Invalid GPS latitude");
  }

  // 5. Kiểm tra longitude.
  const longitude = data.longitude;

  if (!isFiniteNumberInRange(longitude, -180, 180)) {
    throw new ValidationError("Invalid GPS longitude");
  }

  // 6. Kiểm tra bán kính.
  const radiusMeters = data.radiusMeters;

  if (
    typeof radiusMeters !== "number" ||
    !Number.isSafeInteger(radiusMeters) ||
    radiusMeters < 10 ||
    radiusMeters > 200
  ) {
    throw new ValidationError("Invalid GPS radius");
  }

  // 7. Kiểm tra độ chính xác do Android báo cáo.
  const accuracyMeters = data.accuracyMeters;

  if (!isFiniteNumberInRange(accuracyMeters, 0, 30)) {
    throw new ValidationError(
      "GPS accuracy is insufficient",
    );
  }

  // 8. Trả về dữ liệu đã kiểm tra.
  return {
    meetingId: data.meetingId,
    durationMinutes: duration,
    latitude,
    longitude,
    radiusMeters,
    accuracyMeters,
  };
}
