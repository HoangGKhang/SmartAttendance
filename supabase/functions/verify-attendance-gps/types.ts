export interface VerifyGpsInput {
  meetingId: string;
  sessionId: string;
  latitude: number;
  longitude: number;
  accuracyMeters: number;
}

export interface VerifyGpsResult {
  verified: boolean;
  attendanceStatus: "PRESENT" | "ABSENT" | null;
}