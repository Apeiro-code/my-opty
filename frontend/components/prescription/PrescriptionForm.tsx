"use client";

import { useRef, useState, type DragEvent, type FormEvent } from "react";

import { ApiRequestError, NetworkError, submitPrescription } from "@/lib/api";
import {
  ACCEPTED_DOCUMENT_TYPES,
  EYE_LABELS,
  FIELD_LABELS,
  MAX_DOCUMENT_BYTES,
  type EyeField,
  type EyeSide,
  type FieldErrors,
  type Prescription,
  type PrescriptionDraft,
  emptyDraft,
  formatBytes,
  toPayload,
  validateDocument,
  validateDraft,
} from "@/lib/prescription";

const EYE_SIDES: EyeSide[] = ["rightEye", "leftEye"];

const EYE_FIELDS: EyeField[] = ["sphere", "cylinder", "axis", "addPower"];

const FIELD_HINTS: Record<EyeField, string> = {
  sphere: "Always on your prescription, for example -2.25",
  cylinder: "0.00 if you have no astigmatism",
  axis: "Only with a cylinder, 1 to 180",
  addPower: "For reading or progressive lenses, 0.25 to 4.00",
};

const FIELD_INPUT_MODE: Record<EyeField, "decimal" | "numeric"> = {
  sphere: "decimal",
  cylinder: "decimal",
  axis: "numeric",
  addPower: "decimal",
};

