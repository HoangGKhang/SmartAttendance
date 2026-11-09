
import type {
  ParsedQrToken,
  VerifyQrInput,
} from "./types.ts";


export class ValidationError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "ValidationError";
  }
}


const uuidPattern =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;


// HMAC-SHA256 có 32 byte.
// Base64URL không padding: 43 ký tự.
const signaturePattern =
  /^[A-Za-z0-9_-]{43}$/;


// Window phải là số nguyên dương dạng thập phân.
const windowPattern = /^[1-9][0-9]*$/;


/**
 * Kiểm tra JSON body.
 */
export function validateVerifyQrRequest(
  body: unknown,
): VerifyQrInput {

  if (
    body === null ||
    typeof body !== "object" ||
    Array.isArray(body)
  ) {
    throw new ValidationError(
      "INVALID_REQUEST_BODY",
    );
  }

  const data = body as Record<string, unknown>;

  if (
    typeof data.qrToken !== "string" ||
    data.qrToken.length === 0 ||
    data.qrToken.length > 256
  ) {
    throw new ValidationError(
      "INVALID_QR_TOKEN",
    );
  }

  return {
    qrToken: data.qrToken,
  };
}


/**
 * Phân tích cấu trúc token.
 * KHÔNG xác minh chữ ký tại đây.
 */
export function parseQrToken(
  token: string,
): ParsedQrToken {

  const parts = token.split(".");

  // Token phải có đúng 4 thành phần.
  if (parts.length !== 4) {
    throw new ValidationError(
      "INVALID_QR_TOKEN",
    );
  }

  const [
    version,
    sessionId,
    windowString,
    signature,
  ] = parts;


  // 1. Kiểm tra phiên bản.
  if (version !== "sa1") {
    throw new ValidationError(
      "UNSUPPORTED_QR_VERSION",
    );
  }


  // 2. Kiểm tra Session UUID chuẩn.
  if (
    !uuidPattern.test(sessionId) ||
    sessionId !== sessionId.toLowerCase()
  ) {
    throw new ValidationError(
      "INVALID_QR_TOKEN",
    );
  }


  // 3. Kiểm tra cửa sổ thời gian.
  if (!windowPattern.test(windowString)) {
    throw new ValidationError(
      "INVALID_QR_TOKEN",
    );
  }

  const window = Number(windowString);

  if (!Number.isSafeInteger(window)) {
    throw new ValidationError(
      "INVALID_QR_TOKEN",
    );
  }


  // 4. Kiểm tra định dạng chữ ký.
  if (!signaturePattern.test(signature)) {
    throw new ValidationError(
      "INVALID_QR_TOKEN",
    );
  }


  return {
    version: "sa1",
    sessionId,
    window,
    signature,
  };
}
