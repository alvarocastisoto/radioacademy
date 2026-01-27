import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TestPaymentComponent } from './test-payment';
import { PaymentService } from '../../../services/payment/payment';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';

describe('TestPaymentComponent', () => {
  let component: TestPaymentComponent;
  let fixture: ComponentFixture<TestPaymentComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestPaymentComponent],
      providers: [
        PaymentService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    })
      .compileComponents();

    fixture = TestBed.createComponent(TestPaymentComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
