
import { createClient } from "npm:@supabase/supabase-js@2";

export async function closeAttendanceSession(
  userId: string,
  sessionId: string,
) {
  // 1. Đọc cấu hình server.
  const supabaseUrl = Deno.env.get("SUPABASE_URL");

  const serviceRoleKey =
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");

  if (!supabaseUrl || !serviceRoleKey) {
    throw new Error("SERVER_CONFIGURATION_ERROR");
  }

  // 2. Tạo Supabase Client cho backend.
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

  // 3. Gọi PostgreSQL RPC.
  return await supabaseAdmin.rpc(
    "close_attendance_session",
    {
      p_user_id: userId,
      p_session_id: sessionId,
    },
  );
}
