
/**
 * Dữ liệu Meeting đã được kiểm tra và chuẩn hóa.
 *
 * Các trường nullable sẽ được lưu thành null
 * thay vì undefined.
 */
export interface CreateMeetingInput {
  courseId: string;
  meetingNumber: number;
  meetingDate: string;
  title: string | null;
  startTime: string | null;
  endTime: string | null;
  room: string | null;
}

/**
 * Thông tin người dùng đã xác thực.
 */
export interface AuthenticatedUser {
  id: string;
}
