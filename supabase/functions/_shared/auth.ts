
import { createAdminClient } from "./supabase.ts";

export class AuthenticationError extends Error {
  constructor(message = "Unauthorized") {
    super(message);
    this.name = "AuthenticationError";
  }
}

export async function authenticateRequest(
  request: Request,
): Promise<string> {
  const header = request.headers.get("Authorization");

  if (!header?.startsWith("Bearer ")) {
    throw new AuthenticationError();
  }

  const token = header.slice(7).trim();

  if (!token) {
    throw new AuthenticationError();
  }

  const supabase = createAdminClient();

  const { data, error } = await supabase.auth.getUser(token);

  if (error || !data.user) {
    throw new AuthenticationError("Invalid session");
  }

  return data.user.id;
}
