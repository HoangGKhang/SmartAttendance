
import {
  authenticateRequest,
} from "../_shared/auth.ts";

import {
  validateVerifyQrRequest,
  ValidationError,
} from "./validation.ts";

import {
  verifyAttendanceQrService,
} from "./service.ts";

import {
  mapVerifyQrError,
} from "./errors.ts";


/**
 * Response JSON không được lưu vào HTTP cache.
 */
function jsonNoStore(
  data: unknown,
  status = 200,
): Response {
  return Response.json(data, {
    status,
    headers: {
      "Cache-Control": "no-store",
      "Pragma": "no-cache",
      "X-Content-Type-Options": "nosniff",
    },
  });
}


/**
 * POST /functions/v1/verify-attendance-qr
 */
Deno.serve(async (req: Request): Promise<Response> => {

  // 1. Chỉ chấp nhận POST.
  if (req.method !== "POST") {
    return Response.json(
      {
        error: "METHOD_NOT_ALLOWED",
      },
      {
        status: 405,
        headers: {
          "Allow": "POST",
          "Cache-Control": "no-store",
        },
      },
    );
  }

  try {

    // 2. Xác thực JWT của Student.
    const userId = await authenticateRequest(req);

    // 3. Đọc JSON body.
    let body: unknown;

    try {
      body = await req.json();
    } catch {
      throw new ValidationError(
        "INVALID_REQUEST_BODY",
      );
    }

    // 4. Kiểm tra dữ liệu đầu vào.
    const input = validateVerifyQrRequest(body);

    // 5. Gọi Service xác minh QR.
    const result = await verifyAttendanceQrService(
      userId,
      input,
    );

    // 6. Chỉ trả kết quả công khai.
    return jsonNoStore(
      {
        verified: true,
        sessionId: result.sessionId,
        meetingId: result.meetingId,
        verifiedAt: result.verifiedAt,
      },
      200,
    );

  } catch (error: unknown) {

    // 7. Chuyển lỗi thành HTTP status.
    const apiError = mapVerifyQrError(error);

    // Không log QR Token, JWT,
    // qr_secret hoặc toàn bộ exception.
    if (apiError.status >= 500) {
      console.error(
        "Verify attendance QR failed",
        {
          status: apiError.status,
          category: apiError.message,
        },
      );
    }

    return jsonNoStore(
      {
        error: apiError.message,
      },
      apiError.status,
    );
  }
});
