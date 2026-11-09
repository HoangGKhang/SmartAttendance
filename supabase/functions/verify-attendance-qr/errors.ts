
import {
  AuthenticationError,
} from "../_shared/auth.ts";

import {
  ValidationError,
} from "./validation.ts";

import {
  QrVerificationRepositoryError,
} from "./repository.ts";

import {
  QrVerificationError,
} from "./service.ts";


export interface ApiError {
  status: number;
  message: string;
}


/**
 * Chuyển lỗi nội bộ thành HTTP response an toàn.
 */
export function mapVerifyQrError(
  error: unknown,
): ApiError {

  // 1. Xác thực thất bại.
  if (error instanceof AuthenticationError) {
    return {
      status: 401,
      message: "UNAUTHORIZED",
    };
  }

  // 2. QR Token sai định dạng.
  if (error instanceof ValidationError) {
    switch (error.message) {
      case "INVALID_REQUEST_BODY":
      case "INVALID_QR_TOKEN":
      case "UNSUPPORTED_QR_VERSION":
        return {
          status: 400,
          message: error.message,
        };

      default:
        return {
          status: 400,
          message: "INVALID_QR_TOKEN",
        };
    }
  }

  // 3. PostgreSQL RPC từ chối.
  if (
    error instanceof
      QrVerificationRepositoryError
  ) {
    switch (error.code) {

      case "INVALID_QR_REQUEST":
        return {
          status: 400,
          message: "INVALID_QR_REQUEST",
        };

      case "SESSION_NOT_FOUND":
      case "MEETING_NOT_FOUND":
        return {
          status: 404,
          message: "SESSION_NOT_FOUND",
        };

      case "NOT_COURSE_STUDENT":
        return {
          status: 403,
          message: "FORBIDDEN",
        };

      case "QR_NOT_AVAILABLE":
        return {
          status: 409,
          message: "QR_NOT_AVAILABLE",
        };

      case "INVALID_QR_CONFIGURATION":
      case "INVALID_RPC_RESPONSE":
      case "RPC_ERROR":
        return {
          status: 500,
          message: "QR_VERIFICATION_FAILED",
        };
    }
  }

  // 4. Lỗi xác minh HMAC và thời gian.
  if (error instanceof QrVerificationError) {
    switch (error.code) {

      case "QR_EXPIRED":
        return {
          status: 409,
          message: "QR_EXPIRED",
        };

      case "INVALID_QR_SIGNATURE":
        return {
          status: 400,
          message: "INVALID_QR_SIGNATURE",
        };

      case "INVALID_QR_CONTEXT":
        return {
          status: 500,
          message: "QR_VERIFICATION_FAILED",
        };
    }
  }

  // 5. Lỗi không mong đợi.
  return {
    status: 500,
    message: "INTERNAL_SERVER_ERROR",
  };
}
