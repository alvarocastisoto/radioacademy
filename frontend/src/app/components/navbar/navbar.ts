import { Component, HostListener, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../services/auth/auth';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss',
})
export class Navbar {
  public authService = inject(AuthService);

  isScrolled = false;
  isHidden = false;
  private lastScrollTop = 0;

  @HostListener('window:scroll')
  onScroll() {
    const currentScroll = window.scrollY || document.documentElement.scrollTop;

    // 1. Estilo visual (glass vs solido)
    this.isScrolled = currentScroll > 20;

    // 2. Lógica de esconder (Navbar inteligente)
    if (currentScroll > this.lastScrollTop && currentScroll > 100) {
      // Si bajamos Y hemos pasado 100px, escondemos
      this.isHidden = true;
    } else {
      // Si subimos, mostramos
      this.isHidden = false;
    }

    // Actualizamos la última posición (evitando números negativos en Safari mobile)
    this.lastScrollTop = currentScroll <= 0 ? 0 : currentScroll;
  }

  logout() {
    this.authService.logout();
  }
}
