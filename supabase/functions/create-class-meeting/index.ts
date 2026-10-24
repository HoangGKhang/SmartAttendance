
import {
  authenticateRequest,
} from "../_shared/auth.ts";

import {
  jsonResponse,
  errorResponse,
} from "../_shared/response.ts";

import {
  validateCreateMeeting,
  ValidationError,
} from "./validation.ts";

import {
  createMeetingService,
} from "./service.ts";

import {
  mapCreateMeetingError,
} from "./errors.ts";

Deno.serve(async (req: Request): Promise<Response> => {

  // Chỉ cho phép HTTP POST
  if (req.method !== "POST") {
    return errorResponse(
      "Method not allowed",
      405,
    );
  }

  try {
    // 1. Authentication
    const userId = await authenticateRequest(req);

    // 2. Parse JSON
    let body: unknown;

    try {
      body = await req.json();
    } catch {
      throw new ValidationError("Invalid JSON body");
    }

    // 3. Validation
    const input = validateCreateMeeting(body);

    // 4. Business logic
    const meeting = await createMeetingService(
      userId,
      input,
    );

    // 5. Success response
    return jsonResponse(
      { meeting },
      201,
    );

  } catch (error: unknown) {

    // Ghi log lỗi trên Backend
    console.error("Create meeting request failed", error);

    // Chuyển lỗi thành HTTP status phù hợp
    const apiError = mapCreateMeetingError(error);

    return errorResponse(
      apiError.message,
      apiError.status,
    );
  }
});
