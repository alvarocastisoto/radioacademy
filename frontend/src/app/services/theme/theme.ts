
import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  isDarkMode = signal<boolean>(false);

  constructor() {
    const savedTheme = localStorage.getItem('theme');
    
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;

    if (savedTheme === 'dark' || (!savedTheme && prefersDark)) {
      this.enableDarkMode();
    }
  }

  toggleTheme() {
    console.log('Cambiando tema... actual:', this.isDarkMode());
    this.isDarkMode() ? this.enableLightMode() : this.enableDarkMode();
  }

  private enableDarkMode() {
    document.body.classList.add('dark-mode');
    localStorage.setItem('theme', 'dark');
    this.isDarkMode.set(true);
  }

  private enableLightMode() {
    document.body.classList.remove('dark-mode');
    localStorage.setItem('theme', 'light');
    this.isDarkMode.set(false);
  }
}
