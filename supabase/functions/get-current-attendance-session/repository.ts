
import { createClient } from "npm:@supabase/supabase-js@2";

export async function findCurrentSession(
  userId: string,
  meetingId: string,
) {
  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const serviceRoleKey =
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");

  if (!supabaseUrl || !serviceRoleKey) {
    throw new Error("SERVER_CONFIGURATION_ERROR");
  }

  const supabaseAdmin = createClient(
    supabaseUrl,
    serviceRoleKey,
    {
      auth: {
        persistSession: false,
        autoRefreshToken: false,
      },
    },
  );

  return await supabaseAdmin.rpc(
    "get_current_attendance_session",
    {
      p_user_id: userId,
      p_meeting_id: meetingId,
    },
  );
}
