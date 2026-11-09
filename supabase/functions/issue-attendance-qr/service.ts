
import type {
  IssueQrInput,
  IssueQrResponse,
} from "./types.ts";

import {
  getQrIssuanceContext,
} from "./repository.ts";


const QR_VERSION = "sa1";

export type QrServiceErrorCode =
  | "QR_WINDOW_EXPIRED"
  | "INVALID_QR_CONTEXT";

export class QrServiceError extends Error {
  constructor(
    public readonly code: QrServiceErrorCode,
  ) {
    super(code);
    this.name = "QrServiceError";
  }
}


/**
 * Chuyển chuỗi hex 64 ký tự thành 32 byte.
 *
 * Ví dụ "a1b2" -> [0xa1, 0xb2].
 */
function hexToBytes(hex: string): Uint8Array {
  if (!/^[0-9a-f]{64}$/.test(hex)) {
    throw new QrServiceError(
      "INVALID_QR_CONTEXT",
    );
  }

  const bytes = new Uint8Array(32);

  for (let i = 0; i < 32; i++) {
    const pair = hex.slice(i * 2, i * 2 + 2);

    bytes[i] = Number.parseInt(pair, 16);
  }

  return bytes;
}


/**
 * Chuyển dữ liệu chữ ký sang Base64URL.
 */
function toBase64Url(
  buffer: ArrayBuffer,
): string {
  const bytes = new Uint8Array(buffer);

  let binary = "";

  for (const byte of bytes) {
    binary += String.fromCharCode(byte);
  }

  return btoa(binary)
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");
}


/**
 * Tạo chữ ký HMAC-SHA256.
 */
async function signQrMessage(
  secretHex: string,
  message: string,
): Promise<string> {
  const secretBytes = hexToBytes(secretHex);

  const key = await crypto.subtle.importKey(
    "raw",
    secretBytes,
    {
      name: "HMAC",
      hash: "SHA-256",
    },
    false,
    ["sign"],
  );

  const messageBytes =
    new TextEncoder().encode(message);

  const signature = await crypto.subtle.sign(
    "HMAC",
    key,
    messageBytes,
  );

  return toBase64Url(signature);
}


/**
 * Nghiệp vụ cấp QR động cho giảng viên.
 *
 * userId: lấy từ JWT đã xác thực.
 * input: đã được validate.
 */
export async function issueAttendanceQrService(
  userId: string,
  input: IssueQrInput,
): Promise<IssueQrResponse> {

  // 1. Lấy dữ liệu nội bộ từ PostgreSQL RPC.
  const context = await getQrIssuanceContext(
    userId,
    input.sessionId,
  );

  // 2. Đọc thời gian server và hạn Session.
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
    throw new QrServiceError(
      "INVALID_QR_CONTEXT",
    );
  }


  const refreshMs =
    context.qr_refresh_seconds * 1000;

  const MIN_REMAINING_MS = 1000;

  // Thử tạo mã tối đa 3 lần nếu gặp ranh giới cửa sổ.
  for (let attempt = 0; attempt < 3; attempt++) {

    const nowMs = Math.max(
      databaseTimeMs,
      Date.now(),
    );

    // Session thực sự hết hạn.
    if (nowMs >= sessionExpiresMs) {
      throw new QrServiceError(
        "QR_WINDOW_EXPIRED",
      );
    }

    const window = Math.floor(nowMs / refreshMs);

    const windowEndMs =
      (window + 1) * refreshMs;

    const qrExpiresMs = Math.min(
      windowEndMs,
      sessionExpiresMs,
    );

    const message = [
      QR_VERSION,
      context.session_id,
      String(window),
    ].join(":");

    const signature = await signQrMessage(
      context.qr_secret,
      message,
    );

    // Kiểm tra lại sau khi đã ký.
    const remainingMs =
      qrExpiresMs - Math.max(
        databaseTimeMs,
        Date.now(),
      );

    if (remainingMs > MIN_REMAINING_MS) {
      return {
        qrToken: [
          QR_VERSION,
          context.session_id,
          String(window),
          signature,
        ].join("."),

        qrExpiresAt:
          new Date(qrExpiresMs).toISOString(),
      };
    }

    // Nếu chính Session sắp hết hạn,
    // không được cấp mã vượt thời hạn Session.
    if (qrExpiresMs === sessionExpiresMs) {
      throw new QrServiceError(
        "QR_WINDOW_EXPIRED",
      );
    }

    // Cửa sổ QR sắp hết hạn:
    // chờ sang cửa sổ tiếp theo rồi tạo mã mới.
    const waitMs = Math.max(
      50,
      windowEndMs - Date.now() + 75,
    );

    await new Promise<void>((resolve) => {
      setTimeout(resolve, waitMs);
    });
  }

  throw new QrServiceError(
    "QR_WINDOW_EXPIRED",
  );

}
