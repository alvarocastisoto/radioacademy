import { Component, HostListener, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../services/auth/auth';
import { CommonModule } from '@angular/common';
import { ThemeService } from '../../services/theme/theme';
@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss',
})
export class Navbar {
  public authService = inject(AuthService);
  public themeService = inject(ThemeService);
  isScrolled = false;
  isHidden = false;
  private lastScrollTop = 0;

  @HostListener('window:scroll')
  onScroll() {
    const currentScroll = window.scrollY || document.documentElement.scrollTop;

    
    this.isScrolled = currentScroll > 20;

    
    if (currentScroll > this.lastScrollTop && currentScroll > 100) {
      
      this.isHidden = true;
    } else {
      
      this.isHidden = false;
    }

    
    this.lastScrollTop = currentScroll <= 0 ? 0 : currentScroll;
  }

  logout() {
    this.authService.logout();
  }
}
