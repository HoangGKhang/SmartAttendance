import type {
  VerifyGpsInput,
  VerifyGpsResult,
} from "./types.ts";

import {
  verifyGpsRepository,
} from "./repository.ts";

export async function verifyGpsService(
  userId: string,
  input: VerifyGpsInput,
): Promise<VerifyGpsResult> {
  return await verifyGpsRepository(
    userId,
    input,
  );
}