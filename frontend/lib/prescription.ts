/**
 * Prescription form contract, shared by the form UI and the API client.
 *
 * The validation ranges mirror the backend (`EyePrescription`,
 * `V3__order_create_prescription_table.sql`) so the customer gets feedback before
 * a round trip. The backend re-checks everything: this is convenience, not the
 * authority.
 */

export type EyeField = "sphere" | "cylinder" | "axis" | "addPower";

export type EyeSide = "rightEye" | "leftEye";

export type EyeValues = Record<EyeField, string>;

export interface PrescriptionDraft {
  customerId?: number;
  progressive: boolean;
  issuedDate: string;
  rightEye: EyeValues;
  leftEye: EyeValues;
  notes: string;
}

export interface PrescriptionEyePayload {
  sphere: number;
  cylinder?: number;
  axis?: number;
  addPower?: number;
}

export interface PrescriptionPayload {
  customerId?: number;
  progressive: boolean;
  issuedDate?: string;
  rightEye: PrescriptionEyePayload;
  leftEye: PrescriptionEyePayload;
  notes?: string;
}

export interface Prescription {
  id: number;
  customerId?: number;
  status: "PENDING_REVIEW" | "VERIFIED" | "REJECTED";
  progressive: boolean;
  issuedDate?: string;
  rightEye: PrescriptionEyePayload;
  leftEye: PrescriptionEyePayload;
  notes?: string;
  rejectionReason?: string;
  document?: {
    filename: string;
    contentType: string;
    sizeBytes: number;
    uploadedAt: string;
    url: string;
  };
  createdAt: string;
  updatedAt: string;
}

export interface ApiErrorBody {
  code: string;
  message: string;
  fieldErrors?: Record<string, string>;
}

export type FieldErrors = Record<string, string>;

export const SPHERE_RANGE = { min: -30, max: 30 } as const;
export const CYLINDER_RANGE = { min: -10, max: 10 } as const;
export const AXIS_RANGE = { min: 0, max: 180 } as const;
export const ADD_RANGE = { min: 0.25, max: 4 } as const;

export const MAX_DOCUMENT_BYTES = 10 * 1024 * 1024;

export const ACCEPTED_DOCUMENT_TYPES = [
  "image/jpeg",
  "image/png",
  "image/webp",
  "image/heic",
  "image/heif",
  "application/pdf",
];

export const EYE_LABELS: Record<EyeSide, string> = {
  rightEye: "Right eye (OD)",
  leftEye: "Left eye (OS)",
};

export const FIELD_LABELS: Record<EyeField, string> = {
  sphere: "Sphere (SPH)",
  cylinder: "Cylinder (CYL)",
  axis: "Axis",
  addPower: "Add power (ADD)",
};

export const EMPTY_EYE: EyeValues = {
  sphere: "",
  cylinder: "",
  axis: "",
  addPower: "",
};

export function emptyDraft(): PrescriptionDraft {
  return {
    progressive: false,
    issuedDate: new Date().toISOString().slice(0, 10),
    rightEye: { ...EMPTY_EYE },
    leftEye: { ...EMPTY_EYE },
    notes: "",
  };
}

/**
 * Parses a typed optical value. Blank input is absent rather than zero: an empty
 * cylinder means "no astigmatism" and an empty addition means "no reading add".
 */
export function parseOpticalValue(raw: string): number | undefined {
  const trimmed = raw.trim();
  if (trimmed === "") {
    return undefined;
  }
  const parsed = Number(trimmed);
  return Number.isFinite(parsed) ? parsed : undefined;
}

export function formatOpticalValue(value: number | undefined): string {
  return value === undefined ? "" : value.toFixed(2);
}

