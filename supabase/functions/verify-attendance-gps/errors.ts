import {
  AuthenticationError,
} from "../_shared/auth.ts";

import {
  GpsValidationError,
} from "./validation.ts";

import {
  GpsRepositoryError,
} from "./repository.ts";

interface ApiError {
  status: number;
  message: string;
}

export function mapVerifyGpsError(
  error: unknown,
): ApiError {
  if (error instanceof AuthenticationError) {
    return {
      status: 401,
      message: "UNAUTHORIZED",
    };
  }

  if (error instanceof GpsValidationError) {
    return {
      status: 400,
      message: error.message,
    };
  }

  if (error instanceof GpsRepositoryError) {
    if (error.code === "P0001") {
      switch (error.databaseMessage) {
        case "INVALID_GPS_DATA":
          return {
            status: 400,
            message: "INVALID_GPS_DATA",
          };

        case "NOT_ENROLLED":
          return {
            status: 403,
            message: "FORBIDDEN",
          };

        case "SESSION_NOT_FOUND":
          return {
            status: 404,
            message: "SESSION_NOT_FOUND",
          };

        case "SESSION_UNAVAILABLE":
        case "MEETING_UNAVAILABLE":
          return {
            status: 409,
            message: "SESSION_UNAVAILABLE",
          };

        case "GPS_NOT_CONFIGURED":
          return {
            status: 409,
            message: "GPS_NOT_CONFIGURED",
          };
      }
    }

    return {
      status: 500,
      message: "GPS_VERIFICATION_FAILED",
    };
  }

  return {
    status: 500,
    message: "INTERNAL_SERVER_ERROR",
  };
}