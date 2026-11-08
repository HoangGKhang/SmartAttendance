
/**
 * Request đã được validation.
 * User ID không lấy từ body.
 */
export interface IssueQrInput {
  sessionId: string;
}

/**
 * Dữ liệu nội bộ PostgreSQL RPC trả về.
 * Không được gửi trực tiếp đối tượng này cho Android.
 */
export interface QrIssuanceContext {
  session_id: string;
  qr_secret: string;
  qr_refresh_seconds: number;
  session_expires_at: string;
  server_time: string;
}

/**
 * Response công khai trả về Android.
 */
export interface IssueQrResponse {
  qrToken: string;
  qrExpiresAt: string;
}