export function PrescriptionForm() {
  const [draft, setDraft] = useState<PrescriptionDraft>(emptyDraft);
  const [file, setFile] = useState<File | null>(null);
  const [errors, setErrors] = useState<FieldErrors>({});
  const [documentError, setDocumentError] = useState<string | undefined>();
  const [formError, setFormError] = useState<string | undefined>();
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState<Prescription | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  function updateEye(side: EyeSide, field: EyeField, value: string) {
    setDraft((current) => ({
      ...current,
      [side]: { ...current[side], [field]: value },
    }));
    setFormError(undefined);
  }

  function selectDocument(file: File | null) {
    setFile(file);
    setDocumentError(validateDocument(file));
    setFormError(undefined);
  }

  function onDrop(event: DragEvent<HTMLLabelElement>) {
    event.preventDefault();
    selectDocument(event.dataTransfer.files[0] ?? null);
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitted(null);
    setFormError(undefined);

    const draftErrors = validateDraft(draft);
    const nextDocumentError = validateDocument(file);
    setErrors(draftErrors);
    setDocumentError(nextDocumentError);

    if (Object.keys(draftErrors).length > 0 || nextDocumentError || !file) {
      return;
    }

    setSubmitting(true);
    try {
      const prescription = await submitPrescription(toPayload(draft), file);
      setSubmitted(prescription);
      setDraft(emptyDraft());
      setFile(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
    } catch (error) {
      if (error instanceof ApiRequestError) {
        setErrors(error.fieldErrors);
        setFormError(error.message);
      } else if (error instanceof NetworkError) {
        setFormError(error.message);
      } else {
        setFormError("Something went wrong while submitting your prescription.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  if (submitted) {
    return (
      <div className="rounded-lg border border-green-300 bg-green-50 p-6 text-zinc-900 dark:border-green-800 dark:bg-green-950 dark:text-zinc-100">
        <h2 className="text-lg font-semibold">Prescription received</h2>
        <p className="mt-2 text-sm">
          We have your prescription (reference #{submitted.id}) and the document you
          uploaded. Our optician will verify it and get back to you. You can track it
          under prescription {submitted.id}.
        </p>
        <button
          type="button"
          className="mt-4 rounded-md border border-zinc-300 px-3 py-2 text-sm font-medium hover:bg-zinc-100 dark:border-zinc-700 dark:hover:bg-zinc-800"
          onClick={() => setSubmitted(null)}
        >
          Submit another prescription
        </button>
      </div>
    );
  }

  return (
    <form onSubmit={onSubmit} noValidate className="flex flex-col gap-8">
      <fieldset className="flex flex-col gap-3">
        <legend className="text-base font-semibold">Lens type</legend>
        <label className="flex items-start gap-3 text-sm">
          <input
            type="checkbox"
            checked={draft.progressive}
            onChange={(event) => {
              setDraft((current) => ({ ...current, progressive: event.target.checked }));
              setFormError(undefined);
            }}
            className="mt-1 size-4"
          />
          <span>
            I need progressive lenses
            <span className="block text-zinc-600 dark:text-zinc-400">
              Progressive lenses need a near addition on both eyes, so you will be
              asked for an add power below.
            </span>
          </span>
        </label>
        <div className="flex flex-col gap-1.5">
          <label htmlFor="issuedDate" className="text-sm font-medium">
            Date on your prescription
          </label>
          <input
            id="issuedDate"
            type="date"
            value={draft.issuedDate}
            max={new Date().toISOString().slice(0, 10)}
            onChange={(event) =>
              setDraft((current) => ({ ...current, issuedDate: event.target.value }))
            }
            className="w-full max-w-xs rounded-md border border-zinc-300 bg-transparent px-3 py-2 text-sm dark:border-zinc-700"
          />
        </div>
      </fieldset>

      {EYE_SIDES.map((side) => (
        <fieldset key={side} className="flex flex-col gap-3">
          <legend className="text-base font-semibold">{EYE_LABELS[side]}</legend>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            {EYE_FIELDS.map((field) => {
              const fieldKey = `${side}.${field}`;
              const error = errors[fieldKey];
              const fieldId = fieldKey.replace(".", "-");
              return (
                <div key={fieldKey} className="flex flex-col gap-1.5">
                  <label htmlFor={fieldId} className="text-sm font-medium">
                    {FIELD_LABELS[field]}
                  </label>
                  <input
                    id={fieldId}
                    name={fieldKey}
                    type="text"
                    inputMode={FIELD_INPUT_MODE[field]}
                    autoComplete="off"
                    placeholder={field === "axis" ? "0" : "0.00"}
                    value={draft[side][field]}
                    onChange={(event) => updateEye(side, field, event.target.value)}
                    aria-invalid={error ? true : undefined}
                    aria-describedby={`${fieldId}-hint${error ? ` ${fieldId}-error` : ""}`}
                    className={`w-full rounded-md border bg-transparent px-3 py-2 text-sm ${
                      error
                        ? "border-red-500 dark:border-red-400"
                        : "border-zinc-300 dark:border-zinc-700"
                    }`}
                  />
                  <p id={`${fieldId}-hint`} className="text-xs text-zinc-600 dark:text-zinc-400">
                    {FIELD_HINTS[field]}
                  </p>
                  {error ? (
                    <p id={`${fieldId}-error`} role="alert" className="text-xs text-red-600 dark:text-red-400">
                      {error}
                    </p>
                  ) : null}
                </div>
              );
            })}
          </div>
        </fieldset>
      ))}

      <div className="flex flex-col gap-1.5">
        <label htmlFor="notes" className="text-sm font-medium">
          Anything we should know? (optional)
        </label>
        <textarea
          id="notes"
          name="notes"
          rows={3}
          maxLength={500}
          value={draft.notes}
          onChange={(event) =>
            setDraft((current) => ({ ...current, notes: event.target.value }))
          }
          className="w-full rounded-md border border-zinc-300 bg-transparent px-3 py-2 text-sm dark:border-zinc-700"
        />
      </div>

      <fieldset className="flex flex-col gap-2">
        <legend className="text-base font-semibold">Prescription document</legend>
        <p className="text-sm text-zinc-600 dark:text-zinc-400">
          Upload a scan or a clear photo of your prescription. Our optician checks it
          against the values above before your lenses are made. PDF, JPEG, PNG, WebP or
          HEIC, up to {formatBytes(MAX_DOCUMENT_BYTES)}.
        </p>
        <label
          htmlFor="document"
          onDragOver={(event) => event.preventDefault()}
          onDrop={onDrop}
          className={`flex cursor-pointer flex-col items-center gap-1 rounded-lg border border-dashed px-4 py-8 text-center text-sm ${
            documentError
              ? "border-red-500 dark:border-red-400"
              : "border-zinc-400 dark:border-zinc-600"
          }`}
        >
          <span className="font-medium">
            {file ? file.name : "Choose a file or drag it here"}
          </span>
          {file ? (
            <span className="text-zinc-600 dark:text-zinc-400">
              {formatBytes(file.size)}
            </span>
          ) : (
            <span className="text-zinc-600 dark:text-zinc-400">No file chosen yet</span>
          )}
          <input
            ref={fileInputRef}
            id="document"
            name="document"
            type="file"
            accept={ACCEPTED_DOCUMENT_TYPES.join(",")}
            className="sr-only"
            onChange={(event) => selectDocument(event.target.files?.[0] ?? null)}
          />
        </label>
        {documentError ? (
          <p role="alert" className="text-xs text-red-600 dark:text-red-400">
            {documentError}
          </p>
        ) : null}
      </fieldset>

      {formError ? (
        <p role="alert" className="rounded-md bg-red-50 p-3 text-sm text-red-700 dark:bg-red-950 dark:text-red-300">
          {formError}
        </p>
      ) : null}

      <button
        type="submit"
        disabled={submitting}
        className="w-fit rounded-md bg-zinc-900 px-4 py-2 text-sm font-medium text-zinc-50 disabled:opacity-60 dark:bg-zinc-100 dark:text-zinc-900"
      >
        {submitting ? "Submitting…" : "Submit prescription"}
      </button>
    </form>
  );
}
