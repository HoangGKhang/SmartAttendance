
import {
  AuthenticationError,
} from "../_shared/auth.ts";

import {
  ValidationError,
} from "./validation.ts";

import {
  QrRepositoryError,
} from "./repository.ts";

import {
  QrServiceError,
} from "./service.ts";


export interface ApiError {
  status: number;
  message: string;
}


/**
 * Chuyển lỗi nội bộ thành HTTP error.
 * Không trả lỗi SQL chi tiết cho Android.
 */
export function mapIssueQrError(
  error: unknown,
): ApiError {

  // 1. JWT không hợp lệ.
  if (error instanceof AuthenticationError) {
    return {
      status: 401,
      message: "UNAUTHORIZED",
    };
  }

  // 2. Request body không hợp lệ.
  if (error instanceof ValidationError) {
    return {
      status: 400,
      message: error.message,
    };
  }

  // 3. PostgreSQL RPC từ chối yêu cầu.
  if (error instanceof QrRepositoryError) {
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

      case "NOT_COURSE_LECTURER":
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
          message: "QR_ISSUANCE_FAILED",
        };
    }
  }

  // 4. Lỗi trong quá trình tạo token.
  if (error instanceof QrServiceError) {
    if (error.code === "QR_WINDOW_EXPIRED") {
      return {
        status: 409,
        message: "QR_WINDOW_EXPIRED",
      };
    }

    return {
      status: 500,
      message: "QR_ISSUANCE_FAILED",
    };
  }

  // 5. Lỗi không xác định.
  return {
    status: 500,
    message: "INTERNAL_SERVER_ERROR",
  };
}
