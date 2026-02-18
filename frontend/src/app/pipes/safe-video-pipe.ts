import { Pipe, PipeTransform, inject } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Pipe({
  name: 'safeVideo',
  standalone: true,
})
export class SafeVideoPipe implements PipeTransform {
  private sanitizer = inject(DomSanitizer);

  
  transform(url: string): SafeResourceUrl {
    if (!url) {
      
      return this.sanitizer.bypassSecurityTrustResourceUrl('');
    }

    
    const regExp = /^.*(youtu.be\/|v\/|u\/\w\/|embed\/|watch\?v=|&v=)([^#&?]*).*/;
    const match = url.match(regExp);

    if (match && match[2].length === 11) {
      const videoId = match[2];
      const embedUrl = `https://www.youtube.com/embed/${videoId}`;
      return this.sanitizer.bypassSecurityTrustResourceUrl(embedUrl);
    }

    
    
    
    return this.sanitizer.bypassSecurityTrustResourceUrl('');
  }
}
