import type { Metadata } from "next";

import { PrescriptionForm } from "@/components/prescription/PrescriptionForm";

export const metadata: Metadata = {
  title: "Submit your prescription | Flanet Opticals",
  description:
    "Enter your prescription values and upload a scan or photo of your prescription so we can make your lenses to measure.",
};

export default function NewPrescriptionPage() {
  return (
    <main className="mx-auto w-full max-w-3xl flex-1 px-4 py-10 sm:px-6">
      <header className="mb-8 flex flex-col gap-2">
        <h1 className="text-2xl font-semibold tracking-tight">Submit your prescription</h1>
        <p className="text-sm text-zinc-600 dark:text-zinc-400">
          We make every lens to your exact prescription. Copy the values from your
          prescription below, then upload a scan or photo of it so our optician can
          verify them before production.
        </p>
      </header>
      <PrescriptionForm />
    </main>
  );
}
