
import {
  authenticateRequest,
} from "../_shared/auth.ts";

import {
  jsonResponse,
  errorResponse,
} from "../_shared/response.ts";

import {
  validateCreateSession,
  ValidationError,
} from "./validation.ts";

import {
  createAttendanceSessionService,
} from "./service.ts";

import {
  mapCreateSessionError,
} from "./errors.ts";

Deno.serve(async (req: Request): Promise<Response> => {
  // 1. Chỉ cho phép POST.
  if (req.method !== "POST") {
    return errorResponse("Method not allowed", 405);
  }

  try {
    // 2. Xác thực JWT với Supabase Auth.
    const userId = await authenticateRequest(req);

    // 3. Đọc JSON.
    let body: unknown;

    try {
      body = await req.json();
    } catch {
      throw new ValidationError("Invalid JSON body");
    }

    // 4. Validation.
    const input = validateCreateSession(body);

    // 5. Business logic và PostgreSQL RPC.
    const session = await createAttendanceSessionService(
      userId,
      input,
    );

    // 6. Trả session, không chứa qr_secret.
    return jsonResponse({ session }, 201);
  } catch (error: unknown) {
    console.error("Create attendance session failed", error);

    const apiError = mapCreateSessionError(error);

    return errorResponse(
      apiError.message,
      apiError.status,
    );
  }
});
