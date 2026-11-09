
import {
  createAdminClient,
} from "../_shared/supabase.ts";

import type {
  QrVerificationContext,
} from "./types.ts";


export type QrVerificationRepositoryErrorCode =
  | "INVALID_QR_REQUEST"
  | "SESSION_NOT_FOUND"
  | "MEETING_NOT_FOUND"
  | "NOT_COURSE_STUDENT"
  | "QR_NOT_AVAILABLE"
  | "INVALID_QR_CONFIGURATION"
  | "RPC_ERROR"
  | "INVALID_RPC_RESPONSE";


export class QrVerificationRepositoryError
  extends Error {

  constructor(
    public readonly code:
      QrVerificationRepositoryErrorCode,
  ) {
    super(code);
    this.name = "QrVerificationRepositoryError";
  }
}


const knownRpcErrors = new Set<string>([
  "INVALID_QR_REQUEST",
  "SESSION_NOT_FOUND",
  "MEETING_NOT_FOUND",
  "NOT_COURSE_STUDENT",
  "QR_NOT_AVAILABLE",
  "INVALID_QR_CONFIGURATION",
]);


function isRecord(
  value: unknown,
): value is Record<string, unknown> {
  return (
    value !== null &&
    typeof value === "object" &&
    !Array.isArray(value)
  );
}


/**
 * Lấy ngữ cảnh xác minh QR.
 *
 * userId phải được lấy từ JWT đã xác thực.
 */
export async function getQrVerificationContext(
  userId: string,
  sessionId: string,
): Promise<QrVerificationContext> {

  // 1. Tạo Supabase Admin Client.
  const admin = createAdminClient();


  // 2. Gọi RPC kiểm tra Student và Session.
  const { data, error } = await admin.rpc(
    "get_qr_verification_context",
    {
      p_user_id: userId,
      p_session_id: sessionId,
    },
  );


  // 3. Phân loại lỗi nghiệp vụ.
  if (error) {
    const code =
      error.code === "P0001" &&
      knownRpcErrors.has(error.message)
        ? error.message as
            QrVerificationRepositoryErrorCode
        : "RPC_ERROR";

    throw new QrVerificationRepositoryError(
      code,
    );
  }


  // 4. Kiểm tra kiểu dữ liệu trả về.
  if (!isRecord(data)) {
    throw new QrVerificationRepositoryError(
      "INVALID_RPC_RESPONSE",
    );
  }


  // 5. Kiểm tra các trường bắt buộc.
  if (
    typeof data.session_id !== "string" ||
    typeof data.meeting_id !== "string" ||
    typeof data.qr_secret !== "string" ||
    !/^[0-9a-f]{64}$/.test(data.qr_secret) ||
    typeof data.qr_refresh_seconds !== "number" ||
    !Number.isSafeInteger(data.qr_refresh_seconds) ||
    data.qr_refresh_seconds < 5 ||
    typeof data.session_expires_at !== "string" ||
    typeof data.server_time !== "string"
  ) {
    throw new QrVerificationRepositoryError(
      "INVALID_RPC_RESPONSE",
    );
  }


  // 6. Kiểm tra định dạng thời gian.
  const expiresAt = Date.parse(
    data.session_expires_at,
  );

  const serverTime = Date.parse(
    data.server_time,
  );

  if (
    !Number.isFinite(expiresAt) ||
    !Number.isFinite(serverTime)
  ) {
    throw new QrVerificationRepositoryError(
      "INVALID_RPC_RESPONSE",
    );
  }


  // 7. Trả về dữ liệu nội bộ cho Service.
  return {
    session_id: data.session_id,
    meeting_id: data.meeting_id,
    qr_secret: data.qr_secret,
    qr_refresh_seconds:
      data.qr_refresh_seconds,
    session_expires_at:
      data.session_expires_at,
    server_time: data.server_time,
  };
}
