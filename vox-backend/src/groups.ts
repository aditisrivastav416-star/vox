import { BadRequestException, Body, Controller, Delete, Get, NotFoundException, Param, ParseUUIDPipe, Post, Req, UseGuards } from '@nestjs/common';
import { IsArray, IsIn, IsObject, IsString, IsUUID, Length, Matches } from 'class-validator';
import { AuthGuard, Db, date, minor } from './core';

class GroupInput {
  @IsString() @Length(1,100) name!: string;
  @IsIn(['TRIP','HOME','FRIENDS','OTHER']) type!: string;
  @Matches(/^[A-Z]{3}$/) currencyCode!: string;
  @IsArray() @IsUUID('4', { each: true }) memberIds!: string[];
}
class ExpenseInput {
  @IsUUID() id!: string;
  @IsString() @Length(1,200) description!: string;
  @Matches(/^(0|[1-9]\d*)$/) amountMinor!: string;
  @IsUUID() paidById!: string;
  @IsString() timestamp!: string;
  @IsObject() shares!: Record<string,string>;
}
@UseGuards(AuthGuard) @Controller('groups')
export class GroupsController {
  constructor(private db: Db) {}
  private async member(userId: string, id: string) {
    if (!await this.db.membership.findUnique({ where: { groupId_userId: { groupId: id, userId } } })) throw new NotFoundException();
  }
  @Get() async list(@Req() req: any) { return this.db.group.findMany({ where: { members: { some: { userId: req.user.id } } }, include: { members: { include: { user: { select: { id: true, name: true, email: true } } } } } }); }
  @Post() async create(@Req() req: any, @Body() body: GroupInput) {
    const ids = [...new Set([req.user.id, ...body.memberIds])];
    if (await this.db.user.count({ where: { id: { in: ids } } }) !== ids.length) throw new BadRequestException('Unknown member');
    return this.db.group.create({ data: { name: body.name, type: body.type, currencyCode: body.currencyCode, members: { create: ids.map(userId => ({ userId, role: userId === req.user.id ? 'OWNER' : 'MEMBER' })) } }, include: { members: true } });
  }
  @Get(':id') async one(@Req() req: any, @Param('id', ParseUUIDPipe) id: string) {
    await this.member(req.user.id, id);
    const group = await this.db.group.findUniqueOrThrow({ where: { id }, include: { members: { include: { user: { select: { id: true, name: true, email: true } } } }, expenses: { include: { shares: true } }, settlements: true } });
    return { ...group, expenses: group.expenses.map(e => ({ ...e, amountMinor: e.amountMinor.toString(), shares: Object.fromEntries(e.shares.map(s => [s.userId, s.amountMinor.toString()])) })), settlements: group.settlements.map(s => ({ ...s, amountMinor: s.amountMinor.toString() })) };
  }
  @Post(':id/expenses') async expense(@Req() req: any, @Param('id', ParseUUIDPipe) id: string, @Body() body: ExpenseInput) {
    await this.member(req.user.id, id);
    const members = await this.db.membership.findMany({ where: { groupId: id }, select: { userId: true } });
    const ids = new Set(members.map(m => m.userId));
    if (!ids.has(body.paidById) || !body.shares || Object.keys(body.shares).some(k => !ids.has(k))) throw new BadRequestException('Invalid participant');
    const shares = Object.entries(body.shares).map(([userId, value]) => ({ userId, amountMinor: minor(value) }));
    if (!shares.length || shares.reduce((n,s) => n + s.amountMinor, 0n) !== minor(body.amountMinor)) throw new BadRequestException('Shares must sum to total');
    const existing = await this.db.groupExpense.findUnique({ where: { id: body.id } });
    if (existing) { if (existing.groupId !== id) throw new BadRequestException('ID already used'); return { id: existing.id }; }
    return this.db.groupExpense.create({ data: { id: body.id, groupId: id, description: body.description, amountMinor: minor(body.amountMinor), paidById: body.paidById, timestamp: date(body.timestamp), shares: { create: shares } }, select: { id: true } });
  }
  @Get(':id/balances') async balances(@Req() req: any, @Param('id', ParseUUIDPipe) id: string) {
    const g = await this.one(req, id);
    const balance = new Map<string,bigint>(g.members.map(m => [m.userId, 0n]));
    for (const e of g.expenses) { balance.set(e.paidById, balance.get(e.paidById)! + BigInt(e.amountMinor)); for (const [userId, amount] of Object.entries(e.shares)) balance.set(userId, balance.get(userId)! - BigInt(amount as string)); }
    for (const s of g.settlements) { balance.set(s.fromId, balance.get(s.fromId)! + BigInt(s.amountMinor)); balance.set(s.toId, balance.get(s.toId)! - BigInt(s.amountMinor)); }
    return { currencyCode: g.currencyCode, balances: Object.fromEntries([...balance].map(([id, n]) => [id, n.toString()])) };
  }
  @Post(':id/settlements') async settle(@Req() req: any, @Param('id', ParseUUIDPipe) id: string, @Body() body: { fromId: string; toId: string; amountMinor: string }) {
    await this.member(req.user.id, id);
    const members = await this.db.membership.findMany({ where: { groupId: id } });
    if (body.fromId === body.toId || (body.fromId !== req.user.id && body.toId !== req.user.id) || !members.some(m => m.userId === body.fromId) || !members.some(m => m.userId === body.toId) || minor(body.amountMinor) <= 0n) throw new BadRequestException('Invalid settlement');
    const s = await this.db.settlement.create({ data: { groupId: id, fromId: body.fromId, toId: body.toId, amountMinor: minor(body.amountMinor) } });
    return { ...s, amountMinor: s.amountMinor.toString() };
  }
  @Delete(':id') async remove(@Req() req: any, @Param('id', ParseUUIDPipe) id: string) {
    const membership = await this.db.membership.findUnique({ where: { groupId_userId: { groupId: id, userId: req.user.id } } });
    if (membership?.role !== 'OWNER') throw new NotFoundException();
    const balances = await this.balances(req, id);
    if (Object.values(balances.balances).some(v => v !== '0')) throw new BadRequestException('Settle balances first');
    await this.db.group.delete({ where: { id } }); return { ok: true };
  }
}
