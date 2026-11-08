
import { createAdminClient } from "../_shared/supabase.ts";

import type {
  QrIssuanceContext,
} from "./types.ts";


/**
 * Các mã lỗi Repository được phép đưa
 * lên tầng Service và Error Handler.
 */
export type QrRepositoryErrorCode =
  | "INVALID_QR_REQUEST"
  | "SESSION_NOT_FOUND"
  | "MEETING_NOT_FOUND"
  | "NOT_COURSE_LECTURER"
  | "QR_NOT_AVAILABLE"
  | "INVALID_QR_CONFIGURATION"
  | "RPC_ERROR"
  | "INVALID_RPC_RESPONSE";


/**
 * Lỗi nội bộ của Repository.
 */
export class QrRepositoryError extends Error {
  constructor(
    public readonly code: QrRepositoryErrorCode,
  ) {
    super(code);
    this.name = "QrRepositoryError";
  }
}


/**
 * Danh sách lỗi nghiệp vụ từ PostgreSQL RPC.
 */
const knownRpcErrors = new Set<string>([
  "INVALID_QR_REQUEST",
  "SESSION_NOT_FOUND",
  "MEETING_NOT_FOUND",
  "NOT_COURSE_LECTURER",
  "QR_NOT_AVAILABLE",
  "INVALID_QR_CONFIGURATION",
]);


/**
 * Kiểm tra giá trị có phải object hay không.
 */
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
 * Lấy ngữ cảnh nội bộ phục vụ việc tạo QR.
 *
 * userId phải được lấy từ JWT đã xác thực.
 * Không nhận userId trực tiếp từ Android body.
 */
export async function getQrIssuanceContext(
  userId: string,
  sessionId: string,
): Promise<QrIssuanceContext> {

  // 1. Tạo client với quyền backend.
  const admin = createAdminClient();

  // 2. Gọi PostgreSQL RPC.
  const { data, error } = await admin.rpc(
    "get_qr_issuance_context",
    {
      p_user_id: userId,
      p_session_id: sessionId,
    },
  );

  // 3. Xử lý lỗi từ PostgreSQL.
  if (error) {
    const code =
      error.code === "P0001" &&
      knownRpcErrors.has(error.message)
        ? error.message as QrRepositoryErrorCode
        : "RPC_ERROR";

    throw new QrRepositoryError(code);
  }

  // 4. Kiểm tra dữ liệu trả về.
  if (!isRecord(data)) {
    throw new QrRepositoryError(
      "INVALID_RPC_RESPONSE",
    );
  }

  // 5. Kiểm tra định dạng các trường.
  if (
    typeof data.session_id !== "string" ||
    typeof data.qr_secret !== "string" ||
    !/^[0-9a-f]{64}$/.test(data.qr_secret) ||
    typeof data.qr_refresh_seconds !== "number" ||
    !Number.isSafeInteger(data.qr_refresh_seconds) ||
    data.qr_refresh_seconds < 5 ||
    typeof data.session_expires_at !== "string" ||
    typeof data.server_time !== "string"
  ) {
    throw new QrRepositoryError(
      "INVALID_RPC_RESPONSE",
    );
  }

  // 6. Kiểm tra thời gian trả về.
  const expiresAt = Date.parse(
    data.session_expires_at,
  );

  const serverTime = Date.parse(
    data.server_time,
  );

  if (
    !Number.isFinite(expiresAt) ||
    !Number.isFinite(serverTime) ||
    expiresAt <= serverTime
  ) {
    throw new QrRepositoryError(
      "INVALID_RPC_RESPONSE",
    );
  }

  // 7. Chỉ trả dữ liệu cần thiết cho Service.
  return {
    session_id: data.session_id,
    qr_secret: data.qr_secret,
    qr_refresh_seconds: data.qr_refresh_seconds,
    session_expires_at: data.session_expires_at,
    server_time: data.server_time,
  };
}
