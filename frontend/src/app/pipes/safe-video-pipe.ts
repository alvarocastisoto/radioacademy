import { Pipe, PipeTransform, inject } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Pipe({
  name: 'safeVideo',
  standalone: true,
})
export class SafeVideoPipe implements PipeTransform {
  private sanitizer = inject(DomSanitizer);

  // Quitamos '| null' del tipo de retorno
  transform(url: string): SafeResourceUrl {
    if (!url) {
      // Devolvemos una cadena vacía segura en vez de null
      return this.sanitizer.bypassSecurityTrustResourceUrl('');
    }

    // Regex para detectar YouTube
    const regExp = /^.*(youtu.be\/|v\/|u\/\w\/|embed\/|watch\?v=|&v=)([^#&?]*).*/;
    const match = url.match(regExp);

    if (match && match[2].length === 11) {
      const videoId = match[2];
      const embedUrl = `https://www.youtube.com/embed/${videoId}`;
      return this.sanitizer.bypassSecurityTrustResourceUrl(embedUrl);
    }

    // 🛡️ CAMBIO CLAVE AQUÍ:
    // Si no es un vídeo válido, devolvemos una cadena vacía pero "santificada".
    // Esto hace que el iframe se quede en blanco (about:blank) sin lanzar errores rojos.
    return this.sanitizer.bypassSecurityTrustResourceUrl('');
  }
}
