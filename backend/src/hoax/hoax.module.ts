import { Module } from '@nestjs/common';
import { HoaxController } from './hoax.controller';
import { HoaxService } from './hoax.service';

@Module({
  controllers: [HoaxController],
  providers: [HoaxService],
  exports: [HoaxService],
})
export class HoaxModule {}
