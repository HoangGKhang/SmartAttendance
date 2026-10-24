
import type { CreateMeetingInput } from "./types.ts";

import {
  isActiveLecturer,
  createMeetingWithRecords,
} from "./repository.ts";

export class ForbiddenError extends Error {
  constructor(message = "Forbidden") {
    super(message);
    this.name = "ForbiddenError";
  }
}

export async function createMeetingService(
  userId: string,
  input: CreateMeetingInput,
) {
  // Kiểm tra quyền sớm để trả HTTP 403.
  const canCreate = await isActiveLecturer(
    userId,
    input.courseId,
  );

  if (!canCreate) {
    throw new ForbiddenError();
  }

  // RPC kiểm tra lại quyền trong transaction.
  return await createMeetingWithRecords(
    userId,
    input,
  );
}
