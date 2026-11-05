
export function validateCloseSessionRequest(
  body: unknown,
): string {
  // 1. Request body phải là JSON object.
  if (
    typeof body !== "object" ||
    body === null ||
    Array.isArray(body)
  ) {
    throw new Error("INVALID_REQUEST_BODY");
  }

  const data = body as Record<string, unknown>;
  const sessionId = data.sessionId;

  // 2. Kiểm tra định dạng UUID.
  const uuidRegex =
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

  if (
    typeof sessionId !== "string" ||
    !uuidRegex.test(sessionId)
  ) {
    throw new Error("INVALID_SESSION_ID");
  }

  // 3. Trả ID đã kiểm tra.
  return sessionId;
}
