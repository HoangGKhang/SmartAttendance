
export function jsonResponse(
  data: unknown,
  status = 200,
): Response {
  return Response.json(data, { status });
}

export function errorResponse(
  message: string,
  status: number,
): Response {
  return jsonResponse({ error: message }, status);
}
