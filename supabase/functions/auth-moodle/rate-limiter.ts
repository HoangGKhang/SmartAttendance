
import { createClient } from "npm:@supabase/supabase-js@2";

export type RateLimitDecision = {
  allowed: boolean;
  retryAfterSeconds: number;
};

/**
 * Creates a privileged client for the rate-limit RPC.
 * Never expose this client or its key to Android.
 */
function createAdminClient() {
  const url = Deno.env.get("SUPABASE_URL");
  const serviceRoleKey = Deno.env.get(
    "SUPABASE_SERVICE_ROLE_KEY",
  );

  if (!url || !serviceRoleKey) {
    throw new Error("RATE_LIMIT_CONFIGURATION_ERROR");
  }

  return createClient(url, serviceRoleKey, {
    auth: {
      autoRefreshToken: false,
      persistSession: false,
      detectSessionInUrl: false,
    },
  });
}

/**
 * Create a pseudonymous database bucket for a username.
 * HMAC prevents storing raw Moodle usernames.
 */
async function createUsernameBucket(
  username: string,
): Promise<string> {
  const secret = Deno.env.get(
    "AUTH_MOODLE_RATE_LIMIT_HMAC_KEY",
  ) ?? "";

  const encoder = new TextEncoder();

  if (encoder.encode(secret).length < 32) {
    throw new Error("RATE_LIMIT_CONFIGURATION_ERROR");
  }

  const key = await crypto.subtle.importKey(
    "raw",
    encoder.encode(secret),
    {
      name: "HMAC",
      hash: "SHA-256",
    },
    false,
    ["sign"],
  );

  const normalizedUsername =
    username.trim().toLowerCase();

  const message =
    `auth-moodle:username:v1:${normalizedUsername}`;

  const signature = await crypto.subtle.sign(
    "HMAC",
    key,
    encoder.encode(message),
  );

  return Array.from(new Uint8Array(signature))
    .map((byte) => byte.toString(16).padStart(2, "0"))
    .join("");
}

/**
 * Consume one login attempt.
 * Both global and username limits are handled
 * by the PostgreSQL RPC.
 */

export async function consumeMoodleLoginRateLimit(
  username: string,
): Promise<RateLimitDecision> {

  console.info("[rate-limit] STEP_1_START");

  // STEP 2: Create HMAC bucket.
  const usernameBucket =
    await createUsernameBucket(username);

  console.info("[rate-limit] STEP_2_HMAC_OK");

  // STEP 3: Create Supabase admin client.
  const supabase = createAdminClient();

  console.info("[rate-limit] STEP_3_CLIENT_OK");

  // STEP 4: Call PostgreSQL RPC.
  const { data, error } = await supabase.rpc(
    "consume_auth_moodle_rate_limit",
    {
      p_username_bucket: usernameBucket,
    },
  );

  console.info("[rate-limit] STEP_4_RPC_RETURNED");

  if (error) {
    console.error("[rate-limit] RPC_ERROR", {
      code: error.code ?? "UNKNOWN",
    });

    throw new Error("RATE_LIMIT_RPC_ERROR");
  }

  if (!Array.isArray(data) || data.length !== 1) {
    console.error("[rate-limit] INVALID_RPC_DATA", {
      isArray: Array.isArray(data),
      rowCount: Array.isArray(data)
        ? data.length
        : null,
    });

    throw new Error("RATE_LIMIT_INVALID_RESPONSE");
  }

  const decision = data[0] as {
    allowed?: unknown;
    retry_after_seconds?: unknown;
  };

  if (
    typeof decision.allowed !== "boolean" ||
    typeof decision.retry_after_seconds !== "number" ||
    !Number.isSafeInteger(decision.retry_after_seconds) ||
    decision.retry_after_seconds < 0
  ) {
    console.error("[rate-limit] INVALID_DECISION");

    throw new Error("RATE_LIMIT_INVALID_RESPONSE");
  }

  console.info("[rate-limit] STEP_5_SUCCESS");

  return {
    allowed: decision.allowed,
    retryAfterSeconds: decision.retry_after_seconds,
  };
}

