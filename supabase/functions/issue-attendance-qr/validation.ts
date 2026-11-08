
import type { IssueQrInput } from "./types.ts";

/**
 * Lỗi dữ liệu đầu vào không hợp lệ.
 */
export class ValidationError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "ValidationError";
  }
}

/**
 * Định dạng UUID.
 * Hỗ trợ UUID version 1–8 theo pattern hiện có.
 */
const uuidPattern =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

/**
 * Kiểm tra JSON body và trả dữ liệu đã validation.
 */
export function validateIssueQrRequest(
  body: unknown,
): IssueQrInput {

  // 1. Body phải là JSON object.
  if (
    body === null ||
    typeof body !== "object" ||
    Array.isArray(body)
  ) {
    throw new ValidationError(
      "INVALID_REQUEST_BODY",
    );
  }

  // 2. Truy cập các thuộc tính của object.
  const data = body as Record<string, unknown>;

  // 3. sessionId phải là chuỗi UUID hợp lệ.
  if (
    typeof data.sessionId !== "string" ||
    !uuidPattern.test(data.sessionId)
  ) {
    throw new ValidationError(
      "INVALID_SESSION_ID",
    );
  }

  // 4. Trả dữ liệu đã được kiểm tra.
  return {
    sessionId: data.sessionId.toLowerCase(),
  };
}
