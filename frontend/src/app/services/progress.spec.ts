import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ProgressService } from './progress';

describe('ProgressService', () => {
  let service: ProgressService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ProgressService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(ProgressService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should toggle progress', () => {
    const lessonId = '123';
    service.toggleProgress(lessonId).subscribe(res => {
      expect(res).toBe(true);
    });

    const req = httpMock.expectOne(`${(service as any).apiUrl}/${lessonId}/toggle`);
    expect(req.request.method).toBe('POST');
    req.flush(true);
  });
});
