
import {
  authenticateRequest,
  AuthenticationError,
} from "../_shared/auth.ts";

import {
  jsonResponse,
  errorResponse,
} from "../_shared/response.ts";

import {
  validateCreateMeeting,
  ValidationError,
} from "./validation.ts";

import {
  createMeetingService,
  ForbiddenError,
} from "./service.ts";

import { RepositoryError } from "./repository.ts";

/**
 * HTTP Entry Point
 *
 * POST /functions/v1/create-class-meeting
 */
Deno.serve(async (req: Request): Promise<Response> => {

  // 1. Chỉ chấp nhận HTTP POST
  if (req.method !== "POST") {
    return errorResponse("Method not allowed", 405);
  }

  try {
    // 2. Xác thực người dùng từ Bearer JWT
    const userId = await authenticateRequest(req);

    // 3. Đọc JSON Request Body
    let body: unknown;

    try {
      body = await req.json();
    } catch {
      throw new ValidationError("Invalid JSON body");
    }

    // 4. Kiểm tra và chuẩn hóa dữ liệu
    const input = validateCreateMeeting(body);

    // 5. Thực hiện nghiệp vụ tạo Meeting
    const meeting = await createMeetingService(
      userId,
      input,
    );

    // 6. Trả kết quả thành công
    return jsonResponse({ meeting }, 201);

  } catch (error: unknown) {

    // Chưa xác thực hoặc JWT không hợp lệ
    if (error instanceof AuthenticationError) {
      return errorResponse(error.message, 401);
    }

    // Dữ liệu đầu vào không hợp lệ
    if (error instanceof ValidationError) {
      return errorResponse(error.message, 400);
    }

    // Không có quyền tạo Meeting
    if (error instanceof ForbiddenError) {
      return errorResponse("Forbidden", 403);
    }

    // Lỗi từ Supabase PostgreSQL
    if (error instanceof RepositoryError) {

      // Unique constraint violation
      if (error.code === "23505") {
        return errorResponse(
          "Meeting number already exists",
          409,
        );
      }

      console.error("Database operation failed", {
        operation: error.operation,
        code: error.code,
      });

      return errorResponse(
        "Unable to create meeting",
        500,
      );
    }

    // Lỗi hệ thống không dự đoán trước
    console.error("Unexpected error", error);

    return errorResponse(
      "Internal server error",
      500,
    );
  }
});
