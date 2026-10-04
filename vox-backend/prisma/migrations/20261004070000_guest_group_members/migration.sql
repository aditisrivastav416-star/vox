-- Existing members keep their user ID as their group participant ID, preserving expenses and balances.
ALTER TABLE "Membership" ADD COLUMN "memberId" UUID;
UPDATE "Membership" SET "memberId" = "userId";
ALTER TABLE "Membership" ALTER COLUMN "memberId" SET NOT NULL;
ALTER TABLE "Membership" ADD COLUMN "guestName" TEXT;

ALTER TABLE "ExpenseShare" RENAME COLUMN "userId" TO "memberId";
ALTER TABLE "ExpenseShare" DROP CONSTRAINT "ExpenseShare_groupId_userId_fkey";
ALTER TABLE "GroupExpense" DROP CONSTRAINT "GroupExpense_groupId_paidById_fkey";
ALTER TABLE "Settlement" DROP CONSTRAINT "Settlement_groupId_fromId_fkey";
ALTER TABLE "Settlement" DROP CONSTRAINT "Settlement_groupId_toId_fkey";
ALTER TABLE "ExpenseShare" DROP CONSTRAINT "ExpenseShare_pkey";
ALTER TABLE "Membership" DROP CONSTRAINT "Membership_pkey";

ALTER TABLE "Membership" ALTER COLUMN "userId" DROP NOT NULL;
ALTER TABLE "Membership" ADD CONSTRAINT "Membership_pkey" PRIMARY KEY ("groupId", "memberId");
CREATE UNIQUE INDEX "Membership_groupId_userId_key" ON "Membership"("groupId", "userId");
ALTER TABLE "ExpenseShare" ADD CONSTRAINT "ExpenseShare_pkey" PRIMARY KEY ("expenseId", "memberId");
ALTER TABLE "ExpenseShare" ADD CONSTRAINT "ExpenseShare_groupId_memberId_fkey" FOREIGN KEY ("groupId", "memberId") REFERENCES "Membership"("groupId", "memberId") ON UPDATE CASCADE;
ALTER TABLE "GroupExpense" ADD CONSTRAINT "GroupExpense_groupId_paidById_fkey" FOREIGN KEY ("groupId", "paidById") REFERENCES "Membership"("groupId", "memberId") ON UPDATE CASCADE;
ALTER TABLE "Settlement" ADD CONSTRAINT "Settlement_groupId_fromId_fkey" FOREIGN KEY ("groupId", "fromId") REFERENCES "Membership"("groupId", "memberId") ON UPDATE CASCADE;
ALTER TABLE "Settlement" ADD CONSTRAINT "Settlement_groupId_toId_fkey" FOREIGN KEY ("groupId", "toId") REFERENCES "Membership"("groupId", "memberId") ON UPDATE CASCADE;
