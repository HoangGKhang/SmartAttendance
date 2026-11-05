
import { createClient } from "npm:@supabase/supabase-js@2";

import { validateGetSessionRequest } from "./validation.ts";
import { findCurrentSession } from "./repository.ts";

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      "Cache-Control": "no-store",
    },
  });
}

Deno.serve(async (request: Request) => {
  // 1. Chỉ cho phép POST.
  if (request.method !== "POST") {
    return jsonResponse({ error: "METHOD_NOT_ALLOWED" }, 405);
  }

  // 2. Kiểm tra Authorization.
  const authorization = request.headers.get("Authorization");
  const token = authorization?.match(/^Bearer\s+(\S+)$/i)?.[1];

  if (!token) {
    return jsonResponse({ error: "UNAUTHORIZED" }, 401);
  }

  try {
    // 3. Chuẩn bị Supabase Auth Client.
    const url = Deno.env.get("SUPABASE_URL");
    const anonKey = Deno.env.get("SUPABASE_ANON_KEY");

    if (!url || !anonKey) {
      return jsonResponse({ error: "SERVER_CONFIGURATION_ERROR" }, 500);
    }

    const authClient = createClient(url, anonKey, {
      auth: {
        persistSession: false,
        autoRefreshToken: false,
      },
    });

    // 4. Xác minh JWT và lấy user ID thật.
    const { data: authData, error: authError } =
      await authClient.auth.getUser(token);

    if (authError || !authData.user) {
      return jsonResponse({ error: "UNAUTHORIZED" }, 401);
    }

    const userId = authData.user.id;

    // 5. Đọc và kiểm tra JSON request.
    let body: unknown;

    try {
      body = await request.json();
    } catch {
      return jsonResponse({ error: "INVALID_JSON" }, 400);
    }

    let meetingId: string;

    try {
      meetingId = validateGetSessionRequest(body);
    } catch {
      return jsonResponse({ error: "INVALID_REQUEST" }, 400);
    }

    // 6. Gọi PostgreSQL RPC.
    const { data, error } = await findCurrentSession(
      userId,
      meetingId,
    );

    if (error) {
      const message = error.message ?? "";

      if (message.includes("MEETING_NOT_FOUND")) {
        return jsonResponse({ error: "MEETING_NOT_FOUND" }, 404);
      }

      if (message.includes("NOT_COURSE_LECTURER")) {
        return jsonResponse({ error: "FORBIDDEN" }, 403);
      }

      if (message.includes("INVALID_SESSION_DATA")) {
        return jsonResponse({ error: "INVALID_REQUEST" }, 400);
      }

      console.error("Current session RPC error:", error.code);
      return jsonResponse({ error: "DATABASE_ERROR" }, 500);
    }

    // 7. Kiểm tra kết quả RPC.
    if (
      !data ||
      typeof data !== "object" ||
      !("session" in data)
    ) {
      return jsonResponse({ error: "INVALID_RPC_RESPONSE" }, 500);
    }

    if (data.session === null) {
      return jsonResponse({ session: null }, 200);
    }

    if (
      typeof data.session !== "object" ||
      Array.isArray(data.session)
    ) {
      return jsonResponse({ error: "INVALID_RPC_RESPONSE" }, 500);
    }

    // 8. Chỉ trả các trường được phép.
    const session = data.session as Record<string, unknown>;

    return jsonResponse({
      session: {
        id: session.id,
        meeting_id: session.meeting_id,
        created_by: session.created_by,
        status: session.status,
        opened_at: session.opened_at,
        expires_at: session.expires_at,
        closed_at: session.closed_at,
      },
    }, 200);

  } catch (error) {
    console.error(
      "Unexpected current session error:",
      error instanceof Error ? error.name : "Unknown",
    );

    return jsonResponse({ error: "INTERNAL_SERVER_ERROR" }, 500);
  }
});
