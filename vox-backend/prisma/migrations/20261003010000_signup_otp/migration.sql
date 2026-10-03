ALTER TABLE "User" ALTER COLUMN "email" DROP NOT NULL;
CREATE UNIQUE INDEX "User_phone_key" ON "User"("phone");
ALTER TABLE "User" ADD CONSTRAINT "User_contact_check" CHECK ("email" IS NOT NULL OR "phone" IS NOT NULL);

CREATE TABLE "SignupCode" (
    "identifier" TEXT NOT NULL,
    "codeHash" TEXT NOT NULL,
    "expiresAt" TIMESTAMP(3) NOT NULL,
    "attempts" INTEGER NOT NULL DEFAULT 0,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "SignupCode_pkey" PRIMARY KEY ("identifier")
);
