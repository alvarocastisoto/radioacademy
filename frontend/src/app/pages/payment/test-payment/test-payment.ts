import { Component, inject } from '@angular/core';
import { PaymentService } from '../../../services/payment/payment';

@Component({
  selector: 'app-test-payment',
  standalone: true,
  imports: [],
  templateUrl: './test-payment.html',
})
export class TestPaymentComponent {
  // private paymentService = inject(PaymentService);
  // buyCourse() {
  //   // Simulo comprar un curso de 49.99€ (4999 céntimos)
  //   this.paymentService.createCheckoutSession('Curso Locución Pro', 4999).subscribe({
  //     next: (res) => {
  //       // Redirigimos al usuario a la URL segura de Stripe
  //       window.location.href = res.url;
  //     },
  //     error: (err) => console.error('Error al iniciar pago', err),
  //   });
  // }
}
