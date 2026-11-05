
export function validateGetSessionRequest(
  body: unknown,
): string {
  if (
    typeof body !== "object" ||
    body === null ||
    Array.isArray(body)
  ) {
    throw new Error("INVALID_REQUEST_BODY");
  }

  const data = body as Record<string, unknown>;

  const meetingId = data.meetingId;

  const uuidRegex =
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

  if (
    typeof meetingId !== "string" ||
    !uuidRegex.test(meetingId)
  ) {
    throw new Error("INVALID_MEETING_ID");
  }

  return meetingId;
}
