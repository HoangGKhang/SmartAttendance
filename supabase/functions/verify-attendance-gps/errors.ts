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

  // 1. Authentication errors.
  if (error instanceof AuthenticationError) {
    return {
      status: 401,
      message: "UNAUTHORIZED",
    };
  }

  // 2. Input validation errors.
  if (error instanceof GpsValidationError) {
    return {
      status: 400,
      message: error.message,
    };
  }

  // 3. PostgreSQL RPC errors.
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

        case "ATTENDANCE_RECORD_NOT_FOUND":
          return {
            status: 404,
            message: "ATTENDANCE_RECORD_NOT_FOUND",
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

        case "QR_VERIFICATION_REQUIRED":
          return {
            status: 409,
            message: "QR_VERIFICATION_REQUIRED",
          };

        case "ALREADY_CHECKED_IN":
          return {
            status: 409,
            message: "ALREADY_CHECKED_IN",
          };
      }
    }

    return {
      status: 500,
      message: "GPS_VERIFICATION_FAILED",
    };
  }

  // 4. Unexpected errors.
  return {
    status: 500,
    message: "INTERNAL_SERVER_ERROR",
  };
}