import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core'; // 👈 1. Importa ChangeDetectorRef
import { ActivatedRoute, RouterLink } from '@angular/router';
import { PaymentService } from '../../../services/payment/payment'; // Asegúrate de que la ruta sea correcta
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-payment-success',
  standalone: true,
  imports: [RouterLink, CommonModule],
  templateUrl: './payment-success.html',
})
export class PaymentSuccessComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private paymentService = inject(PaymentService);
  private cdr = inject(ChangeDetectorRef); // 👈 2. Inyectamos el detector de cambios

  status = 'Verificando pago...';
  isConfirmed = false;

  ngOnInit() {
    this.route.queryParams.subscribe((params) => {
      const sessionId = params['session_id'];
      console.log('🔎 Session ID recibido:', sessionId); // Chivato 1

      if (sessionId) {
        this.paymentService.confirmPayment(sessionId).subscribe({
          next: (res) => {
            console.log('✅ Respuesta 200 del Backend:', res); // Chivato 2

            // Actualizamos variables
            this.status = '¡Matrícula confirmada!';
            this.isConfirmed = true;

            // 👇 3. FORZAMOS LA ACTUALIZACIÓN VISUAL
            this.cdr.detectChanges();
          },
          error: (err) => {
            console.error('❌ Error en el frontend:', err);
            this.status = 'Hubo un error, pero si te han cobrado, contacta con soporte.';
            this.cdr.detectChanges();
          },
        });
      } else {
        this.status = 'No se encontró código de sesión.';
        this.cdr.detectChanges();
      }
    });
  }
}
