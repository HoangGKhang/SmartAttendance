
import { createClient } from "npm:@supabase/supabase-js@2";

import { validateCloseSessionRequest } from "./validation.ts";
import { closeAttendanceSession } from "./repository.ts";

// Helper tạo JSON HTTP response.
function jsonResponse(
  body: unknown,
  status = 200,
): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      "Cache-Control": "no-store",
    },
  });
}

// Kiểm tra một giá trị có phải JSON object không.
function isRecord(
  value: unknown,
): value is Record<string, unknown> {
  return (
    typeof value === "object" &&
    value !== null &&
    !Array.isArray(value)
  );
}

Deno.serve(async (request: Request) => {
  // 1. Chỉ chấp nhận POST.
  if (request.method !== "POST") {
    return jsonResponse(
      { error: "METHOD_NOT_ALLOWED" },
      405,
    );
  }

  // 2. Đọc JWT từ Authorization header.
  const authorization =
    request.headers.get("Authorization");

  const token =
    authorization?.match(/^Bearer\s+(\S+)$/i)?.[1];

  if (!token) {
    return jsonResponse(
      { error: "UNAUTHORIZED" },
      401,
    );
  }

  try {
    // 3. Chuẩn bị Supabase Auth Client.
    const supabaseUrl =
      Deno.env.get("SUPABASE_URL");

    const anonKey =
      Deno.env.get("SUPABASE_ANON_KEY");

    if (!supabaseUrl || !anonKey) {
      return jsonResponse(
        { error: "SERVER_CONFIGURATION_ERROR" },
        500,
      );
    }

    const authClient = createClient(
      supabaseUrl,
      anonKey,
      {
        auth: {
          persistSession: false,
          autoRefreshToken: false,
        },
      },
    );

    // 4. Xác minh JWT.
    const {
      data: authData,
      error: authError,
    } = await authClient.auth.getUser(token);

    if (authError || !authData.user) {
      return jsonResponse(
        { error: "UNAUTHORIZED" },
        401,
      );
    }

    // User ID lấy từ Supabase Auth.
    const userId = authData.user.id;

    // 5. Đọc JSON body.
    let body: unknown;

    try {
      body = await request.json();
    } catch {
      return jsonResponse(
        { error: "INVALID_JSON" },
        400,
      );
    }

    // 6. Validate Session ID.
    let sessionId: string;

    try {
      sessionId = validateCloseSessionRequest(body);
    } catch {
      return jsonResponse(
        { error: "INVALID_REQUEST" },
        400,
      );
    }

    // 7. Gọi PostgreSQL RPC.
    const { data, error } =
      await closeAttendanceSession(
        userId,
        sessionId,
      );

    // 8. Ánh xạ lỗi PostgreSQL sang HTTP.
    if (error) {
      const message = error.message ?? "";

      if (message.includes("SESSION_NOT_FOUND")) {
        return jsonResponse(
          { error: "SESSION_NOT_FOUND" },
          404,
        );
      }

      if (message.includes("MEETING_NOT_FOUND")) {
        return jsonResponse(
          { error: "MEETING_NOT_FOUND" },
          404,
        );
      }

      if (message.includes("NOT_COURSE_LECTURER")) {
        return jsonResponse(
          { error: "FORBIDDEN" },
          403,
        );
      }

      if (
        message.includes("SESSION_ALREADY_CLOSED") ||
        message.includes("SESSION_NOT_OPEN")
      ) {
        return jsonResponse(
          { error: "SESSION_NOT_OPEN" },
          409,
        );
      }

      if (message.includes("INVALID_SESSION_DATA")) {
        return jsonResponse(
          { error: "INVALID_REQUEST" },
          400,
        );
      }

      console.error("Close session RPC error:", error.code);

      return jsonResponse(
        { error: "DATABASE_ERROR" },
        500,
      );
    }

    // 9. Kiểm tra dữ liệu từ RPC.
    if (!isRecord(data) || !isRecord(data.session)) {
      return jsonResponse(
        { error: "INVALID_RPC_RESPONSE" },
        500,
      );
    }

    const session = data.session;

    if (
      typeof session.id !== "string" ||
      typeof session.meeting_id !== "string" ||
      typeof session.created_by !== "string" ||
      session.status !== "CLOSED" ||
      typeof session.closed_at !== "string"
    ) {
      return jsonResponse(
        { error: "INVALID_RPC_RESPONSE" },
        500,
      );
    }

    // 10. Chỉ trả về dữ liệu an toàn.
    return jsonResponse(
      {
        session: {
          id: session.id,
          meeting_id: session.meeting_id,
          created_by: session.created_by,
          status: session.status,
          opened_at: session.opened_at,
          expires_at: session.expires_at,
          closed_at: session.closed_at,
        },
      },
      200,
    );

  } catch (error) {
    console.error(
      "Unexpected close session error:",
      error instanceof Error ? error.name : "Unknown",
    );

    return jsonResponse(
      { error: "INTERNAL_SERVER_ERROR" },
      500,
    );
  }
});
