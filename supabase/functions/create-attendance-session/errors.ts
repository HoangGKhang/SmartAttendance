
import { AuthenticationError } from "../_shared/auth.ts";
import { ValidationError } from "./validation.ts";
import { SessionRepositoryError } from "./repository.ts";

interface ApiError {
  status: number;
  message: string;
}

export function mapCreateSessionError(
  error: unknown,
): ApiError {
  if (error instanceof AuthenticationError) {
    return { status: 401, message: error.message };
  }

  if (error instanceof ValidationError) {
    return { status: 400, message: error.message };
  }

  if (error instanceof SessionRepositoryError) {
    if (error.code === "P0001") {
      switch (error.databaseMessage) {
        case "INVALID_SESSION_DATA":
          return { status: 400, message: "Invalid session data" };

        case "MEETING_NOT_FOUND":
          return { status: 404, message: "Meeting not found" };

        case "NOT_COURSE_LECTURER":
          return { status: 403, message: "Forbidden" };

        case "MEETING_NOT_SCHEDULED":
          return {
            status: 409,
            message: "Meeting is not scheduled",
          };

        case "SESSION_ALREADY_OPEN":
          return {
            status: 409,
            message: "Attendance session already open",
          };
      }
    }

    // Partial UNIQUE Index chống hai Session OPEN.
    if (error.code === "23505") {
      return {
        status: 409,
        message: "Attendance session already open",
      };
    }

    return {
      status: 500,
      message: "Unable to create attendance session",
    };
  }

  return {
    status: 500,
    message: "Internal server error",
  };
}
