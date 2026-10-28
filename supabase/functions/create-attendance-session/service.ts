
import type { CreateSessionInput } from "./types.ts";
import { insertAttendanceSession } from "./repository.ts";

/**
 * userId phải lấy từ JWT đã được xác thực.
 * input phải đi qua validateCreateSession().
 */
export async function createAttendanceSessionService(
  userId: string,
  input: CreateSessionInput,
) {
  return await insertAttendanceSession(userId, input);
}
