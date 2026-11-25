
import { createClient } from "npm:@supabase/supabase-js@2";

export type SmartAttendanceRole = "STUDENT" | "LECTURER";

export type LinkedIdentity = {
  moodleUserId: number;
  supabaseUserId: string;
  role: SmartAttendanceRole;
};

export type IdentityErrorCode =
  | "INVALID_MOODLE_USER_ID"
  | "IDENTITY_NOT_PROVISIONED"
  | "INCOMPLETE_IDENTITY"
  | "IDENTITY_MISMATCH"
  | "ACCOUNT_UNAVAILABLE"
  | "BACKEND_ERROR";

export class IdentityError extends Error {
  constructor(public readonly code: IdentityErrorCode) {
    super(code);
    this.name = "IdentityError";
  }
}

/**
 * Privileged Supabase client.
 * Only use this inside a trusted Edge Function.
 */
function createAdminClient() {
  const url = Deno.env.get("SUPABASE_URL");
  const key = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");

  if (!url || !key) {
    throw new IdentityError("BACKEND_ERROR");
  }

  return createClient(url, key, {
    auth: {
      autoRefreshToken: false,
      persistSession: false,
      detectSessionInUrl: false,
    },
  });
}

/**
 * Resolve a verified Moodle user ID
 * to an existing Supabase Auth identity.
 */
export async function findLinkedIdentity(
  moodleUserId: number,
): Promise<LinkedIdentity> {
  if (
    !Number.isSafeInteger(moodleUserId) ||
    moodleUserId <= 0
  ) {
    throw new IdentityError("INVALID_MOODLE_USER_ID");
  }

  const supabase = createAdminClient();

  // 1. Look up the Moodle -> Supabase mapping.
  const { data: mapping, error: mappingError } = await supabase
    .from("moodle_user_provisioning")
    .select("moodle_user_id, supabase_user_id, status")
    .eq("moodle_user_id", moodleUserId)
    .maybeSingle();

  if (mappingError) {
    throw new IdentityError("BACKEND_ERROR");
  }

  if (!mapping) {
    throw new IdentityError("IDENTITY_NOT_PROVISIONED");
  }

  if (
    !["AUTH_CREATED", "LINKED"].includes(mapping.status) ||
    !mapping.supabase_user_id
  ) {
    throw new IdentityError("INCOMPLETE_IDENTITY");
  }

  const userId = mapping.supabase_user_id;

  // 2. Verify the existing Supabase Auth user.
  const { data: authData, error: authError } =
    await supabase.auth.admin.getUserById(userId);

  if (authError || !authData?.user) {
    throw new IdentityError("ACCOUNT_UNAVAILABLE");
  }

  if (authData.user.id !== userId) {
    throw new IdentityError("IDENTITY_MISMATCH");
  }

  // Check account-state fields when provided by Auth.
  const authState = authData.user as unknown as {
    banned_until?: string | null;
    deleted_at?: string | null;
  };

  if (authState.deleted_at) {
    throw new IdentityError("ACCOUNT_UNAVAILABLE");
  }

  if (
    authState.banned_until &&
    Date.parse(authState.banned_until) > Date.now()
  ) {
    throw new IdentityError("ACCOUNT_UNAVAILABLE");
  }

  // 3. Verify the application profile.
  const { data: profile, error: profileError } = await supabase
    .from("profiles")
    .select("id, moodle_user_id, role")
    .eq("id", userId)
    .maybeSingle();

  if (profileError) {
    throw new IdentityError("BACKEND_ERROR");
  }

  if (!profile) {
    throw new IdentityError("INCOMPLETE_IDENTITY");
  }

  if (
    profile.id !== userId ||
    profile.moodle_user_id !== moodleUserId
  ) {
    throw new IdentityError("IDENTITY_MISMATCH");
  }

  if (
    profile.role !== "STUDENT" &&
    profile.role !== "LECTURER"
  ) {
    throw new IdentityError("INCOMPLETE_IDENTITY");
  }

  // 4. Return only the identity needed by the next layer.
  return {
    moodleUserId,
    supabaseUserId: userId,
    role: profile.role,
  };
}
