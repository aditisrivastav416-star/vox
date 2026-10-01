import { BadRequestException, Body, Controller, Get, Put, Query, Req, UseGuards } from '@nestjs/common';
import { IsString, Matches } from 'class-validator';
import { AuthGuard, Db, date, minor } from './core';
import { categories } from './transactions';

class BudgetInput {
  @Matches(/^\d{4}-(0[1-9]|1[0-2])$/) month!: string;
  @Matches(/^(0|[1-9]\d*)$/) amountMinor!: string;
  @Matches(/^[A-Z]{3}$/) currencyCode!: string;
}
function range(q: any) {
  const now = new Date(), end = q.to ? date(q.to) : now;
  let start: Date;
  if (q.from) start = date(q.from);
  else if (q.period === '7d') start = new Date(now.getTime() - 7 * 86400_000);
  else if (q.period === 'year') start = new Date(Date.UTC(now.getUTCFullYear(), 0, 1));
  else start = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), 1));
  if (start > end) throw new BadRequestException('from must precede to');
  return { start, end };
}
@UseGuards(AuthGuard) @Controller('categories')
export class CategoriesController { @Get() list() { return categories; } }

@UseGuards(AuthGuard) @Controller('budgets')
export class BudgetsController {
  constructor(private db: Db) {}
  @Get() async list(@Req() req: any) {
    const rows = await this.db.budget.findMany({ where: { ownerId: req.user.id } });
    return rows.map(b => ({ ...b, amountMinor: b.amountMinor.toString() }));
  }
  @Put() async upsert(@Req() req: any, @Body() body: BudgetInput) {
    const b = await this.db.budget.upsert({ where: { ownerId_month_currencyCode: { ownerId: req.user.id, month: body.month, currencyCode: body.currencyCode } }, create: { ownerId: req.user.id, month: body.month, currencyCode: body.currencyCode, amountMinor: minor(body.amountMinor) }, update: { amountMinor: minor(body.amountMinor) } });
    return { ...b, amountMinor: b.amountMinor.toString() };
  }
}
@UseGuards(AuthGuard) @Controller('analytics')
export class AnalyticsController {
  constructor(private db: Db) {}
  private async rows(req: any, q: any) {
    const { start, end } = range(q);
    const currencyCode = q.currencyCode || req.user.currencyCode;
    const rows = await this.db.transaction.findMany({ where: { ownerId: req.user.id, deletedAt: null, currencyCode, timestamp: { gte: start, lte: end } }, select: { amountMinor: true, type: true, category: true, timestamp: true } });
    return { rows, start, end, currencyCode };
  }
  @Get('summary') async summary(@Req() req: any, @Query() q: any) {
    const { rows, start, end, currencyCode } = await this.rows(req, q);
    const expenseMinor = rows.filter(r => r.type === 'EXPENSE').reduce((n,r) => n + r.amountMinor, 0n);
    const incomeMinor = rows.filter(r => r.type === 'INCOME').reduce((n,r) => n + r.amountMinor, 0n);
    const month = `${start.getUTCFullYear()}-${String(start.getUTCMonth()+1).padStart(2,'0')}`;
    const budget = await this.db.budget.findUnique({ where: { ownerId_month_currencyCode: { ownerId: req.user.id, month, currencyCode } } });
    return { from: start, to: end, currencyCode, expenseMinor: expenseMinor.toString(), incomeMinor: incomeMinor.toString(), budgetMinor: budget?.amountMinor.toString() || null, remainingMinor: budget ? (budget.amountMinor - expenseMinor).toString() : null, percentageUsed: budget && budget.amountMinor > 0n ? Number(expenseMinor * 10000n / budget.amountMinor) / 100 : null };
  }
  @Get('categories') async byCategory(@Req() req: any, @Query() q: any) {
    const { rows, currencyCode } = await this.rows(req, q);
    const totals = new Map<string,bigint>();
    for (const r of rows) if (r.type === 'EXPENSE') totals.set(r.category || 'OTHER', (totals.get(r.category || 'OTHER') || 0n) + r.amountMinor);
    const total = [...totals.values()].reduce((n,v) => n+v, 0n);
    return { currencyCode, items: [...totals].map(([category, amount]) => ({ category, amountMinor: amount.toString(), percentage: total ? Number(amount * 10000n / total) / 100 : 0 })) };
  }
  @Get('timeline') async timeline(@Req() req: any, @Query() q: any) {
    const { rows, currencyCode } = await this.rows(req, q);
    const totals = new Map<string,bigint>();
    for (const r of rows) if (r.type === 'EXPENSE') { const day = r.timestamp.toISOString().slice(0,10); totals.set(day, (totals.get(day) || 0n) + r.amountMinor); }
    return { currencyCode, items: [...totals].sort().map(([date, amount]) => ({ date, amountMinor: amount.toString() })) };
  }
}
