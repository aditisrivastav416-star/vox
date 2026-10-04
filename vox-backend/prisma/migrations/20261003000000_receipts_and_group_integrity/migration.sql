-- Persist scanned receipt results as drafts; transactions are created only after confirmation.
CREATE TABLE "Receipt" (
    "id" UUID NOT NULL,
    "ownerId" UUID NOT NULL,
    "merchant" TEXT NOT NULL,
    "timestamp" TIMESTAMP(3) NOT NULL,
    "currencyCode" TEXT NOT NULL,
    "taxMinor" BIGINT NOT NULL DEFAULT 0,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "Receipt_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "ReceiptItem" (
    "id" UUID NOT NULL,
    "receiptId" UUID NOT NULL,
    "name" TEXT NOT NULL,
    "amountMinor" BIGINT NOT NULL,
    CONSTRAINT "ReceiptItem_pkey" PRIMARY KEY ("id")
);

CREATE INDEX "Receipt_ownerId_timestamp_idx" ON "Receipt"("ownerId", "timestamp");
ALTER TABLE "Receipt" ADD CONSTRAINT "Receipt_ownerId_fkey" FOREIGN KEY ("ownerId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE "ReceiptItem" ADD CONSTRAINT "ReceiptItem_receiptId_fkey" FOREIGN KEY ("receiptId") REFERENCES "Receipt"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- Existing group data remains valid; reject inconsistent rows rather than silently changing balances.
ALTER TABLE "ExpenseShare" ADD COLUMN "groupId" UUID;
UPDATE "ExpenseShare" s SET "groupId" = e."groupId" FROM "GroupExpense" e WHERE e."id" = s."expenseId";
ALTER TABLE "ExpenseShare" ALTER COLUMN "groupId" SET NOT NULL;
CREATE UNIQUE INDEX "GroupExpense_id_groupId_key" ON "GroupExpense"("id", "groupId");
ALTER TABLE "ExpenseShare" DROP CONSTRAINT "ExpenseShare_expenseId_fkey";
ALTER TABLE "ExpenseShare" ADD CONSTRAINT "ExpenseShare_expenseId_groupId_fkey" FOREIGN KEY ("expenseId", "groupId") REFERENCES "GroupExpense"("id", "groupId") ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE "ExpenseShare" ADD CONSTRAINT "ExpenseShare_groupId_userId_fkey" FOREIGN KEY ("groupId", "userId") REFERENCES "Membership"("groupId", "userId") ON UPDATE CASCADE;
ALTER TABLE "GroupExpense" ADD CONSTRAINT "GroupExpense_groupId_paidById_fkey" FOREIGN KEY ("groupId", "paidById") REFERENCES "Membership"("groupId", "userId") ON UPDATE CASCADE;
ALTER TABLE "Settlement" ADD CONSTRAINT "Settlement_groupId_fromId_fkey" FOREIGN KEY ("groupId", "fromId") REFERENCES "Membership"("groupId", "userId") ON UPDATE CASCADE;
ALTER TABLE "Settlement" ADD CONSTRAINT "Settlement_groupId_toId_fkey" FOREIGN KEY ("groupId", "toId") REFERENCES "Membership"("groupId", "userId") ON UPDATE CASCADE;
ALTER TABLE "Transaction" ADD CONSTRAINT "Transaction_groupId_fkey" FOREIGN KEY ("groupId") REFERENCES "Group"("id") ON DELETE SET NULL ON UPDATE CASCADE;
