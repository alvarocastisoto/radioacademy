import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router'; // <--- Vuelve a entrar
import { Navbar } from './components/navbar/navbar';
import { Footer } from './components/footer/footer';
// Ya no importamos Home aquí, lo gestiona el router

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, Navbar, Footer], // <--- RouterOutlet dentro, Home fuera
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  title = 'frontend';
}