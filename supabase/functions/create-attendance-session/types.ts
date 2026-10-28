
/**
 * Dữ liệu đã được validation,
 * sẵn sàng chuyển đến Service.
 */
export interface CreateSessionInput {
  meetingId: string;
  durationMinutes: number;
}

/**
 * Dữ liệu an toàn trả về Android.
 *
 * Không có qr_secret.
 */
export interface AttendanceSessionResponse {
  id: string;
  meeting_id: string;
  created_by: string;
  status: "OPEN";
  opened_at: string;
  expires_at: string;
  closed_at: string | null;
}
