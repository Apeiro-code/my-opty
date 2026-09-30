import type {
  ApiErrorBody,
  FieldErrors,
  Prescription,
  PrescriptionPayload,
} from "@/lib/prescription";

/**
 * Talks to the Spring Boot API through the rewrite in `next.config.ts`, so the
 * browser stays same-origin and the backend needs no CORS configuration.
 */
const API_BASE = "/api";

interface SuccessEnvelope<T> {
  success: true;
  data: T;
}

interface ErrorEnvelope {
  success: false;
  error: ApiErrorBody;
}

/** An API failure carrying the field errors the backend reported, if any. */
export class ApiRequestError extends Error {
  readonly code: string;
  readonly fieldErrors: FieldErrors;
  readonly status: number;

  constructor(status: number, body: ApiErrorBody) {
    super(body.message);
    this.name = "ApiRequestError";
    this.code = body.code;
    this.fieldErrors = body.fieldErrors ?? {};
    this.status = status;
  }
}

/** A network or server failure with no usable error envelope. */
export class NetworkError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "NetworkError";
  }
}

function isErrorEnvelope(body: unknown): body is ErrorEnvelope {
  return (
    typeof body === "object" &&
    body !== null &&
    (body as ErrorEnvelope).success === false &&
    typeof (body as ErrorEnvelope).error === "object"
  );
}

/**
 * Submits the prescription form. The request is multipart: the optical values
 * travel as a JSON part and the document as a file part, which is what
 * `POST /api/prescriptions` expects.
 */
export async function submitPrescription(
  payload: PrescriptionPayload,
  document: File,
): Promise<Prescription> {
  const body = new FormData();
  body.append(
    "prescription",
    new Blob([JSON.stringify(payload)], { type: "application/json" }),
  );
  body.append("document", document, document.name);

  let response: Response;
  try {
    response = await fetch(`${API_BASE}/prescriptions`, {
      method: "POST",
      body,
    });
  } catch {
    throw new NetworkError(
      "We could not reach the shop right now. Please check your connection and try again.",
    );
  }

  const text = await response.text();
  const parsed = text === "" ? undefined : safeParse(text);

  if (!response.ok) {
    if (isErrorEnvelope(parsed)) {
      throw new ApiRequestError(response.status, parsed.error);
    }
    throw new NetworkError("Something went wrong while submitting your prescription.");
  }

  if (!isSuccessEnvelope(parsed)) {
    throw new NetworkError("The shop sent an unexpected response. Please try again.");
  }

  return parsed.data;
}

function isSuccessEnvelope(body: unknown): body is SuccessEnvelope<Prescription> {
  return (
    typeof body === "object" &&
    body !== null &&
    (body as SuccessEnvelope<Prescription>).success === true
  );
}

function safeParse(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    return undefined;
  }
}
