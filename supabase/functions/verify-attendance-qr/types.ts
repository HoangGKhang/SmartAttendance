
/**
 * Request từ Android Student.
 */
export interface VerifyQrInput {
  qrToken: string;
}


/**
 * Kết quả phân tích token.
 * Chưa chứng minh token hợp lệ.
 */
export interface ParsedQrToken {
  version: "sa1";
  sessionId: string;
  window: number;
  signature: string;
}


/**
 * Dữ liệu nội bộ từ PostgreSQL RPC.
 * Chỉ được dùng trong backend.
 */
export interface QrVerificationContext {
  session_id: string;
  meeting_id: string;
  qr_secret: string;
  qr_refresh_seconds: number;
  session_expires_at: string;
  server_time: string;
}


/**
 * Response công khai cho Android Student.
 */
export interface VerifyQrResponse {
  verified: boolean;
  sessionId: string;
  meetingId: string;
  verifiedAt: string;
}
