import { Module } from '@nestjs/common';
import { Db, AuthGuard } from './core';
import { AuthController, UsersController } from './auth';
import { TransactionsController } from './transactions';
import { AnalyticsController, BudgetsController, CategoriesController } from './dashboard';
import { GroupsController } from './groups';
import { RecurringController } from './recurring';

@Module({ controllers: [AuthController, UsersController, TransactionsController, AnalyticsController, BudgetsController, CategoriesController, GroupsController, RecurringController], providers: [Db, AuthGuard] })
export class AppModule {}
