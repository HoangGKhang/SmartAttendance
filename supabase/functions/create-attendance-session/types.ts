
/**
 * Dữ liệu đã được validation,
 * sẵn sàng chuyển đến Service.
 */
export interface CreateSessionInput {
  meetingId: string;
  durationMinutes: number;

  // GPS Configuration
  latitude: number;
  longitude: number;
  radiusMeters: number;

  // Độ chính xác do thiết bị khai báo.
  accuracyMeters: number;
}

/**
 * Dữ liệu Session trả về Android.
 * Không chứa qr_secret.
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
