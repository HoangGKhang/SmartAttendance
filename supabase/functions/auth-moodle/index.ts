import { authenticateWithMoodle, MoodleAuthError } from "./moodle-client.ts";
import { findLinkedIdentity, IdentityError } from "./identity-repository.ts";
import { createSupabaseSession, SessionError } from "./session-service.ts";
import { consumeMoodleLoginRateLimit } from "./rate-limiter.ts";

const MAX_BODY_BYTES = 4096;

function jsonResponse(
  status: number,
  data: Record<string, unknown>,
  extraHeaders: Record<string, string> = {},
): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      "Cache-Control": "no-store",
      "X-Content-Type-Options": "nosniff",
      ...extraHeaders,
    },
  });
}

function fail(status: number, code: string): Response {
  return jsonResponse(status, {
    success: false,
    error: code,
  });
}

Deno.serve(async (request: Request): Promise<Response> => {
  // 1. Accept POST only.
  if (request.method !== "POST") {
    return fail(405, "METHOD_NOT_ALLOWED");
  }

  // Keep Moodle login disabled until the backend is configured.
 const moodleLoginEnabled = Deno.env.get("AUTH_MOODLE_LOGIN_ENABLED");

 console.info("[moodle-login] flag-check", {
   configured: moodleLoginEnabled !== undefined,
   enabled: moodleLoginEnabled === "true",
   hasOuterWhitespace:
     moodleLoginEnabled !== undefined &&
     moodleLoginEnabled !== moodleLoginEnabled.trim(),
 });

 if (moodleLoginEnabled !== "true") {
   return fail(503, "MOODLE_LOGIN_DISABLED");
 }

  // 4. Validate request content.
  const contentType = request.headers
    .get("content-type")
    ?.split(";")[0]
    ?.trim()
    .toLowerCase();

  if (contentType !== "application/json") {
    return fail(415, "UNSUPPORTED_MEDIA_TYPE");
  }

  const declaredLength = Number(
    request.headers.get("content-length") ?? "0",
  );

  if (declaredLength > MAX_BODY_BYTES) {
    return fail(413, "REQUEST_TOO_LARGE");
  }

  let payload: unknown;

  try {
    const rawBody = await request.text();

    if (
      new TextEncoder().encode(rawBody).length >
      MAX_BODY_BYTES
    ) {
      return fail(413, "REQUEST_TOO_LARGE");
    }

    payload = JSON.parse(rawBody);
  } catch {
    return fail(400, "INVALID_JSON");
  }

  if (
    typeof payload !== "object" ||
    payload === null ||
    Array.isArray(payload)
  ) {
    return fail(400, "INVALID_REQUEST");
  }

  const body = payload as Record<string, unknown>;

  if (
    typeof body.username !== "string" ||
    typeof body.password !== "string" ||
    !body.username.trim() ||
    !body.password ||
    body.username.length > 150 ||
    body.password.length > 256
  ) {
    return fail(400, "INVALID_REQUEST");
  }

  const username = body.username.trim();
  const password = body.password;

  // 5. Check rate limit before contacting Moodle.
// /*   try {
//     const decision = await consumeMoodleLoginRateLimit(
//       username,
//     );
//
//     if (!decision.allowed) {
//       return jsonResponse(
//         429,
//         {
//           success: false,
//           error: "RATE_LIMITED",
//         },
//         {
//           "Retry-After": String(
//             Math.max(1, decision.retryAfterSeconds),
//           ),
//         },
//       );
//     }
//   } catch {
//     // Fail closed: never skip the rate limiter.
//     return fail(503, "RATE_LIMIT_UNAVAILABLE");
//   } */


  try {
    // 5. Let Moodle verify the password.
    const moodleIdentity = await authenticateWithMoodle(
      username,
      password,
    );

    // 6. Resolve the existing Supabase identity.
    const identity = await findLinkedIdentity(
      moodleIdentity.moodleUserId,
    );

    // 7. Create a native Supabase Auth session.
    const session = await createSupabaseSession(identity);

    // 8. Return only the fields Android needs.
    return jsonResponse(200, {
      success: true,
      user: {
        moodle_user_id: identity.moodleUserId,
        supabase_user_id: identity.supabaseUserId,
        role: identity.role,
      },
      session: {
        access_token: session.accessToken,
        refresh_token: session.refreshToken,
        expires_in: session.expiresIn,
        expires_at: session.expiresAt,
      },
    });
  } catch (error) {
    if (error instanceof MoodleAuthError) {
      if (error.code === "INVALID_CREDENTIALS") {
        return fail(401, "AUTHENTICATION_FAILED");
      }

      return fail(502, "MOODLE_AUTH_UNAVAILABLE");
    }

    if (error instanceof IdentityError) {
      if (error.code === "BACKEND_ERROR") {
        return fail(503, "IDENTITY_SERVICE_UNAVAILABLE");
      }

      return fail(401, "AUTHENTICATION_FAILED");
    }

    if (error instanceof SessionError) {
      if (error.code === "DEMO_CONFIRMATION_REQUIRED") {
        return fail(409, "DEMO_CONFIRMATION_REQUIRED");
      }

      return fail(503, "SESSION_CREATION_FAILED");
    }

    // Do not expose internal exceptions or credentials.
    return fail(500, "INTERNAL_ERROR");
  }
});
