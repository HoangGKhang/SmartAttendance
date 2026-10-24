
import { AuthenticationError } from "../_shared/auth.ts";
import { ValidationError } from "./validation.ts";
import { ForbiddenError } from "./service.ts";
import { RepositoryError } from "./repository.ts";

export interface ApiErrorResult {
  status: number;
  message: string;
}

export function mapCreateMeetingError(
  error: unknown,
): ApiErrorResult {

  // 1. Authentication errors
  if (error instanceof AuthenticationError) {
    return {
      status: 401,
      message: error.message,
    };
  }

  // 2. Validation errors
  if (error instanceof ValidationError) {
    return {
      status: 400,
      message: error.message,
    };
  }

  // 3. Authorization errors
  if (error instanceof ForbiddenError) {
    return {
      status: 403,
      message: "Forbidden",
    };
  }

  // 4. Database / RPC errors
  if (error instanceof RepositoryError) {

    // Duplicate meeting number
    if (
      error.operation === "create meeting with records" &&
      error.code === "23505"
    ) {
      return {
        status: 409,
        message: "Meeting number already exists",
      };
    }

    // PostgreSQL business errors
    if (error.code === "P0001") {
      switch (error.databaseMessage) {
        case "NOT_COURSE_LECTURER":
          return {
            status: 403,
            message: "Forbidden",
          };

        case "COURSE_NOT_ACTIVE":
          return {
            status: 403,
            message: "Course is not active",
          };

        case "INVALID_MEETING_DATA":
          return {
            status: 400,
            message: "Invalid meeting data",
          };
      }
    }

    // Unexpected database error
    return {
      status: 500,
      message: "Unable to create meeting",
    };
  }

  // 5. Unexpected error
  return {
    status: 500,
    message: "Internal server error",
  };
}
