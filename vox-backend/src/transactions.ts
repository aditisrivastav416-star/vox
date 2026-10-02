import { BadRequestException, Body, ConflictException, Controller, Delete, Get, NotFoundException, Param, ParseUUIDPipe, Post, Put, Query, Req, UseGuards } from '@nestjs/common';
import { IsDateString, IsIn, IsInt, IsOptional, IsString, IsUUID, Length, Matches } from 'class-validator';
import { Prisma } from '@prisma/client';
import { AuthGuard, Db, date, minor, money } from './core';

export const categories = ['FOOD','GROCERIES','SHOPPING','TRAVEL','TRANSPORT','BILLS','RENT','ENTERTAINMENT','EDUCATION','HEALTH','OTHER','SALARY','GIFT','OTHER_INCOME'];
const types = ['EXPENSE','INCOME'];
const methods = ['UPI','CARD','CASH','BANK'];
const sources = ['MANUAL','VOICE','SMS','RECEIPT','GROUP'];

class TransactionInput {
  @IsUUID() id!: string;
  @IsIn(types) type!: string;
  @IsString() @Matches(/^(0|[1-9]\d*)$/) amountMinor!: string;
  @Matches(/^[A-Z]{3}$/) currencyCode!: string;
  @IsString() @Length(1, 200) title!: string;
  @IsOptional() @IsIn(categories) category?: string | null;
  @IsOptional() @IsIn(methods) paymentMethod?: string;
  @IsDateString() timestamp!: string;
  @IsOptional() @IsIn(sources) source?: string;
  @IsOptional() @IsUUID() groupId?: string | null;
  @IsOptional() @IsString() @Length(0, 2000) notes?: string | null;
  @IsOptional() @IsInt() version?: number;
}
function output(t: any) { return { ...t, amount: money(t.amountMinor, t.currencyCode), amountMinor: t.amountMinor.toString() }; }

@UseGuards(AuthGuard) @Controller('transactions')
export class TransactionsController {
  constructor(private db: Db) {}
  @Get('sync') async sync(@Req() req: any, @Query('cursor') cursor?: string) {
    const [stamp, id] = cursor ? cursor.split('|') : [];
    const updatedAt = stamp ? date(stamp) : new Date(0);
    const items = await this.db.transaction.findMany({ where: { ownerId: req.user.id, OR: [{ updatedAt: { gt: updatedAt } }, ...(id ? [{ updatedAt, id: { gt: id } }] : [])] }, orderBy: [{ updatedAt: 'asc' }, { id: 'asc' }], take: 500 });
    const last = items.at(-1);
    return { items: items.map(output), nextCursor: last ? `${last.updatedAt.toISOString()}|${last.id}` : cursor || null, hasMore: items.length === 500 };
  }
  @Get() async list(@Req() req: any, @Query() q: any) {
    const take = Math.min(Math.max(Number(q.limit) || 50, 1), 100);
    const where: Prisma.TransactionWhereInput = { ownerId: req.user.id, deletedAt: null };
    if (q.type) { if (!types.includes(q.type)) throw new BadRequestException('Invalid type'); where.type = q.type; }
    if (q.category) { if (!categories.includes(q.category)) throw new BadRequestException('Invalid category'); where.category = q.category; }
    if (q.search) where.title = { contains: String(q.search), mode: 'insensitive' };
    if (q.from || q.to) where.timestamp = { ...(q.from && { gte: date(q.from) }), ...(q.to && { lte: date(q.to) }) };
    if (q.cursor) { const [stamp, id] = String(q.cursor).split('|'); where.OR = [{ timestamp: { lt: date(stamp) } }, { timestamp: date(stamp), id: { lt: id } }]; }
    const items = await this.db.transaction.findMany({ where, orderBy: [{ timestamp: 'desc' }, { id: 'desc' }], take: take + 1 });
    const page = items.slice(0, take), last = page.at(-1);
    return { items: page.map(output), nextCursor: items.length > take && last ? `${last.timestamp.toISOString()}|${last.id}` : null };
  }
  @Get(':id') async one(@Req() req: any, @Param('id', ParseUUIDPipe) id: string) {
    const t = await this.db.transaction.findFirst({ where: { id, ownerId: req.user.id, deletedAt: null } });
    if (!t) throw new NotFoundException();
    return output(t);
  }
  @Post() async create(@Req() req: any, @Body() body: TransactionInput) {
    const existing = await this.db.transaction.findUnique({ where: { id: body.id } });
    if (existing) {
      if (existing.ownerId !== req.user.id) throw new ConflictException('ID already used');
      if (existing.type !== body.type || existing.amountMinor !== minor(body.amountMinor) || existing.currencyCode !== body.currencyCode || existing.title !== body.title) throw new ConflictException('ID used for different transaction');
      return output(existing);
    }
    const { id, type, currencyCode, title, category, paymentMethod, timestamp, source, groupId, notes } = body;
    if (category && (type === 'INCOME') !== ['SALARY','GIFT','OTHER_INCOME'].includes(category)) throw new BadRequestException('Category does not match type');
    try {
      return output(await this.db.transaction.create({ data: { id, ownerId: req.user.id, type, amountMinor: minor(body.amountMinor), currencyCode, title, category, paymentMethod, timestamp: date(timestamp), source, groupId, notes } }));
    } catch (error) {
      if (error instanceof Prisma.PrismaClientKnownRequestError && error.code === 'P2002') {
        const saved = await this.db.transaction.findUnique({ where: { id } });
        if (saved?.ownerId === req.user.id && saved.type === type && saved.amountMinor === minor(body.amountMinor) && saved.currencyCode === currencyCode && saved.title === title) return output(saved);
        throw new ConflictException('ID used for different transaction');
      }
      throw error;
    }
  }
  @Put(':id') async update(@Req() req: any, @Param('id', ParseUUIDPipe) id: string, @Body() body: TransactionInput) {
    if (body.id !== id || !Number.isInteger(body.version)) throw new BadRequestException('Matching id and version required');
    const { count } = await this.db.transaction.updateMany({ where: { id, ownerId: req.user.id, version: body.version, deletedAt: null }, data: { type: body.type, amountMinor: minor(body.amountMinor), currencyCode: body.currencyCode, title: body.title, category: body.category, paymentMethod: body.paymentMethod, timestamp: date(body.timestamp), source: body.source, groupId: body.groupId, notes: body.notes, version: { increment: 1 } } });
    if (!count) throw new ConflictException('Missing or stale transaction');
    return this.one(req, id);
  }
  @Delete(':id') async remove(@Req() req: any, @Param('id', ParseUUIDPipe) id: string, @Query('version') version: string) {
    if (!Number.isInteger(Number(version))) throw new BadRequestException('version required');
    const { count } = await this.db.transaction.updateMany({ where: { id, ownerId: req.user.id, version: Number(version), deletedAt: null }, data: { deletedAt: new Date(), version: { increment: 1 } } });
    if (!count) throw new ConflictException('Missing or stale transaction');
    return { ok: true };
  }
}
