import { Module } from '@nestjs/common';
import { ScamController } from './scam.controller';
import { ScamService } from './scam.service';

@Module({
  controllers: [ScamController],
  providers: [ScamService],
  exports: [ScamService],
})
export class ScamModule {}
