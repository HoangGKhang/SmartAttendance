
import type { CreateMeetingInput } from "./types.ts";

import {
  isActiveLecturer,
  insertMeeting,
} from "./repository.ts";

export class ForbiddenError extends Error {
  constructor(message = "Forbidden") {
    super(message);
    this.name = "ForbiddenError";
  }
}

/**
 * Nghiệp vụ tạo Meeting.
 *
 * userId phải đến từ JWT đã xác thực,
 * không lấy trực tiếp từ request body.
 */
export async function createMeetingService(
  userId: string,
  input: CreateMeetingInput,
) {
  // 1. Kiểm tra quyền giảng viên
  const canCreate = await isActiveLecturer(
    userId,
    input.courseId,
  );

  if (!canCreate) {
    throw new ForbiddenError();
  }

  // 2. Tạo Meeting
  const meeting = await insertMeeting(input);

  // 3. Trả kết quả cho Controller
  return meeting;
}
