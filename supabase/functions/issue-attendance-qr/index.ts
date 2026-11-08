
import {
  authenticateRequest,
} from "../_shared/auth.ts";

import {
  validateIssueQrRequest,
  ValidationError,
} from "./validation.ts";

import {
  issueAttendanceQrService,
} from "./service.ts";

import {
  mapIssueQrError,
} from "./errors.ts";


/**
 * Response JSON dành riêng cho endpoint cấp QR.
 *
 * Không cho các lớp HTTP cache lưu token.
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
 * HTTP endpoint:
 * POST /functions/v1/issue-attendance-qr
 */
Deno.serve(
  async (req: Request): Promise<Response> => {

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

      // 2. Xác thực JWT của người dùng.
      // userId lấy từ Supabase Auth,
      // không lấy từ request body.
      const userId =
        await authenticateRequest(req);

      // 3. Đọc JSON request.
      let body: unknown;

      try {
        body = await req.json();
      } catch {
        throw new ValidationError(
          "INVALID_REQUEST_BODY",
        );
      }

      // 4. Kiểm tra sessionId.
      const input =
        validateIssueQrRequest(body);

      // 5. Gọi Service cấp QR.
      const result =
        await issueAttendanceQrService(
          userId,
          input,
        );

      // 6. Chỉ trả hai trường công khai.
      return jsonNoStore(
        {
          qrToken: result.qrToken,
          qrExpiresAt: result.qrExpiresAt,
        },
        200,
      );

    } catch (error: unknown) {

      // 7. Chuyển lỗi thành HTTP status.
      const apiError =
        mapIssueQrError(error);

      // Không log request body,
      // qr_secret hoặc qrToken.
      if (apiError.status >= 500) {
        console.error(
          "Issue attendance QR failed",
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
  },
);
