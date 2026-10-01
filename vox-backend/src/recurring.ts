import { BadRequestException, Body, Controller, Get, NotFoundException, Param, ParseUUIDPipe, Post, Put, Req, UseGuards } from '@nestjs/common';
import { IsBoolean, IsDateString, IsIn, IsInt, IsOptional, IsString, IsUUID, Length, Matches, Min } from 'class-validator';
import { AuthGuard, Db, date, minor } from './core';

class RecurringInput {
  @IsUUID() id!: string;
  @IsString() @Length(1,100) name!: string;
  @IsString() category!: string;
  @IsOptional() @Matches(/^(0|[1-9]\d*)$/) amountMinor?: string;
  @Matches(/^[A-Z]{3}$/) currencyCode!: string;
  @IsIn(['DAILY','WEEKLY','MONTHLY','QUARTERLY','YEARLY','CUSTOM']) frequency!: string;
  @IsOptional() @IsInt() @Min(1) customIntervalDays?: number;
  @IsDateString() nextDueDate!: string;
  @IsOptional() @IsBoolean() remindDaysBefore?: boolean;
  @IsOptional() @IsBoolean() remindOnDueDate?: boolean;
  @IsOptional() @IsBoolean() active?: boolean;
}
const output = (r: any) => ({ ...r, amountMinor: r.amountMinor?.toString() || null, amount: r.amountMinor === null ? null : { amountMinor: r.amountMinor.toString(), currencyCode: r.currencyCode }, nextDueDate: r.nextDueDate.toISOString().slice(0,10) });
export function next(d: Date, frequency: string, custom?: number | null) {
  const n = new Date(d);
  if (frequency === 'DAILY' || frequency === 'CUSTOM') n.setUTCDate(n.getUTCDate() + (frequency === 'DAILY' ? 1 : custom!));
  else if (frequency === 'WEEKLY') n.setUTCDate(n.getUTCDate() + 7);
  else {
    const months = { MONTHLY: 1, QUARTERLY: 3, YEARLY: 12 }[frequency] || 1;
    const day = n.getUTCDate();
    n.setUTCDate(1);
    n.setUTCMonth(n.getUTCMonth() + months);
    const last = new Date(Date.UTC(n.getUTCFullYear(), n.getUTCMonth() + 1, 0)).getUTCDate();
    n.setUTCDate(Math.min(day, last));
  }
  return n;
}
@UseGuards(AuthGuard) @Controller('recurring-payments')
export class RecurringController {
  constructor(private db: Db) {}
  @Get() async list(@Req() req: any) { return (await this.db.recurringPayment.findMany({ where: { ownerId: req.user.id }, orderBy: { nextDueDate: 'asc' } })).map(output); }
  @Post() async create(@Req() req: any, @Body() body: RecurringInput) {
    if (body.frequency === 'CUSTOM' && !body.customIntervalDays) throw new BadRequestException('customIntervalDays required');
    const existing = await this.db.recurringPayment.findUnique({ where: { id: body.id } });
    if (existing) { if (existing.ownerId !== req.user.id) throw new NotFoundException(); return output(existing); }
    return output(await this.db.recurringPayment.create({ data: { ...body, ownerId: req.user.id, amountMinor: body.amountMinor ? minor(body.amountMinor) : null, nextDueDate: date(body.nextDueDate) } }));
  }
  @Put(':id') async update(@Req() req: any, @Param('id', ParseUUIDPipe) id: string, @Body() body: RecurringInput) {
    if (id !== body.id) throw new BadRequestException('ID mismatch');
    const current = await this.db.recurringPayment.findFirst({ where: { id, ownerId: req.user.id } });
    if (!current) throw new NotFoundException();
    return output(await this.db.recurringPayment.update({ where: { id }, data: { name: body.name, category: body.category, amountMinor: body.amountMinor ? minor(body.amountMinor) : null, currencyCode: body.currencyCode, frequency: body.frequency, customIntervalDays: body.customIntervalDays, nextDueDate: date(body.nextDueDate), remindDaysBefore: body.remindDaysBefore, remindOnDueDate: body.remindOnDueDate, active: body.active } }));
  }
  @Get(':id/occurrences') async occurrences(@Req() req: any, @Param('id', ParseUUIDPipe) id: string) {
    if (!await this.db.recurringPayment.findFirst({ where: { id, ownerId: req.user.id } })) throw new NotFoundException();
    return (await this.db.recurringOccurrence.findMany({ where: { paymentId: id }, orderBy: { dueDate: 'desc' } })).map(o => ({ ...o, dueDate: o.dueDate.toISOString().slice(0,10), amountMinor: o.amountMinor?.toString() || null }));
  }
}

export async function generateDue(db: Db, today = new Date()) {
  const rules = await db.recurringPayment.findMany({ where: { active: true, nextDueDate: { lte: today } } });
  for (const r of rules) await db.$transaction(async tx => {
    const claimed = await tx.recurringPayment.updateMany({ where: { id: r.id, active: true, nextDueDate: r.nextDueDate }, data: { nextDueDate: next(r.nextDueDate, r.frequency, r.customIntervalDays) } });
    if (!claimed.count) return;
    await tx.recurringOccurrence.upsert({ where: { paymentId_dueDate: { paymentId: r.id, dueDate: r.nextDueDate } }, create: { paymentId: r.id, dueDate: r.nextDueDate, amountMinor: r.amountMinor }, update: {} });
  });
}
