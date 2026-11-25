
import { createClient } from "npm:@supabase/supabase-js@2";
import type { LinkedIdentity } from "./identity-repository.ts";

export type SupabaseLoginSession = {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  expiresAt: number | null;
  userId: string;
};

export class SessionError extends Error {
  constructor(
    public readonly code:
      | "CONFIGURATION_ERROR"
      | "AUTH_USER_NOT_FOUND"
      | "SESSION_CREATION_FAILED"
      | "SESSION_IDENTITY_MISMATCH"
      | "DEMO_CONFIRMATION_REQUIRED",
  ) {
    super(code);
    this.name = "SessionError";
  }
}

function createAuthClients() {
  const url = Deno.env.get("SUPABASE_URL");
  const serviceRoleKey = Deno.env.get(
    "SUPABASE_SERVICE_ROLE_KEY",
  );
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");

  if (!url || !serviceRoleKey || !anonKey) {
    throw new SessionError("CONFIGURATION_ERROR");
  }

  const authOptions = {
    autoRefreshToken: false,
    persistSession: false,
    detectSessionInUrl: false,
  };

  return {
    admin: createClient(url, serviceRoleKey, {
      auth: authOptions,
    }),
    publicClient: createClient(url, anonKey, {
      auth: authOptions,
    }),
  };
}

/**
 * Called ONLY after:
 * 1. Moodle username/password are verified.
 * 2. Moodle user ID is resolved to an existing
 *    Supabase Auth identity.
 */
export async function createSupabaseSession(
  identity: LinkedIdentity,
): Promise<SupabaseLoginSession> {
  const { admin, publicClient } = createAuthClients();

  // 1. Read the existing Auth User.
  const { data: authData, error: authError } =
    await admin.auth.admin.getUserById(
      identity.supabaseUserId,
    );

  const user = authData?.user;

  if (
    authError ||
    !user ||
    user.id !== identity.supabaseUserId ||
    !user.email
  ) {
    throw new SessionError("AUTH_USER_NOT_FOUND");
  }

  // Avoid silently changing unconfirmed email state.
  // Enable only when testing with a controlled demo account.
  if (
    !user.email_confirmed_at &&
    Deno.env.get("AUTH_MOODLE_DEMO_CONFIRM_UNVERIFIED") !== "true"
  ) {
    throw new SessionError("DEMO_CONFIRMATION_REQUIRED");
  }

  // 2. Generate an internal one-time token.
  // Recovery does not create an unknown Auth User.
  const { data: linkData, error: linkError } =
    await admin.auth.admin.generateLink({
      type: "magiclink",
      email: user.email,
    });

  if (
    linkError ||
    !linkData?.user ||
    linkData.user.id !== identity.supabaseUserId ||
    !linkData.properties?.hashed_token
  ) {
    throw new SessionError("SESSION_CREATION_FAILED");
  }

  // 3. Exchange the one-time token for a Supabase Session.
  const { data: verified, error: verifyError } =
    await publicClient.auth.verifyOtp({
      token_hash: linkData.properties.hashed_token,
      type: "magiclink",
    });

  if (
    verifyError ||
    !verified.user ||
    !verified.session
  ) {
    throw new SessionError("SESSION_CREATION_FAILED");
  }

  // 4. Never issue a session for a different Auth UUID.
  if (verified.user.id !== identity.supabaseUserId) {
    throw new SessionError("SESSION_IDENTITY_MISMATCH");
  }

  const session = verified.session;

  if (
    !session.access_token ||
    !session.refresh_token
  ) {
    throw new SessionError("SESSION_CREATION_FAILED");
  }

  // 5. Return the native Supabase session credentials.
  return {
    accessToken: session.access_token,
    refreshToken: session.refresh_token,
    expiresIn: session.expires_in,
    expiresAt: session.expires_at ?? null,
    userId: verified.user.id,
  };
}
