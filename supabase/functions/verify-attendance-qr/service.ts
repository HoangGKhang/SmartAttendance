
import type {
  VerifyQrInput,
  VerifyQrResponse,
} from "./types.ts";

import {
  parseQrToken,
} from "./validation.ts";

import {
  getQrVerificationContext,
} from "./repository.ts";


export type QrVerificationErrorCode =
  | "QR_EXPIRED"
  | "INVALID_QR_SIGNATURE"
  | "INVALID_QR_CONTEXT";


export class QrVerificationError extends Error {
  constructor(
    public readonly code: QrVerificationErrorCode,
  ) {
    super(code);
    this.name = "QrVerificationError";
  }
}


/**
 * Chuyển secret dạng hex thành 32 byte gốc.
 * Phải giống thuật toán bên Issue QR.
 */
function hexToBytes(hex: string): Uint8Array {

  if (!/^[0-9a-f]{64}$/.test(hex)) {
    throw new QrVerificationError(
      "INVALID_QR_CONTEXT",
    );
  }

  const bytes = new Uint8Array(32);

  for (let i = 0; i < 32; i++) {
    const pair = hex.slice(
      i * 2,
      i * 2 + 2,
    );

    bytes[i] = Number.parseInt(pair, 16);
  }

  return bytes;
}


/**
 * Chuyển chữ ký Base64URL thành byte.
 */
function base64UrlToBytes(
  encoded: string,
): Uint8Array {

  try {
    const base64 = encoded
      .replace(/-/g, "+")
      .replace(/_/g, "/");

    const padded = base64.padEnd(
      Math.ceil(base64.length / 4) * 4,
      "=",
    );

    const binary = atob(padded);

    const bytes = Uint8Array.from(
      binary,
      (char) => char.charCodeAt(0),
    );

    if (bytes.length !== 32) {
      throw new Error("Invalid signature length");
    }

    // Kiểm tra biểu diễn Base64URL chuẩn.
    const canonical = btoa(binary)
      .replace(/\+/g, "-")
      .replace(/\//g, "_")
      .replace(/=+$/g, "");

    if (canonical !== encoded) {
      throw new Error("Non-canonical signature");
    }

    return bytes;

  } catch {
    throw new QrVerificationError(
      "INVALID_QR_SIGNATURE",
    );
  }
}


/**
 * Xác minh chữ ký bằng Web Crypto API.
 */
async function verifyQrSignature(
  secretHex: string,
  message: string,
  signatureBase64Url: string,
): Promise<boolean> {

  const secretBytes = hexToBytes(secretHex);

  const signatureBytes = base64UrlToBytes(
    signatureBase64Url,
  );

  // Import secret để xác minh HMAC-SHA256.
  const key = await crypto.subtle.importKey(
    "raw",
    secretBytes,
    {
      name: "HMAC",
      hash: "SHA-256",
    },
    false,
    ["verify"],
  );

  const messageBytes =
    new TextEncoder().encode(message);

  return await crypto.subtle.verify(
    "HMAC",
    key,
    signatureBytes,
    messageBytes,
  );
}


/**
 * Xác minh QR cho Student đã đăng nhập.
 *
 * Chỉ xác minh QR, không ghi PRESENT.
 */
export async function verifyAttendanceQrService(
  userId: string,
  input: VerifyQrInput,
): Promise<VerifyQrResponse> {

  // 1. Phân tích định dạng QR Token.
  const parsed = parseQrToken(input.qrToken);

  // 2. Lấy ngữ cảnh xác minh từ PostgreSQL.
  // RPC kiểm tra Student thuộc đúng Course.
  const context = await getQrVerificationContext(
    userId,
    parsed.sessionId,
  );

  // 3. Đảm bảo RPC trả về đúng Session.
  if (context.session_id !== parsed.sessionId) {
    throw new QrVerificationError(
      "INVALID_QR_CONTEXT",
    );
  }

  // 4. Đọc thời gian từ backend.
  const databaseTimeMs = Date.parse(
    context.server_time,
  );

  const sessionExpiresMs = Date.parse(
    context.session_expires_at,
  );

  if (
    !Number.isFinite(databaseTimeMs) ||
    !Number.isFinite(sessionExpiresMs)
  ) {
    throw new QrVerificationError(
      "INVALID_QR_CONTEXT",
    );
  }

  // 5. Sử dụng quy tắc thời gian giống Issue QR.
  const nowMs = Math.max(
    databaseTimeMs,
    Date.now(),
  );

  if (nowMs >= sessionExpiresMs) {
    throw new QrVerificationError(
      "QR_EXPIRED",
    );
  }

  // 6. Tính cửa sổ thời gian hiện tại.
  const refreshMs =
    context.qr_refresh_seconds * 1000;

  const currentWindow = Math.floor(
    nowMs / refreshMs,
  );

  // 7. Không cho dùng QR cửa sổ cũ hoặc tương lai.
  if (parsed.window !== currentWindow) {
    throw new QrVerificationError(
      "QR_EXPIRED",
    );
  }

  // 8. Tạo lại chính xác thông điệp đã ký.
  const message = [
    "sa1",
    context.session_id,
    String(parsed.window),
  ].join(":");

  // 9. Xác minh chữ ký HMAC-SHA256.
  const isValid = await verifyQrSignature(
    context.qr_secret,
    message,
    parsed.signature,
  );

  if (!isValid) {
    throw new QrVerificationError(
      "INVALID_QR_SIGNATURE",
    );
  }

  // 10. Kiểm tra thời gian lần cuối.
  // QR có thể hết hạn trong lúc xác minh.
  const verifiedAtMs = Math.max(
    databaseTimeMs,
    Date.now(),
  );

  if (
    verifiedAtMs >= sessionExpiresMs ||
    Math.floor(verifiedAtMs / refreshMs) !==
      parsed.window
  ) {
    throw new QrVerificationError(
      "QR_EXPIRED",
    );
  }

  // 11. Trả về kết quả công khai.
  return {
    verified: true,
    sessionId: context.session_id,
    meetingId: context.meeting_id,
    verifiedAt: new Date(
      verifiedAtMs,
    ).toISOString(),
  };
}
