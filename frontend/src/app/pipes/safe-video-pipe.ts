import { Pipe, PipeTransform, inject } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Pipe({
  name: 'safeVideo',
  standalone: true,
})
export class SafeVideoPipe implements PipeTransform {
  // Inyectamos el desinfectante de Angular 🧼
  private sanitizer = inject(DomSanitizer);

  transform(url: string): SafeResourceUrl {
    if (!url) return '';

    // 1. Extraer el ID del vídeo de YouTube
    // Esta expresión regular (Regex) funciona para links largos (watch?v=) y cortos (youtu.be/)
    const regExp = /^.*(youtu.be\/|v\/|u\/\w\/|embed\/|watch\?v=|&v=)([^#&?]*).*/;
    const match = url.match(regExp);

    if (match && match[2].length === 11) {
      const videoId = match[2];
      // 2. Convertir al formato EMBED
      const embedUrl = `https://www.youtube.com/embed/${videoId}`;

      // 3. Decirle a Angular: "Confía en mí, esta URL es segura"
      return this.sanitizer.bypassSecurityTrustResourceUrl(embedUrl);
    }

    // Si no es un link de YouTube válido, devolvemos null o la url tal cual (aunque fallará)
    return this.sanitizer.bypassSecurityTrustResourceUrl(url);
  }
}
