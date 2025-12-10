import { Component } from '@angular/core';
// 1. AÑADE RouterLinkActive AQUÍ
import { RouterLink, RouterLinkActive } from '@angular/router'; 

@Component({
  selector: 'app-navbar',
  standalone: true,
  // 2. AÑADE RouterLinkActive AQUÍ TAMBIÉN
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss'
})
export class Navbar {

}