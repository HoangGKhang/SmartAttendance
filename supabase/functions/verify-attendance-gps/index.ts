
import {
  authenticateRequest,
} from "../_shared/auth.ts";

import {
  validateVerifyGps,
  GpsValidationError,
} from "./validation.ts";

import {
  verifyGpsService,
} from "./service.ts";

import {
  mapVerifyGpsError,
} from "./errors.ts";

function jsonResponse(
  data: unknown,
  status = 200,
): Response {
  return Response.json(data, {
    status,
    headers: {
      "Cache-Control": "no-store",
    },
  });
}

Deno.serve(async (request: Request): Promise<Response> => {
  // 1. Only POST requests are allowed.
  if (request.method !== "POST") {
    return jsonResponse(
      { error: "METHOD_NOT_ALLOWED" },
      405,
    );
  }

  try {
    // 2. Authenticate using the Student JWT.
    const userId = await authenticateRequest(request);

    // 3. Parse request body.
    let body: unknown;

    try {
      body = await request.json();
    } catch {
      throw new GpsValidationError(
        "INVALID_REQUEST_BODY",
      );
    }

    // 4. Validate GPS input.
    const input = validateVerifyGps(body);

    // 5. Execute business logic and PostgreSQL RPC.
    const result = await verifyGpsService(
      userId,
      input,
    );

    // 6. Return only the verification result.
    return jsonResponse(
      {
        verified: result.verified,
      },
      200,
    );

  } catch (error: unknown) {
    // Never expose sensitive data or database errors.
    console.error(
      "GPS verification failed",
      error instanceof Error ? error.name : "UnknownError",
    );

    const apiError = mapVerifyGpsError(error);

    return jsonResponse(
      { error: apiError.message },
      apiError.status,
    );
  }
});
