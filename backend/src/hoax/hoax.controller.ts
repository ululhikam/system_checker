import { Controller, Post, Body, HttpCode, HttpStatus } from '@nestjs/common';
import { HoaxService, FactCheckRequest, FactCheckResult } from './hoax.service';

@Controller('check-hoax')
export class HoaxController {
  constructor(private readonly hoaxService: HoaxService) {}

  @Post()
  @HttpCode(HttpStatus.OK)
  async checkHoax(@Body() payload: FactCheckRequest): Promise<FactCheckResult> {
    return this.hoaxService.checkHoax(payload);
  }
}
