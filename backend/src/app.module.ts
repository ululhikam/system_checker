import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { HoaxModule } from './hoax/hoax.module';
import { ScamModule } from './scam/scam.module';

@Module({
  imports: [ConfigModule.forRoot({ isGlobal: true }), HoaxModule, ScamModule],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule {}
