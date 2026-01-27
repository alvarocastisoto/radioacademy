import { TestBed } from '@angular/core/testing';
import { DomSanitizer } from '@angular/platform-browser';
import { SafeVideoPipe } from './safe-video-pipe';

describe('SafeVideoPipe', () => {
  let pipe: SafeVideoPipe;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        SafeVideoPipe,
        {
          provide: DomSanitizer,
          useValue: {
            bypassSecurityTrustResourceUrl: (val: string) => val
          }
        }
      ]
    });
    pipe = TestBed.inject(SafeVideoPipe);
  });

  it('create an instance', () => {
    expect(pipe).toBeTruthy();
  });
});
