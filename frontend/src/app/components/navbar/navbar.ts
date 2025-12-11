import { Component, inject } from '@angular/core';
// 1. AÑADE RouterLinkActive AQUÍ
import { RouterLink, RouterLinkActive } from '@angular/router'; 
import { AuthService } from '../../services/auth/auth';
@Component({
  selector: 'app-navbar',
  standalone: true,
  // 2. AÑADE RouterLinkActive AQUÍ TAMBIÉN
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss'
})
export class Navbar {
public authService = inject(AuthService);
  logout() {
    this.authService.logout();
  }

}