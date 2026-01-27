import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PaymentSuccessComponent } from './payment-success';
import { PaymentService } from '../../../services/payment/payment';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

describe('PaymentSuccessComponent', () => {
  let component: PaymentSuccessComponent;
  let fixture: ComponentFixture<PaymentSuccessComponent>;
  let paymentServiceSpy: any;

  beforeEach(async () => {
    paymentServiceSpy = { confirmPayment: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [PaymentSuccessComponent],
      providers: [
        { provide: PaymentService, useValue: paymentServiceSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            queryParams: of({ session_id: 'test-session' })
          }
        },
        provideRouter([])
      ]
    })
      .compileComponents();

    fixture = TestBed.createComponent(PaymentSuccessComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