export function formatBytes(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  if (bytes < 1024 * 1024) {
    return `${Math.round(bytes / 1024)} KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function isQuarterStep(value: number): boolean {
  return Math.abs(value * 4 - Math.round(value * 4)) < 1e-9;
}

function inRange(value: number, min: number, max: number): boolean {
  return value >= min && value <= max;
}

/**
 * Mirrors the cross-field rules in `PrescriptionServiceImpl`: quarter dioptre
 * steps, an axis only with a cylinder, and an addition on both eyes when the
 * lens is progressive.
 */
export function validateEye(
  eye: EyeValues,
  side: EyeSide,
  progressive: boolean,
): FieldErrors {
  const errors: FieldErrors = {};
  const key = (field: EyeField) => `${side}.${field}`;

  const sphere = parseOpticalValue(eye.sphere);
  const cylinder = parseOpticalValue(eye.cylinder);
  const addPower = parseOpticalValue(eye.addPower);
  const axis = eye.axis.trim() === "" ? undefined : Number(eye.axis);

  if (sphere === undefined) {
    errors[key("sphere")] = "Enter the sphere value from your prescription";
  } else if (!inRange(sphere, SPHERE_RANGE.min, SPHERE_RANGE.max)) {
    errors[key("sphere")] = `Must be between ${SPHERE_RANGE.min.toFixed(2)} and +${SPHERE_RANGE.max.toFixed(2)}`;
  } else if (!isQuarterStep(sphere)) {
    errors[key("sphere")] = "Must be in 0.25 dioptre steps";
  }

  if (cylinder !== undefined) {
    if (!inRange(cylinder, CYLINDER_RANGE.min, CYLINDER_RANGE.max)) {
      errors[key("cylinder")] = `Must be between ${CYLINDER_RANGE.min.toFixed(2)} and +${CYLINDER_RANGE.max.toFixed(2)}`;
    } else if (!isQuarterStep(cylinder)) {
      errors[key("cylinder")] = "Must be in 0.25 dioptre steps";
    }
  }

  if (addPower !== undefined && addPower !== 0) {
    if (!inRange(addPower, ADD_RANGE.min, ADD_RANGE.max)) {
      errors[key("addPower")] = `Must be between ${ADD_RANGE.min.toFixed(2)} and ${ADD_RANGE.max.toFixed(2)}`;
    } else if (!isQuarterStep(addPower)) {
      errors[key("addPower")] = "Must be in 0.25 dioptre steps";
    }
  } else if (progressive) {
    errors[key("addPower")] = "Required for a progressive lens";
  }

  const hasCylinder = cylinder !== undefined && cylinder !== 0;
  if (axis !== undefined) {
    if (!Number.isInteger(axis) || !inRange(axis, AXIS_RANGE.min, AXIS_RANGE.max)) {
      errors[key("axis")] = `Must be a whole number between ${AXIS_RANGE.min} and ${AXIS_RANGE.max}`;
    } else if (axis !== 0 && !hasCylinder) {
      errors[key("axis")] = "Enter 0 or leave blank when the cylinder is 0.00";
    }
  } else if (hasCylinder) {
    errors[key("axis")] = "Required when the cylinder is not 0.00";
  }

  return errors;
}

export function validateDraft(draft: PrescriptionDraft): FieldErrors {
  return {
    ...validateEye(draft.rightEye, "rightEye", draft.progressive),
    ...validateEye(draft.leftEye, "leftEye", draft.progressive),
  };
}

export function validateDocument(file: File | null): string | undefined {
  if (!file) {
    return "Upload a scan or photo of your prescription so we can verify it";
  }
  if (file.size === 0) {
    return "That file is empty. Please choose a scan or photo of your prescription";
  }
  if (file.size > MAX_DOCUMENT_BYTES) {
    return `The document must be smaller than ${MAX_DOCUMENT_BYTES / (1024 * 1024)} MB`;
  }
  if (file.type && !ACCEPTED_DOCUMENT_TYPES.includes(file.type)) {
    return "The document must be a PDF, JPEG, PNG, WebP or HEIC file";
  }
  return undefined;
}

/**
 * Drops blank values so the payload carries `undefined` rather than `0`, and
 * keeps the same shape the backend validates.
 */
export function toPayload(draft: PrescriptionDraft): PrescriptionPayload {
  const eye = (values: EyeValues): PrescriptionEyePayload => {
    const payload: PrescriptionEyePayload = {
      sphere: parseOpticalValue(values.sphere) ?? 0,
    };
    const cylinder = parseOpticalValue(values.cylinder);
    const addPower = parseOpticalValue(values.addPower);
    const axis = values.axis.trim() === "" ? undefined : Number(values.axis);

    if (cylinder !== undefined) {
      payload.cylinder = cylinder;
    }
    if (addPower !== undefined && addPower !== 0) {
      payload.addPower = addPower;
    }
    if (axis !== undefined && axis !== 0) {
      payload.axis = axis;
    }
    return payload;
  };

  return {
    customerId: draft.customerId,
    progressive: draft.progressive,
    issuedDate: draft.issuedDate || undefined,
    rightEye: eye(draft.rightEye),
    leftEye: eye(draft.leftEye),
    notes: draft.notes.trim() || undefined,
  };
}
