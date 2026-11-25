
export type MoodleIdentity = {
  moodleUserId: number;
  username: string;
};

export type MoodleAuthErrorCode =
  | "INVALID_CREDENTIALS"
  | "MOODLE_UNAVAILABLE"
  | "MOODLE_SERVICE_ERROR"
  | "INVALID_MOODLE_RESPONSE";

export class MoodleAuthError extends Error {
  constructor(public readonly code: MoodleAuthErrorCode) {
    super(code);
    this.name = "MoodleAuthError";
  }
}

const SERVICE = "smartattendance_login";

function getMoodleBaseUrl(): string {
  const raw = Deno.env.get("MOODLE_BASE_URL");

  if (!raw) {
    throw new MoodleAuthError("MOODLE_SERVICE_ERROR");
  }

  const url = new URL(raw);

  if (url.protocol !== "https:") {
    throw new MoodleAuthError("MOODLE_SERVICE_ERROR");
  }

  if (url.username || url.password || url.search || url.hash) {
    throw new MoodleAuthError("MOODLE_SERVICE_ERROR");
  }

  url.pathname = url.pathname.replace(/\/+$/, "");
  return url.toString().replace(/\/$/, "");
}

async function postForm(
  endpoint: string,
  data: Record<string, string>
): Promise<Record<string, unknown>> {
  let response: Response;

  try {
    response = await fetch(
      `${getMoodleBaseUrl()}${endpoint}`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/x-www-form-urlencoded",
          "Accept": "application/json"
        },
        body: new URLSearchParams(data).toString(),
        signal: AbortSignal.timeout(10000),
        redirect: "error"
      }
    );
  } catch {
    throw new MoodleAuthError("MOODLE_UNAVAILABLE");
  }

  if (!response.ok) {
    throw new MoodleAuthError("MOODLE_UNAVAILABLE");
  }

  let result: unknown;

  try {
    result = await response.json();
  } catch {
    throw new MoodleAuthError("INVALID_MOODLE_RESPONSE");
  }

  if (
    typeof result !== "object" ||
    result === null ||
    Array.isArray(result)
  ) {
    throw new MoodleAuthError("INVALID_MOODLE_RESPONSE");
  }

  return result as Record<string, unknown>;
}

export async function authenticateWithMoodle(
  username: string,
  password: string
): Promise<MoodleIdentity> {
  if (!username.trim() || !password) {
    throw new MoodleAuthError("INVALID_CREDENTIALS");
  }

  // 1. Moodle validates username/password.
  const login = await postForm("/login/token.php", {
    username,
    password,
    service: SERVICE
  });

  if (login.errorcode === "invalidlogin") {
    throw new MoodleAuthError("INVALID_CREDENTIALS");
  }

  if (typeof login.token !== "string" || !login.token) {
    throw new MoodleAuthError("MOODLE_SERVICE_ERROR");
  }

  // 2. Retrieve the identity associated with the token.
  const site = await postForm(
    "/webservice/rest/server.php",
    {
      wstoken: login.token,
      wsfunction: "core_webservice_get_site_info",
      moodlewsrestformat: "json"
    }
  );

  if (site.errorcode || site.exception) {
    throw new MoodleAuthError("MOODLE_SERVICE_ERROR");
  }

  if (
    !Number.isSafeInteger(site.userid) ||
    (site.userid as number) <= 0 ||
    typeof site.username !== "string" ||
    !site.username
  ) {
    throw new MoodleAuthError("INVALID_MOODLE_RESPONSE");
  }

  return {
    moodleUserId: site.userid as number,
    username: site.username
  };
}
